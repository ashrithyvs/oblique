package com.example.oblique_android.activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.oblique_android.BlockedAppEntity
import com.example.oblique_android.R
import com.example.oblique_android.services.MonitoringService
import com.example.oblique_android.utils.BitmapUtils
import com.example.oblique_android.models.GoalsViewModel
import com.example.oblique_android.viewmodel.GoalsViewModelFactory
import kotlinx.coroutines.launch

/**
 * DashboardActivity — shows protection state, today's goals, stats and blocked apps.
 * Works with GoalsViewModel (LiveData) and SharedPreferences-stored blocked apps list.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var switchProtection: Switch
    private lateinit var tvProtectionStatus: TextView
    private lateinit var tvAppsBlocked: TextView
    private lateinit var tvGoalsDone: TextView
    private lateinit var tvTimeSaved: TextView
    private lateinit var containerBlockedApps: LinearLayout
    private lateinit var btnStartProtection: Button
    private lateinit var btnSettings: View
    private lateinit var tvGoalTitle: TextView
    private lateinit var tvGoalProgressText: TextView
    private lateinit var progressBarGoal: View

    private var protectionActive = false
    private lateinit var vm: GoalsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        switchProtection = findViewById(R.id.switchProtection)
        tvProtectionStatus = findViewById(R.id.tvProtectionStatus)
        tvAppsBlocked = findViewById(R.id.tvAppsBlocked)
        tvGoalsDone = findViewById(R.id.tvGoalsDone)
        tvTimeSaved = findViewById(R.id.tvTimeSaved)
        containerBlockedApps = findViewById(R.id.containerBlockedApps)
        btnStartProtection = findViewById(R.id.btnStartProtection)
        btnSettings = findViewById(R.id.btnSettings)
        tvGoalTitle = findViewById(R.id.tvGoalPlaceholder)
        tvGoalProgressText = findViewById(R.id.tvGoalProgress)
        progressBarGoal = findViewById(R.id.viewGoalProgressBar)

        // ViewModel
        vm = ViewModelProvider(this, GoalsViewModelFactory(application)).get(GoalsViewModel::class.java)

        btnSettings.setOnClickListener {
            // Open settings (two tab screen) so user can edit goals and apps
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Observe goals from ViewModel to show today's goals
        vm.allGoals.observe(this) { goals ->
            updateGoalsUI(goals)
        }

        lifecycleScope.launch {
            loadAndShowBlockedApps()
            // initialize stats
            tvGoalsDone.text = "0"
            tvTimeSaved.text = "0h"
        }

        btnStartProtection.setOnClickListener {
            protectionActive = true
            tvProtectionStatus.text = getString(R.string.protection_active)
            btnStartProtection.visibility = View.GONE
            switchProtection.isChecked = true
            startMonitoring()
            refreshBlockedStatuses()
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
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            loadAndShowBlockedApps()
        }
    }

    private fun startMonitoring() {
        val intent = Intent(this, MonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    private fun stopMonitoring() {
        stopService(Intent(this, MonitoringService::class.java))
    }

    private suspend fun loadAndShowBlockedApps() {
        val prefs = getSharedPreferences("blocked_apps", MODE_PRIVATE)
        val pkgs = prefs.getStringSet("pkgs", emptySet()) ?: emptySet()
        val pm = packageManager

        Log.d("Dashboard", "Blocked packages from prefs: $pkgs")

        val apps = pkgs.mapNotNull { pkg ->
            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                val name = ai.loadLabel(pm).toString()
                val icon = ai.loadIcon(pm)

                Log.d("Dashboard", "Loaded app: $name ($pkg)")

                BlockedAppEntity(
                    packageName = pkg,
                    appName = name,
                    isBlocked = true,
                    icon = BitmapUtils.drawableToByteArray(icon)
                )
            } catch (e: Exception) {
                Log.e("Dashboard", "Package not found or failed to load: $pkg", e)
                null
            }
        }

        tvAppsBlocked.text = apps.size.toString()
        showBlockedApps(apps)
    }

    private fun updateGoalsUI(goals: List<com.example.oblique_android.models.Goal>) {
        if (goals.isEmpty()) {
            tvGoalTitle.text = getString(R.string.no_goals_yet)
            tvGoalProgressText.visibility = View.GONE
            progressBarGoal.visibility = View.GONE
            tvGoalsDone.text = "0"
        } else {
            // For now show first goal as "Today's primary goal"
            val g = goals.first()
            val unitLabel = when (g.unit) {
                "minutes" -> "minutes"
                "pages" -> "pages"
                else -> g.unit
            }
            tvGoalTitle.text = "${g.platform} • ${g.targetValue} $unitLabel"
            tvGoalProgressText.visibility = View.VISIBLE
            progressBarGoal.visibility = View.VISIBLE
            tvGoalProgressText.text = "${g.progress}/${g.targetValue}"
            // Goals done stat
            val doneCount = goals.count { it.progress >= it.targetValue }
            tvGoalsDone.text = doneCount.toString()
        }
    }

    private fun showBlockedApps(apps: List<BlockedAppEntity>) {
        containerBlockedApps.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for (app in apps) {
            val view = inflater.inflate(R.layout.item_blocked_app, containerBlockedApps, false)
            val iv = view.findViewById<ImageView>(R.id.ivBlockedIcon)
            val tvName = view.findViewById<TextView>(R.id.tvBlockedName)
            val tvSubtitle = view.findViewById<TextView>(R.id.tvBlockedSubtitle)
            val tvStatus = view.findViewById<TextView>(R.id.tvBlockedStatus)

            tvName.text = app.appName
            tvSubtitle.text = getString(R.string.not_protected)
            tvStatus.text = if (protectionActive) getString(R.string.active) else getString(R.string.paused)

            if (app.icon != null) {
                val bmp = BitmapFactory.decodeByteArray(app.icon, 0, app.icon.size)
                iv.setImageBitmap(bmp)
            } else {
                iv.setImageResource(R.mipmap.ic_launcher) // fallback
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
