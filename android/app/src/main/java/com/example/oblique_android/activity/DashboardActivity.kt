package com.example.oblique_android.activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.oblique_android.BlockedAppEntity
import com.example.oblique_android.R
import com.example.oblique_android.models.GoalsViewModel
import com.example.oblique_android.services.MonitoringService
import com.example.oblique_android.utils.BitmapUtils
import com.example.oblique_android.viewmodel.GoalsViewModelFactory
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
        containerGoals = findViewById(R.id.containerGoals)
        btnStartProtection = findViewById(R.id.btnStartProtection)
        btnSettings = findViewById(R.id.btnSettings)

        // ViewModel
        vm = ViewModelProvider(this, GoalsViewModelFactory(application)).get(GoalsViewModel::class.java)

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Observe goals
        vm.allGoals.observe(this) { goals ->
            updateGoalsUI(goals)
        }

        lifecycleScope.launch {
            loadAndShowBlockedApps()
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

        val apps = pkgs.mapNotNull { pkg ->
            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                val name = ai.loadLabel(pm).toString()
                val icon = ai.loadIcon(pm)

                BlockedAppEntity(
                    packageName = pkg,
                    appName = name,
                    isBlocked = true,
                    icon = BitmapUtils.drawableToByteArray(icon)
                )
            } catch (_: Exception) {
                null
            }
        }

        tvAppsBlocked.text = apps.size.toString()
        showBlockedApps(apps)
    }

    private fun updateGoalsUI(goals: List<com.example.oblique_android.models.Goal>) {
        containerGoals.removeAllViews()
        val inflater = LayoutInflater.from(this)

        if (goals.isEmpty()) {
            val tv = TextView(this)
            tv.text = getString(R.string.no_goals_yet)
            tv.setTextColor(resources.getColor(R.color.text_primary))
            containerGoals.addView(tv)
            tvGoalsDone.text = "0"
            return
        }

        var doneCount = 0
        for (g in goals) {
            val view = inflater.inflate(R.layout.item_dashboard_goal, containerGoals, false)

            val ivIcon = view.findViewById<ImageView>(R.id.ivGoalIcon)
            val tvTitle = view.findViewById<TextView>(R.id.tvGoalTitle)
            val tvProgress = view.findViewById<TextView>(R.id.tvGoalProgress)

            val unitLabel = when (g.unit) {
                "minutes" -> "minutes"
                "pages" -> "pages"
                else -> g.unit
            }
            tvTitle.text = "${g.platform} • ${g.targetValue} $unitLabel"
            tvProgress.text = "${g.progress}/${g.targetValue}"

            if (g.progress >= g.targetValue) doneCount++

            // pick icon
            val iconRes = when (g.platform) {
                "LeetCode" -> R.drawable.ic_leetcode
                "Duolingo" -> R.drawable.ic_duolingo
                else -> R.drawable.ic_meditation
            }
            ivIcon.setImageResource(iconRes)

            containerGoals.addView(view)
        }

        tvGoalsDone.text = doneCount.toString()
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
