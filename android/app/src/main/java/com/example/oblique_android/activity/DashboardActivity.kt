package com.example.oblique_android.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.oblique_android.R
import com.example.oblique_android.BuildConfig
import com.example.oblique_android.gating.MonitoringController
import com.example.oblique_android.models.Goal
import com.example.oblique_android.models.GoalsViewModel
import com.example.oblique_android.services.Prefs
import com.example.oblique_android.ui.goals.GoalPeriodUiHelper
import com.example.oblique_android.ui.loading.LoadingOverlayController
import com.example.oblique_android.utils.AppTime
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
import com.google.android.material.button.MaterialButton
import com.example.oblique_android.validation.ValidationOutcome
import com.example.oblique_android.validation.platform.GoalPeriodPolicy
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var switchProtection: Switch
    private lateinit var tvProtectionStatus: TextView
    private lateinit var tvAppsBlocked: TextView
    private lateinit var tvGoalsDone: TextView
    private lateinit var tvTimeSaved: TextView
    private lateinit var containerBlockedApps: LinearLayout
    private lateinit var containerGoals: LinearLayout
    private lateinit var btnStartProtection: MaterialButton
    private lateinit var btnSettings: View
    private lateinit var btnManualPoll: View
    private var btnDevSimulateHour: View? = null

    private var protectionActive = false
    private lateinit var vm: GoalsViewModel
    private lateinit var scheduleManager: GoalValidationScheduleManager
    private lateinit var validationService: GoalValidationService
    private lateinit var loadingOverlay: LoadingOverlayController
    private var tvDevSimulatedTime: TextView? = null
    private var tvDevRealTime: TextView? = null
    private val devSimulateHandler = Handler(Looper.getMainLooper())
    private var devSimulateHoldStartedAt = 0L
    private var devSimulateResetTriggered = false
    private val devSimulateResetHoldMs = 5_000L
    private val devSimulateResetRunnable = Runnable {
        devSimulateResetTriggered = true
        resetSimulatedTime()
    }

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
        loadingOverlay = LoadingOverlayController.bind(findViewById(R.id.dashboardRoot))
        Prefs.init(this)
        AppTime.init(this)

        if (BuildConfig.DEV_MODE) {
            findViewById<View>(R.id.devTimePanel)?.visibility = View.VISIBLE
            tvDevSimulatedTime = findViewById(R.id.tvDevSimulatedTime)
            tvDevRealTime = findViewById(R.id.tvDevRealTime)
            btnDevSimulateHour = findViewById(R.id.btnDevSimulateHour)
            btnDevSimulateHour?.visibility = View.VISIBLE
            btnDevSimulateHour?.contentDescription = getString(R.string.dev_simulate_hold_reset)
            updateDevTimeDisplay()
            setupDevSimulateTouchListener()
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        vm.allGoals.observe(this) { goals ->
            updateGoalsUI(goals)
            if (protectionActive) {
                lifecycleScope.launch { MonitoringController.syncMonitoringState(this@DashboardActivity) }
            }
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
            enableProtection()
        }

        btnManualPoll.setOnClickListener {
            val limiter = ManualValidationLimiter(this)
            if (!limiter.canTrigger()) {
                Toast.makeText(this, getString(R.string.manual_validation_rate_limit), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                loadingOverlay.show(getString(R.string.loading_checking_goals))
                val goals = vm.allGoals.value.orEmpty()
                    .filter { it.status == GoalStatusConstants.ACTIVE }
                if (goals.isEmpty()) {
                    loadingOverlay.hide()
                    Toast.makeText(
                        this@DashboardActivity,
                        getString(R.string.manual_validation_no_progress),
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@launch
                }

                var anyUpdated = false
                var validatedAny = false

                for (goal in goals) {
                    val platformKey = PlatformConstants.normalizePlatform(goal.platform)
                    val username = PrefsUtils.getPlatformUsername(this@DashboardActivity, platformKey)
                    if (username.isNullOrBlank()) continue

                    validatedAny = true
                    loadingOverlay.updateStatus(
                        when (platformKey) {
                            PlatformConstants.KEY_LEETCODE -> getString(R.string.loading_fetching_progress)
                            else -> getString(R.string.loading_checking_platform, goal.platform)
                        },
                    )

                    when (validationService.validateGoal(goal, username)) {
                        is ValidationOutcome.Updated,
                        is ValidationOutcome.Completed -> anyUpdated = true
                        else -> {}
                    }
                }

                if (anyUpdated) {
                    loadingOverlay.updateStatus(getString(R.string.loading_updating_progress))
                    vm.refreshDashboard()
                    MonitoringController.syncMonitoringState(this@DashboardActivity)
                }

                loadingOverlay.hide()
                limiter.markTriggered()

                if (!validatedAny) {
                    Toast.makeText(
                        this@DashboardActivity,
                        getString(R.string.manual_validation_no_username),
                        Toast.LENGTH_SHORT,
                    ).show()
                } else if (anyUpdated) {
                    Toast.makeText(
                        this@DashboardActivity,
                        getString(R.string.manual_validation_progress_updated),
                        Toast.LENGTH_SHORT,
                    ).show()
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
                enableProtection()
            } else {
                disableProtection()
            }
            refreshBlockedStatuses()
        }

        protectionActive = Prefs.isProtectionEnabled()
        switchProtection.isChecked = protectionActive
        if (protectionActive) {
            tvProtectionStatus.text = getString(R.string.protection_active)
            btnStartProtection.visibility = View.GONE
            scheduleManager.refreshForGoals(vm.allGoals.value.orEmpty())
            lifecycleScope.launch { MonitoringController.syncMonitoringState(this@DashboardActivity) }
        } else {
            tvProtectionStatus.text = getString(R.string.protection_paused)
            btnStartProtection.visibility = View.VISIBLE
        }

        vm.refreshDashboard()
        if (BuildConfig.DEV_MODE) updateDevTimeDisplay()
    }

    private fun setupDevSimulateTouchListener() {
        btnDevSimulateHour?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    devSimulateResetTriggered = false
                    devSimulateHoldStartedAt = System.currentTimeMillis()
                    devSimulateHandler.postDelayed(devSimulateResetRunnable, devSimulateResetHoldMs)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    devSimulateHandler.removeCallbacks(devSimulateResetRunnable)
                    if (!devSimulateResetTriggered &&
                        System.currentTimeMillis() - devSimulateHoldStartedAt < devSimulateResetHoldMs
                    ) {
                        advanceSimulatedTimeOneHour()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    devSimulateHandler.removeCallbacks(devSimulateResetRunnable)
                    true
                }
                else -> false
            }
        }
    }

    private fun advanceSimulatedTimeOneHour() {
        AppTime.advanceDevTimeMs(this, 3_600_000L)
        updateDevTimeDisplay()
        vm.allGoals.value?.let { updateGoalsUI(it) }
        lifecycleScope.launch { MonitoringController.syncMonitoringState(this@DashboardActivity) }
        Toast.makeText(this, getString(R.string.dev_simulate_plus_hour), Toast.LENGTH_SHORT).show()
    }

    private fun resetSimulatedTime() {
        AppTime.resetDevTimeMs(this)
        updateDevTimeDisplay()
        vm.allGoals.value?.let { updateGoalsUI(it) }
        lifecycleScope.launch { MonitoringController.syncMonitoringState(this@DashboardActivity) }
        Toast.makeText(this, getString(R.string.dev_simulated_time_reset), Toast.LENGTH_SHORT).show()
    }

    private fun updateDevTimeDisplay() {
        if (!BuildConfig.DEV_MODE) return
        val realNow = System.currentTimeMillis()
        val realFormatted = java.text.SimpleDateFormat("EEE, MMM d h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(realNow))
        tvDevSimulatedTime?.text = getString(
            R.string.dev_simulated_time,
            AppTime.formatNowForDisplay(),
            AppTime.formatOffsetForDisplay(),
        )
        tvDevRealTime?.text = getString(R.string.dev_real_time, realFormatted)
    }

    private fun enableProtection() {
        protectionActive = true
        Prefs.setProtectionEnabled(true)
        tvProtectionStatus.text = getString(R.string.protection_active)
        btnStartProtection.visibility = View.GONE
        switchProtection.isChecked = true
        scheduleManager.refreshForGoals(vm.allGoals.value.orEmpty())
        lifecycleScope.launch { MonitoringController.syncMonitoringState(this@DashboardActivity) }
    }

    private fun disableProtection() {
        protectionActive = false
        Prefs.setProtectionEnabled(false)
        tvProtectionStatus.text = getString(R.string.protection_paused)
        btnStartProtection.visibility = View.VISIBLE
        switchProtection.isChecked = false
        MonitoringController.stopMonitoring(this)
        scheduleManager.cancelAll()
    }

    override fun onResume() {
        super.onResume()
        if (!PermissionGuard.ensureGranted(this)) return
        vm.refreshDashboard()
        if (BuildConfig.DEV_MODE) updateDevTimeDisplay()
        if (Prefs.isProtectionEnabled()) {
            lifecycleScope.launch { MonitoringController.syncMonitoringState(this@DashboardActivity) }
        }
    }

    private fun ensureRequiredPermissions(): Boolean {
        if (PermissionUtils.hasRequiredPermissions(this)) return true
        PermissionGuard.ensureGranted(this)
        return false
    }

    private fun updateGoalsUI(goals: List<Goal>) {
        containerGoals.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val bufferMs = PrefsUtils.getDeadlineBufferMs(this)
        val nowMs = AppTime.nowMs()
        val periodPolicy = GoalPeriodPolicy()

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
            val tvPeriodStatus = view.findViewById<TextView>(R.id.tvGoalPeriodStatus)
            val tvProgress = view.findViewById<TextView>(R.id.tvGoalProgress)

            val progressContainer = view.findViewById<ViewGroup>(R.id.progressContainer)
            val fillView = view.findViewById<View>(R.id.viewProgressFill)
            val glowView = view.findViewById<View>(R.id.viewProgressGlow)

            val target = if (g.targetValue > 0) g.targetValue else 1
            val periodSatisfied = periodPolicy.isCurrentPeriodSatisfied(g, bufferMs, nowMs)
            val currentProgress = periodPolicy.currentPeriodProgress(g, bufferMs, nowMs).coerceAtMost(target)
            if (periodSatisfied) doneCount++

            val unitLabel = when (g.unit.lowercase()) {
                "minutes" -> "minutes"
                "pages" -> "pages"
                else -> g.unit
            }

            tvTitle.text = "${g.platform} • ${g.targetValue} $unitLabel"
            tvProgress.text = "$currentProgress/$target"

            val statusText = GoalPeriodUiHelper.formatPeriodStatus(this, g, bufferMs, nowMs)
            if (statusText.isNotBlank()) {
                tvPeriodStatus.text = statusText
                tvPeriodStatus.visibility = View.VISIBLE
            } else {
                tvPeriodStatus.visibility = View.GONE
            }

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

                if (periodSatisfied) {
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
