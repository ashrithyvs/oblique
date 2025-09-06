package com.example.oblique_android

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var switchProtection: Switch
    private lateinit var tvProtectionStatus: TextView
    private lateinit var tvAppsBlocked: TextView
    private lateinit var tvGoalsDone: TextView
    private lateinit var tvTimeSaved: TextView
    private lateinit var containerBlockedApps: LinearLayout
    private lateinit var btnStartProtection: Button
    private lateinit var btnSettings: Button
    private lateinit var repo: BlockedAppRepository
    private var protectionActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        repo = BlockedAppRepository.getInstance(this)

        switchProtection = findViewById(R.id.switchProtection)
        tvProtectionStatus = findViewById(R.id.tvProtectionStatus)
        tvAppsBlocked = findViewById(R.id.tvAppsBlocked)
        tvGoalsDone = findViewById(R.id.tvGoalsDone)
        tvTimeSaved = findViewById(R.id.tvTimeSaved)
        containerBlockedApps = findViewById(R.id.containerBlockedApps)
        btnStartProtection = findViewById(R.id.btnStartProtection)
        btnSettings = findViewById(R.id.btnSettings)
        btnSettings.setOnClickListener {
            val intent = Intent(this, AppListActivity::class.java)
            startActivity(intent)
        }

        lifecycleScope.launch {
            loadAndShowBlockedApps()
            // simple placeholders for goals/time saved
            tvGoalsDone.text = "0"
            tvTimeSaved.text = "0h"
        }

        // Preload apps in background (only if cache empty)
        if (BlockedAppRepository.AppCache.getCachedApps() == null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val pm = packageManager
                val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                    .filter { it.packageName != packageName }

                val apps = installed.sortedWith(compareBy { it.loadLabel(pm).toString().lowercase() })
                    .map { appInfo ->
                        val name = appInfo.loadLabel(pm).toString()
                        val icon = appInfo.loadIcon(pm)
                        val pkg = appInfo.packageName
                        AppInfo(name, pkg, icon, false)
                    }

                BlockedAppRepository.AppCache.setCachedApps(apps)
                Log.d("Dashboard", "Preloaded ${apps.size} apps")
            }
        }

        // CTA -> start protection
        btnStartProtection.setOnClickListener {
            protectionActive = true
            tvProtectionStatus.text = "Protection active"
            btnStartProtection.visibility = Button.GONE
            switchProtection.isChecked = true
            startMonitoring()
            refreshBlockedStatuses()
        }

        switchProtection.setOnCheckedChangeListener { _, isChecked ->
            protectionActive = isChecked
            if (isChecked) {
                tvProtectionStatus.text = "Protection active"
                btnStartProtection.visibility = Button.GONE
                startMonitoring()
            } else {
                tvProtectionStatus.text = "Protection paused"
                btnStartProtection.visibility = Button.VISIBLE
                stopMonitoring()
            }
            refreshBlockedStatuses()
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
        val apps = repo.getBlockedApps()
        tvAppsBlocked.text = apps.size.toString()
        showBlockedApps(apps)
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
            tvSubtitle.text = "Not protected"
            tvStatus.text = if (protectionActive) "Active" else "Paused"

            // Load from DB blob
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
        // simply re-update statuses shown on each blocked-app card
        for (i in 0 until containerBlockedApps.childCount) {
            val v = containerBlockedApps.getChildAt(i)
            val tvStatus = v.findViewById<TextView>(R.id.tvBlockedStatus)
            tvStatus.text = if (protectionActive) "Active" else "Paused"
        }
    }

}
