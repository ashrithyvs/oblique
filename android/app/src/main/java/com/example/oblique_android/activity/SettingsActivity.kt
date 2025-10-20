package com.example.oblique_android.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.adapters.AppInfo
import com.example.oblique_android.adapters.AppsSettingsAdapter
import com.example.oblique_android.adapters.GoalsSettingsAdapter
import com.example.oblique_android.adapters.PlatformPrefsAdapter
import com.example.oblique_android.models.Goal
import com.example.oblique_android.models.GoalsViewModel
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.GoalRequest
import com.example.oblique_android.utils.PrefsUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import com.example.oblique_android.network.api.UserApi
import com.example.oblique_android.network.api.UserDto
import com.example.oblique_android.network.request.UserPreferencesRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var tabGoals: LinearLayout
    private lateinit var tabApps: LinearLayout
    private lateinit var indicatorGoals: View
    private lateinit var indicatorApps: View
    private lateinit var tvGoals: TextView
    private lateinit var tvApps: TextView
    private lateinit var rvGoals: RecyclerView
    private lateinit var rvApps: RecyclerView
    private lateinit var btnAddGoal: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var settingsTabLabel: TextView

    private lateinit var goalsAdapter: GoalsSettingsAdapter
    private lateinit var appsAdapter: AppsSettingsAdapter
    private lateinit var vm: GoalsViewModel
    private lateinit var tabPreferences: LinearLayout
    private lateinit var indicatorPreferences: View
    private lateinit var tvPreferences: TextView
    private lateinit var api: ApiClient
    private lateinit var adapter: PlatformPrefsAdapter

    private lateinit var userApi: UserApi


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tabGoals = findViewById(R.id.tabGoals)
        tabApps = findViewById(R.id.tabApps)
        indicatorGoals = findViewById(R.id.indicatorGoals)
        indicatorApps = findViewById(R.id.indicatorApps)
        tvGoals = findViewById(R.id.tvGoals)
        tvApps = findViewById(R.id.tvApps)
        rvGoals = findViewById(R.id.rvGoals)
        rvApps = findViewById(R.id.recyclerViewApps)
        btnAddGoal = findViewById(R.id.btnAddGoal)
        btnSave = findViewById(R.id.btnSave)
        settingsTabLabel = findViewById(R.id.settingsTabLabel)

        vm = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(GoalsViewModel::class.java)

        tabPreferences = findViewById(R.id.tabPreferences)
        indicatorPreferences = findViewById(R.id.indicatorPreferences)
        tvPreferences = findViewById(R.id.tvPreferences)


        tabPreferences.setOnClickListener {
            startActivity(Intent(this, UserPreferencesActivity::class.java))
        }

        goalsAdapter = GoalsSettingsAdapter(
            emptyList(),
            onEdit = { goal -> showEditGoalDialog(goal) },
            onDelete = { goal -> showDeleteConfirm(goal) }
        )
        rvGoals.layoutManager = LinearLayoutManager(this)
        rvGoals.adapter = goalsAdapter

        val installedApps = loadInstalledApps()
        appsAdapter = AppsSettingsAdapter(
            installedApps,
            mutableSetOf()
        ) { pkg, checked ->
            if (checked) {
                vm.addBlockedApp(pkg)
            } else {
                vm.removeBlockedApp(pkg)
            }
        }
        rvApps.layoutManager = LinearLayoutManager(this)
        rvApps.adapter = appsAdapter

        tabGoals.setOnClickListener { switchTab(true) }
        tabApps.setOnClickListener { switchTab(false) }
        switchTab(true)

        vm.allGoals.observe(this) { list ->
            goalsAdapter.submitList(list)
            rvGoals.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }

        vm.allBlockedApps.observe(this) { pkgs ->
            appsAdapter.updateBlockedApps(pkgs.toMutableSet())
            PrefsUtils.saveBlockedSet(this, pkgs.toSet()) // keep cache synced
        }

        btnAddGoal.setOnClickListener {
            startActivity(Intent(this, GoalsActivity::class.java))
        }

//        btnSave.setOnClickListener {
//            lifecycleScope.launch {
//                try {
//                    val displayName = PrefsUtils.getDisplayName(this@SettingsActivity) ?: ""
//
//                    val request = UserPreferencesRequest(
//                        displayName = displayName,
//                        usernames = adapter.getUsernames() // or emptyMap<String, String>()
//                    )
//
//                    val result = withContext(Dispatchers.IO) {
//                        userApi.updatePreferences(request)
//                    }
//                    Toast.makeText(
//                        this@SettingsActivity,
//                        "Preferences updated for ${result.name ?: displayName}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                    Toast.makeText(this@SettingsActivity, "Failed to update preferences", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            finish()
//        }

        btnSave.setOnClickListener {
            lifecycleScope.launch {
                try {
                    // 🧩 Get latest blocked apps from adapter
                    val blockedPkgs = appsAdapter.getSelectedPackages()

                    // 🧠 Push update to backend
                    vm.replaceBlockedApps(blockedPkgs)

                    // 💾 Save locally
                    PrefsUtils.saveBlockedSet(this@SettingsActivity, blockedPkgs.toSet())

                    Toast.makeText(
                        this@SettingsActivity,
                        "Blocked apps updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@SettingsActivity,
                        "Failed to update blocked apps: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }



        // Initial load
        vm.refreshGoals()
        vm.refreshBlockedApps()
    }

    private fun switchTab(goals: Boolean) {
        if (goals) {
            indicatorGoals.visibility = View.VISIBLE
            indicatorApps.visibility = View.INVISIBLE
            rvGoals.visibility = View.VISIBLE
            rvApps.visibility = View.GONE
            btnAddGoal.visibility = View.VISIBLE
            btnSave.visibility = View.GONE
            settingsTabLabel.text = "Daily Goals"
        } else {
            indicatorGoals.visibility = View.INVISIBLE
            indicatorApps.visibility = View.VISIBLE
            rvGoals.visibility = View.GONE
            rvApps.visibility = View.VISIBLE
            btnAddGoal.visibility = View.GONE
            btnSave.visibility = View.VISIBLE
            settingsTabLabel.text = "Blocked App List"
        }
    }

    private fun showEditGoalDialog(goal: Goal) {
        val ctx = this
        val builder = AlertDialog.Builder(ctx)
        val view = layoutInflater.inflate(R.layout.dialog_edit_goal, null)
        val etTarget = view.findViewById<EditText>(R.id.etEditTarget)
        etTarget.setText(goal.targetValue.toString())

        builder.setTitle("Edit ${goal.platform}")
        builder.setView(view)
        builder.setPositiveButton("Save") { _, _ ->
            val newTarget = etTarget.text.toString().toIntOrNull()
            if (newTarget != null && newTarget > 0) {
                val req = GoalRequest(
                    platform = goal.platform,
                    platformUsername = "",
                    unit = goal.unit,
                    targetValue = newTarget,
                    baselineValue = 0,
                    deadline = null,
                    title = "${goal.unit} goal",
                    checkIntervalMs = 3600000L
                )
                vm.deleteGoal(goal.id) {
                    vm.createGoal(req)
                }
            } else {
                Toast.makeText(ctx, "Invalid target", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showDeleteConfirm(goal: Goal) {
        AlertDialog.Builder(this)
            .setTitle("Delete goal")
            .setMessage("Delete ${goal.platform}?")
            .setPositiveButton("Delete") { _, _ -> vm.deleteGoal(goal.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val apps = mutableListOf<AppInfo>()
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in packages.sortedBy { it.loadLabel(pm).toString() }) {
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
            val label = appInfo.loadLabel(pm).toString()
            val pkg = appInfo.packageName
            val icon = appInfo.loadIcon(pm)
            apps.add(AppInfo(label, pkg, icon))
        }
        return apps
    }
}
