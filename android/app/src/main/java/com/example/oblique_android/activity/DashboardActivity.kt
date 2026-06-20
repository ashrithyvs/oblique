package com.example.oblique_android.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import com.example.oblique_android.activity.DashboardActivity
import com.example.oblique_android.services.MonitoringService
import com.example.oblique_android.services.Prefs
import com.example.oblique_android.utils.BitmapUtils
import com.example.oblique_android.utils.PrefsUtils
import com.example.oblique_android.utils.setupWindowInsets
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.example.oblique_android.validation.GoalValidator
import com.example.oblique_android.validation.ManualValidationLimiter

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
            if (!ensureUsageAccessPermission()) return@setOnClickListener
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri())
                startActivity(intent)
                Toast.makeText(this, "Please grant overlay permission", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            protectionActive = true
            tvProtectionStatus.text = getString(R.string.protection_active)
            btnStartProtection.visibility = View.GONE
            switchProtection.isChecked = true
            startMonitoring()
            refreshBlockedStatuses()
        }

        btnManualPoll.setOnClickListener {
            val limiter = ManualValidationLimiter(this)
            if (!limiter.canTrigger()) {
                Toast.makeText(this, "Manual validation available once per hour", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val username = PrefsUtils.getPlatformUsername(this@DashboardActivity, "leetcode")
                if (username.isNullOrBlank()) {
                    Toast.makeText(this@DashboardActivity, "No LeetCode username found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val goals = vm.allGoals.value.orEmpty()
                val validator = GoalValidator(this@DashboardActivity)
                var anyUpdated = false

                for (goal in goals) {
                    if (goal.platform.lowercase() == "leetcode" && goal.status == "active") {
                        if (validator.validate(goal, username)) anyUpdated = true
                    }
                }

                limiter.markTriggered()
                if (anyUpdated) {
                    Toast.makeText(this@DashboardActivity, "Progress updated!", Toast.LENGTH_SHORT).show()
                    vm.refreshDashboard()
                } else {
                    Toast.makeText(this@DashboardActivity, "No new progress detected", Toast.LENGTH_SHORT).show()
                }
            }
        }

        switchProtection.setOnCheckedChangeListener { _, isChecked ->
            protectionActive = isChecked
            if (isChecked) {
                tvProtectionStatus.text = getString(R.string.protection_active)
                btnStartProtection.visibility = View.GONE
                startMonitoring()
            } else {
                tvProtectionStatus.text = getString(R.string.protection_paused)
                btnStartProtection.visibility = View.VISIBLE
                stopMonitoring()
            }
            refreshBlockedStatuses()
        }

        vm.refreshDashboard()
    }

    private fun ensureUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        val granted = mode == AppOpsManager.MODE_ALLOWED
        if (!granted) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show()
        }
        return granted
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

            // --- Compute values safely ---
            val target = if (g.targetValue > 0) g.targetValue else 1
            val currentProgress = g.progress.coerceAtMost(target)

            // --- Title and progress text ---
            val unitLabel = when (g.unit.lowercase()) {
                "minutes" -> "minutes"
                "pages" -> "pages"
                else -> g.unit
            }

            tvTitle.text = "${g.platform} • ${g.targetValue} $unitLabel"
            tvProgress.text = "$currentProgress/$target"

            // --- icon ---
            val iconRes = when (g.platform.lowercase()) {
                "leetcode" -> R.drawable.ic_leetcode
                "duolingo" -> R.drawable.ic_duolingo
                else -> R.drawable.ic_meditation
            }
            ivIcon.setImageResource(iconRes)

            // Ensure fillView + glowView initial width = previous saved width (or 0)
            // We'll animate width to target width after layout is measured
            progressContainer.post {
                val totalW = progressContainer.width.takeIf { it > 0 } ?: return@post

                val ratio = currentProgress.toFloat() / target.toFloat()
                val newWidth = (totalW * ratio).toInt()

                // Read old width (if previously set). Default 0.
                val oldWidth = fillView.width.takeIf { it > 0 } ?: 0

                // Animate fill width smoothly
                val widthAnimator = ValueAnimator.ofInt(oldWidth, newWidth).apply {
                    duration = 700
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { anim ->
                        val w = anim.animatedValue as Int
                        val lp = fillView.layoutParams
                        lp.width = w
                        fillView.layoutParams = lp

                        // keep glowView same width so overlay matches fill
                        val gLp = glowView.layoutParams
                        gLp.width = w
                        glowView.layoutParams = gLp
                    }
                }

                // When completed -> pulse glow; when partial -> subtle breathing on the filled portion
                if (currentProgress >= target) {
                    doneCount++
                    // Completed: make fill fully opaque and loop a gentle pulse on glow
                    glowView.alpha = 0.35f
                    val pulse = ObjectAnimator.ofFloat(glowView, "alpha", 0.35f, 0.9f, 0.35f).apply {
                        duration = 900
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.REVERSE
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    // ensure fill gets to final width then start pulse
                    widthAnimator.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            pulse.start()
                        }
                    })
                } else {
                    // Partial: subtle breathing effect, smaller amplitude
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

    override fun onResume() {
        super.onResume()
        vm.refreshDashboard()
    }
}
