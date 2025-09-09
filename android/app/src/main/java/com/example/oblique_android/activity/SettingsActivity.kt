package com.example.oblique_android.activity

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.adapters.AppInfo
import com.example.oblique_android.adapters.AppsSettingsAdapter
import com.example.oblique_android.adapters.GoalsSettingsAdapter
import com.example.oblique_android.models.Goal
import com.google.android.material.button.MaterialButton
import android.widget.Toast
import android.content.Context
import android.widget.EditText
import com.example.oblique_android.models.GoalsViewModel

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

    private lateinit var goalsAdapter: GoalsSettingsAdapter
    private lateinit var appsAdapter: AppsSettingsAdapter
    private lateinit var vm: GoalsViewModel

    private val prefsBlockedKey = "blocked_apps"
    private val prefsBlockedSetKey = "pkgs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // find views
        tabGoals = findViewById(R.id.tabGoals)
        tabApps = findViewById(R.id.tabApps)
        indicatorGoals = findViewById(R.id.indicatorGoals)
        indicatorApps = findViewById(R.id.indicatorApps)
        tvGoals = findViewById(R.id.tvGoals)
        tvApps = findViewById(R.id.tvApps)
        rvGoals = findViewById(R.id.rvGoals)
        rvApps = findViewById(R.id.recyclerViewApps) // reuse id: set your layout to include this id in Apps tab
        btnAddGoal = findViewById(R.id.btnAddGoal)
        btnSave = findViewById(R.id.btnSave)

        // ViewModel
        vm = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(GoalsViewModel::class.java)

        // Goals adapter
        goalsAdapter = GoalsSettingsAdapter(emptyList(), onEdit = { goal ->
            showEditGoalDialog(goal)
        }, onDelete = { goal ->
            showDeleteConfirm(goal)
        })
        rvGoals.layoutManager = LinearLayoutManager(this)
        rvGoals.adapter = goalsAdapter

        // Apps adapter: prepare list then adapter
        val installedApps = loadInstalledApps()
        val blockedSet = loadBlockedSet().toMutableSet()
        appsAdapter = AppsSettingsAdapter(installedApps, blockedSet, onSelectionChanged = { pkg, checked ->
            if (checked) blockedSet.add(pkg) else blockedSet.remove(pkg)
            saveBlockedSet(blockedSet)
        })
        rvApps.layoutManager = LinearLayoutManager(this)
        rvApps.adapter = appsAdapter

        // tabs
        tabGoals.setOnClickListener { switchTab(true) }
        tabApps.setOnClickListener { switchTab(false) }
        switchTab(true)

        // observe goals
        vm.allGoals.observe(this) { list ->
            goalsAdapter.submitList(list)
        }

        btnAddGoal.setOnClickListener {
            // Launch the same GoalsActivity onboarding in add-mode (or show a dialog)
            val intent = Intent(this, GoalsActivity::class.java)
            startActivity(intent)
        }

        btnSave.setOnClickListener {
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun switchTab(goals: Boolean) {
        if (goals) {
            indicatorGoals.visibility = View.VISIBLE
            indicatorApps.visibility = View.INVISIBLE
            rvGoals.visibility = View.VISIBLE
            rvApps.visibility = View.GONE
            btnAddGoal.visibility = View.VISIBLE
        } else {
            indicatorGoals.visibility = View.INVISIBLE
            indicatorApps.visibility = View.VISIBLE
            rvGoals.visibility = View.GONE
            rvApps.visibility = View.VISIBLE
            btnAddGoal.visibility = View.GONE
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
                goal.targetValue = newTarget
                vm.updateGoal(goal)
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
            .setPositiveButton("Delete") { _, _ -> vm.deleteGoal(goal) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val apps = mutableListOf<AppInfo>()
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in packages.sortedBy { it.loadLabel(pm).toString() }) {
            // Exclude system apps for clarity (optional)
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
            val label = appInfo.loadLabel(pm).toString()
            val pkg = appInfo.packageName
            val icon = appInfo.loadIcon(pm)
            apps.add(AppInfo(label, pkg, icon))
        }
        return apps
    }

    private fun loadBlockedSet(): Set<String> {
        val p = getSharedPreferences(prefsBlockedKey, Context.MODE_PRIVATE)
        return p.getStringSet(prefsBlockedSetKey, emptySet()) ?: emptySet()
    }

    private fun saveBlockedSet(set: Set<String>) {
        val p = getSharedPreferences(prefsBlockedKey, Context.MODE_PRIVATE)
        p.edit().putStringSet(prefsBlockedSetKey, set).apply()
    }
}
