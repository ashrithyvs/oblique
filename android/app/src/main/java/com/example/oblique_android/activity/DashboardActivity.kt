package com.example.oblique_android.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.oblique_android.R
import com.example.oblique_android.models.Goal
import com.example.oblique_android.models.GoalsViewModel
import com.example.oblique_android.services.MonitoringService
import com.example.oblique_android.services.Prefs
import com.example.oblique_android.utils.BitmapUtils
import com.example.oblique_android.utils.GoalStatusConstants
import com.example.oblique_android.utils.PermissionGuard
import com.example.oblique_android.utils.PermissionUtils
import com.example.oblique_android.utils.PlatformCatalog
import com.example.oblique_android.utils.PlatformConstants
import com.example.oblique_android.utils.PrefsUtils
import com.example.oblique_android.utils.setupWindowInsets
import com.example.oblique_android.validation.GoalValidationScheduleManager
import com.example.oblique_android.validation.GoalValidationService
import com.example.oblique_android.validation.ManualValidationLimiter
import com.example.oblique_android.validation.ValidationOutcome
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var switchProtection: Switch
    private lateinit var tvProtectionStatus: TextView
    private lateinit var tvAppsBlocked: TextView
    private lateinit var tvGoalsDone: TextView
    private lateinit var tvTimeSaved: TextView
    private lateinit var containerBlockedApps: LinearLayout
    private lateinit var containerGoals: LinearLayout
    private lateinit var btnStartProtection: Button
    private lateinit var btnSettings: View
    private lateinit var btnManualPoll: View

    private var protectionActive = false
    private lateinit var vm: GoalsViewModel
    private lateinit var scheduleManager: GoalValidationScheduleManager
    private lateinit var validationService: GoalValidationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        setupWindowInsets(R.id.scrollRoot)

        switchProtection = findViewById(R.id.switchProtection)
        tvProtectionStatus = findViewById(R.id.tvProtectionStatus)
        tvAppsBlocked = findViewById(R.id.tvAppsBlocked)
        tvGoalsDone = findViewById(R.id.tvGoalsDone)
        tvTimeSaved = findViewById(R.id.tvTimeSaved)
        containerBlockedApps = findViewById(R.id.containerBlockedApps)
        containerGoals = findViewById(R.id.containerGoals)
        btnStartProtection = findViewById(R.id.btnStartProtection)
        btnSettings = findViewById(R.id.btnSettings)
        btnManualPoll = findViewById(R.id.manualPoll)
        vm = ViewModelProvider(this)[GoalsViewModel::class.java]
        scheduleManager = GoalValidationScheduleManager(this)
        validationService = GoalValidationService(this)
        Prefs.init(this)

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        vm.allGoals.observe(this) { goals ->
            updateGoalsUI(goals)
        }

        vm.allBlockedApps.observe(this) { pkgs ->
            tvAppsBlocked.text = pkgs.size.toString()
            showBlockedApps(pkgs)
            val pkgSet = pkgs.toSet()
            PrefsUtils.saveBlockedSet(this, pkgSet)
            Prefs.setSelectedApps(pkgSet)
        }

        btnStartProtection.setOnClickListener {
            if (!ensureRequiredPermissions()) return@setOnClickListener
            protectionActive = true
            tvProtectionStatus.text = getString(R.string.protection_active)
            btnStartProtection.visibility = View.GONE
            switchProtection.isChecked = true
            startMonitoring()
            scheduleManager.refreshForGoals(vm.allGoals.value.orEmpty())
            refreshBlockedStatuses()
        }

        btnManualPoll.setOnClickListener {
            val limiter = ManualValidationLimiter(this)
            if (!limiter.canTrigger()) {
                Toast.makeText(this, getString(R.string.manual_validation_rate_limit), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val username = PrefsUtils.getPlatformUsername(
                    this@DashboardActivity,
                    PlatformConstants.KEY_LEETCODE,
                )
                if (username.isNullOrBlank()) {
                    Toast.makeText(
                        this@DashboardActivity,
                        getString(R.string.manual_validation_no_username),
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@launch
                }

                val goals = vm.allGoals.value.orEmpty()
                var anyUpdated = false

                for (goal in goals) {
                    if (PlatformConstants.normalizePlatform(goal.platform) == PlatformConstants.KEY_LEETCODE &&
                        goal.status == GoalStatusConstants.ACTIVE
                    ) {
                        when (validationService.validateGoal(goal, username)) {
                            is ValidationOutcome.Updated,
                            is ValidationOutcome.Completed -> anyUpdated = true
                            else -> {}
                        }
                    }
                }

                limiter.markTriggered()
                if (anyUpdated) {
                    Toast.makeText(
                        this@DashboardActivity,
                        getString(R.string.manual_validation_progress_updated),
                        Toast.LENGTH_SHORT,
                    ).show()
                    vm.refreshDashboard()
                } else {
                    Toast.makeText(
                        this@DashboardActivity,
                        getString(R.string.manual_validation_no_progress),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }

        switchProtection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !ensureRequiredPermissions()) {
                switchProtection.isChecked = false
                return@setOnCheckedChangeListener
            }
            protectionActive = isChecked
            if (isChecked) {
                tvProtectionStatus.text = getString(R.string.protection_active)
                btnStartProtection.visibility = View.GONE
                startMonitoring()
                scheduleManager.refreshForGoals(vm.allGoals.value.orEmpty())
            } else {
                tvProtectionStatus.text = getString(R.string.protection_paused)
                btnStartProtection.visibility = View.VISIBLE
                stopMonitoring()
                scheduleManager.cancelAll()
            }
            refreshBlockedStatuses()
        }

        vm.refreshDashboard()
    }

    override fun onResume() {
        super.onResume()
        if (!PermissionGuard.ensureGranted(this)) return
        vm.refreshDashboard()
    }

    private fun ensureRequiredPermissions(): Boolean {
        if (PermissionUtils.hasRequiredPermissions(this)) return true
        PermissionGuard.ensureGranted(this)
        return false
    }

    private fun startMonitoring() {
        val intent = Intent(this, MonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    private fun stopMonitoring() {
        stopService(Intent(this, MonitoringService::class.java))
    }

    private fun updateGoalsUI(goals: List<Goal>) {
        containerGoals.removeAllViews()
        val inflater = LayoutInflater.from(this)

        if (goals.isEmpty()) {
            val tv = TextView(this).apply {
                text = getString(R.string.no_goals_yet)
                setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.text_primary))
            }
            containerGoals.addView(tv)
            tvGoalsDone.text = "0"
            tvTimeSaved.text = "0%"
            return
        }

        var doneCount = 0

        for (g in goals) {
            val view = inflater.inflate(R.layout.item_dashboard_goal, containerGoals, false)

            val ivIcon = view.findViewById<ImageView>(R.id.ivGoalIcon)
            val tvTitle = view.findViewById<TextView>(R.id.tvGoalTitle)
            val tvProgress = view.findViewById<TextView>(R.id.tvGoalProgress)

            val progressContainer = view.findViewById<ViewGroup>(R.id.progressContainer)
            val fillView = view.findViewById<View>(R.id.viewProgressFill)
            val glowView = view.findViewById<View>(R.id.viewProgressGlow)

            val target = if (g.targetValue > 0) g.targetValue else 1
            val currentProgress = g.progress.coerceAtMost(target)

            val unitLabel = when (g.unit.lowercase()) {
                "minutes" -> "minutes"
                "pages" -> "pages"
                else -> g.unit
            }

            tvTitle.text = "${g.platform} • ${g.targetValue} $unitLabel"
            tvProgress.text = "$currentProgress/$target"

            ivIcon.setImageResource(PlatformCatalog.iconFor(g.platform))

            progressContainer.post {
                val totalW = progressContainer.width.takeIf { it > 0 } ?: return@post

                val ratio = currentProgress.toFloat() / target.toFloat()
                val newWidth = (totalW * ratio).toInt()

                val oldWidth = fillView.width.takeIf { it > 0 } ?: 0

                val widthAnimator = ValueAnimator.ofInt(oldWidth, newWidth).apply {
                    duration = 700
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { anim ->
                        val w = anim.animatedValue as Int
                        val lp = fillView.layoutParams
                        lp.width = w
                        fillView.layoutParams = lp

                        val gLp = glowView.layoutParams
                        gLp.width = w
                        glowView.layoutParams = gLp
                    }
                }

                if (currentProgress >= target) {
                    doneCount++
                    glowView.alpha = 0.35f
                    val pulse = ObjectAnimator.ofFloat(glowView, "alpha", 0.35f, 0.9f, 0.35f).apply {
                        duration = 900
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.REVERSE
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    widthAnimator.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            pulse.start()
                        }
                    })
                } else {
                    glowView.alpha = 0.18f
                    val breathe = ObjectAnimator.ofFloat(glowView, "alpha", 0.12f, 0.28f, 0.12f).apply {
                        duration = 1100
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.REVERSE
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    widthAnimator.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            breathe.start()
                        }
                    })
                }

                widthAnimator.start()
            }

            containerGoals.addView(view)
        }

        tvGoalsDone.text = doneCount.toString()
        val successRate = if (goals.isNotEmpty()) (doneCount * 100) / goals.size else 0
        tvTimeSaved.text = "$successRate%"
    }

    private fun showBlockedApps(pkgs: List<String>) {
        containerBlockedApps.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val pm = packageManager

        for (pkg in pkgs) {
            val view = inflater.inflate(R.layout.item_blocked_app, containerBlockedApps, false)
            val iv = view.findViewById<ImageView>(R.id.ivBlockedIcon)
            val tvName = view.findViewById<TextView>(R.id.tvBlockedName)
            val tvSubtitle = view.findViewById<TextView>(R.id.tvBlockedSubtitle)
            val tvStatus = view.findViewById<TextView>(R.id.tvBlockedStatus)

            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                tvName.text = ai.loadLabel(pm).toString()
                tvSubtitle.text = getString(R.string.not_protected)
                tvStatus.text = if (protectionActive) getString(R.string.active) else getString(R.string.paused)

                val icon = ai.loadIcon(pm)
                val bmp = BitmapUtils.drawableToBitmap(icon)
                iv.setImageBitmap(bmp)
            } catch (_: Exception) {
                tvName.text = pkg
                iv.setImageResource(R.mipmap.ic_launcher)
            }

            containerBlockedApps.addView(view)
        }
    }

    private fun refreshBlockedStatuses() {
        for (i in 0 until containerBlockedApps.childCount) {
            val v = containerBlockedApps.getChildAt(i)
            val tvStatus = v.findViewById<TextView>(R.id.tvBlockedStatus)
            tvStatus.text = if (protectionActive) getString(R.string.active) else getString(R.string.paused)
        }
    }
}
