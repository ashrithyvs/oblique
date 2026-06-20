package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.adapters.AppsSettingsAdapter
import com.example.oblique_android.adapters.GoalsSettingsAdapter
import com.example.oblique_android.adapters.PlatformPrefsAdapter
import com.example.oblique_android.models.Goal
import com.example.oblique_android.models.GoalsViewModel
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.UserApi
import com.example.oblique_android.network.request.UserPreferencesRequest
import com.example.oblique_android.prefs.PlatformPref
import com.example.oblique_android.repository.AppRepository
import com.example.oblique_android.repository.AuthRepository
import com.example.oblique_android.services.PINManager
import com.example.oblique_android.services.Prefs
import com.example.oblique_android.utils.PrefsUtils
import com.example.oblique_android.utils.PermissionGuard
import com.example.oblique_android.utils.setupWindowInsets
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {

    private enum class Tab { GOALS, APPS, PREFERENCES }

    private lateinit var tabGoals: LinearLayout
    private lateinit var tabApps: LinearLayout
    private lateinit var tabPreferences: LinearLayout
    private lateinit var indicatorGoals: View
    private lateinit var indicatorApps: View
    private lateinit var indicatorPreferences: View
    private lateinit var tvGoals: TextView
    private lateinit var tvApps: TextView
    private lateinit var tvPreferences: TextView
    private lateinit var rvGoals: RecyclerView
    private lateinit var rvApps: RecyclerView
    private lateinit var btnAddGoal: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var settingsTabLabel: TextView
    private lateinit var sectionHeaderRow: LinearLayout
    private lateinit var preferencesPanel: View
    private lateinit var etDisplayName: EditText
    private lateinit var rvPlatformPrefs: RecyclerView
    private lateinit var btnSavePreferences: MaterialButton
    private lateinit var btnResetPin: MaterialButton
    private lateinit var btnSignOut: MaterialButton

    private lateinit var goalsAdapter: GoalsSettingsAdapter
    private lateinit var appsAdapter: AppsSettingsAdapter
    private lateinit var platformPrefsAdapter: PlatformPrefsAdapter
    private lateinit var vm: GoalsViewModel
    private lateinit var authRepo: AuthRepository
    private lateinit var loadingOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setupWindowInsets(R.id.rootSettings)
        Prefs.init(this)
        authRepo = AuthRepository(this)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingOverlay.visibility = View.VISIBLE

        bindViews()
        setupGoalsList()
        setupAppsList()
        setupPreferencesTab()
        setupTabs()
        setupActions()

        vm.refreshGoals()
        vm.refreshBlockedApps()
    }

    override fun onResume() {
        super.onResume()
        PermissionGuard.ensureGranted(this)
    }

    private fun bindViews() {
        tabGoals = findViewById(R.id.tabGoals)
        tabApps = findViewById(R.id.tabApps)
        tabPreferences = findViewById(R.id.tabPreferences)
        indicatorGoals = findViewById(R.id.indicatorGoals)
        indicatorApps = findViewById(R.id.indicatorApps)
        indicatorPreferences = findViewById(R.id.indicatorPreferences)
        tvGoals = findViewById(R.id.tvGoals)
        tvApps = findViewById(R.id.tvApps)
        tvPreferences = findViewById(R.id.tvPreferences)
        rvGoals = findViewById(R.id.rvGoals)
        rvApps = findViewById(R.id.recyclerViewApps)
        btnAddGoal = findViewById(R.id.btnAddGoal)
        btnSave = findViewById(R.id.btnSave)
        settingsTabLabel = findViewById(R.id.settingsTabLabel)
        sectionHeaderRow = findViewById(R.id.sectionHeaderRow)
        preferencesPanel = findViewById(R.id.preferencesPanel)
        etDisplayName = findViewById(R.id.etDisplayName)
        rvPlatformPrefs = findViewById(R.id.rvPlatformPrefs)
        btnSavePreferences = findViewById(R.id.btnSavePreferences)
        btnResetPin = findViewById(R.id.btnResetPin)
        btnSignOut = findViewById(R.id.btnSignOut)

        vm = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(GoalsViewModel::class.java)
    }

    private fun setupGoalsList() {
        goalsAdapter = GoalsSettingsAdapter(
            emptyList(),
            onEdit = { goal -> showEditGoalDialog(goal) },
            onDelete = { goal -> showDeleteConfirm(goal) }
        )
        rvGoals.layoutManager = LinearLayoutManager(this)
        rvGoals.adapter = goalsAdapter

        vm.allGoals.observe(this) { list ->
            goalsAdapter.submitList(list)
            rvGoals.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun setupAppsList() {
        appsAdapter = AppsSettingsAdapter(
            emptyList(),
            mutableSetOf()
        ) { pkg, checked ->
            if (checked) vm.addBlockedApp(pkg) else vm.removeBlockedApp(pkg)
        }
        rvApps.layoutManager = LinearLayoutManager(this)
        rvApps.adapter = appsAdapter

        vm.allBlockedApps.observe(this) { pkgs ->
            appsAdapter.updateBlockedApps(pkgs.toMutableSet())
            PrefsUtils.saveBlockedSet(this, pkgs.toSet())
            Prefs.setSelectedApps(pkgs.toSet())
        }

        lifecycleScope.launch {
            try {
                val apps = AppRepository.getInstance(this@SettingsActivity).getInstalledApps()
                appsAdapter.submitList(apps)
            } catch (_: Exception) {
                Toast.makeText(this@SettingsActivity, R.string.failed_load_apps, Toast.LENGTH_SHORT).show()
            } finally {
                loadingOverlay.visibility = View.GONE
            }
        }
    }

    private fun setupPreferencesTab() {
        etDisplayName.setText(PrefsUtils.getDisplayName(this).orEmpty())

        val platforms = listOf(
            PlatformPref("leetcode", "LeetCode", R.drawable.ic_leetcode),
            PlatformPref("duolingo", "Duolingo", R.drawable.ic_duolingo),
        )
        platformPrefsAdapter = PlatformPrefsAdapter(this, platforms)
        rvPlatformPrefs.layoutManager = LinearLayoutManager(this)
        rvPlatformPrefs.adapter = platformPrefsAdapter
        rvPlatformPrefs.isNestedScrollingEnabled = false
    }

    private fun setupTabs() {
        tabGoals.setOnClickListener { switchTab(Tab.GOALS) }
        tabApps.setOnClickListener { switchTab(Tab.APPS) }
        tabPreferences.setOnClickListener { switchTab(Tab.PREFERENCES) }
        switchTab(Tab.GOALS)
    }

    private fun setupActions() {
        btnAddGoal.setOnClickListener {
            startActivity(Intent(this, GoalsActivity::class.java))
        }

        btnSave.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val blockedPkgs = appsAdapter.getSelectedPackages()
                    vm.replaceBlockedApps(blockedPkgs)
                    val pkgSet = blockedPkgs.toSet()
                    PrefsUtils.saveBlockedSet(this@SettingsActivity, pkgSet)
                    Prefs.setSelectedApps(pkgSet)
                    Toast.makeText(
                        this@SettingsActivity,
                        "Blocked apps updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Failed to update blocked apps: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        btnSavePreferences.setOnClickListener { savePreferences() }

        btnResetPin.setOnClickListener { startResetPinFlow() }

        btnSignOut.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.sign_out)
                .setMessage(R.string.sign_out_confirm)
                .setPositiveButton(R.string.sign_out) { _, _ -> performSignOut() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun startResetPinFlow() {
        if (!PINManager.isPinSet(this)) {
            Toast.makeText(this, R.string.pin_not_set, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, PinUnlockActivity::class.java).apply {
                putExtra(PinUnlockActivity.EXTRA_VERIFY_FOR_RESET, true)
            }
        )
    }

    private fun savePreferences() {
        val displayName = etDisplayName.text.toString().trim()
        val usernames = platformPrefsAdapter.getUsernames()

        if (displayName.isEmpty()) {
            Toast.makeText(this, R.string.display_name_required, Toast.LENGTH_SHORT).show()
            return
        }

        PrefsUtils.saveDisplayName(this, displayName)
        platformPrefsAdapter.saveUsernames()

        lifecycleScope.launch {
            try {
                val request = UserPreferencesRequest(
                    displayName = displayName,
                    usernames = usernames
                )
                val updatedUser = ApiClient.getClient(this@SettingsActivity)
                    .create(UserApi::class.java)
                    .updatePreferences(request)

                for ((platform, uname) in usernames) {
                    if (uname.isNotBlank()) {
                        PrefsUtils.savePlatformUsername(this@SettingsActivity, platform, uname)
                    }
                }

                Toast.makeText(
                    this@SettingsActivity,
                    getString(R.string.preferences_saved, updatedUser.user.displayName ?: displayName),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@SettingsActivity,
                    R.string.preferences_save_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun performSignOut() {
        lifecycleScope.launch {
            authRepo.logout()
            withContext(Dispatchers.Main) {
                val intent = Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun switchTab(tab: Tab) {
        val primary = ContextCompat.getColor(this, R.color.text_primary)
        val muted = ContextCompat.getColor(this, R.color.text_muted)

        indicatorGoals.visibility = if (tab == Tab.GOALS) View.VISIBLE else View.INVISIBLE
        indicatorApps.visibility = if (tab == Tab.APPS) View.VISIBLE else View.INVISIBLE
        indicatorPreferences.visibility = if (tab == Tab.PREFERENCES) View.VISIBLE else View.INVISIBLE

        tvGoals.setTextColor(if (tab == Tab.GOALS) primary else muted)
        tvApps.setTextColor(if (tab == Tab.APPS) primary else muted)
        tvPreferences.setTextColor(if (tab == Tab.PREFERENCES) primary else muted)

        rvGoals.visibility = if (tab == Tab.GOALS) View.VISIBLE else View.GONE
        rvApps.visibility = if (tab == Tab.APPS) View.VISIBLE else View.GONE
        preferencesPanel.visibility = if (tab == Tab.PREFERENCES) View.VISIBLE else View.GONE

        sectionHeaderRow.visibility = if (tab == Tab.PREFERENCES) View.GONE else View.VISIBLE
        btnAddGoal.visibility = if (tab == Tab.GOALS) View.VISIBLE else View.GONE
        btnSave.visibility = if (tab == Tab.APPS) View.VISIBLE else View.GONE

        settingsTabLabel.text = when (tab) {
            Tab.GOALS -> "Daily Goals"
            Tab.APPS -> "Blocked App List"
            Tab.PREFERENCES -> "Preferences"
        }
    }

    private fun showEditGoalDialog(goal: Goal) {
        val ctx = this
        val builder = AlertDialog.Builder(ctx)
        val view = layoutInflater.inflate(R.layout.dialog_edit_goal, null)
        val etTarget = view.findViewById<EditText>(R.id.etEditTarget)
        val btnPickDeadline = view.findViewById<ImageButton>(R.id.btnPickDeadlineEdit)
        val tvDeadlinePreview = view.findViewById<TextView>(R.id.tvDeadlinePreviewEdit)

        etTarget.setText(goal.targetValue.toString())

        var selectedDeadlineEpoch: Long = goal.deadline
        if (selectedDeadlineEpoch > 0L) {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDeadlineEpoch }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val display = String.format(
                "%02d:%02d %s",
                if (hour % 12 == 0) 12 else hour % 12,
                minute,
                if (hour >= 12) "PM" else "AM"
            )
            tvDeadlinePreview.text = getString(R.string.goals_deadline_preview, display)
        }

        val alarmTint = ContextCompat.getColor(ctx, R.color.auth_title)
        btnPickDeadline.setColorFilter(alarmTint, android.graphics.PorterDuff.Mode.SRC_IN)
        btnPickDeadline.setOnClickListener {
            val cal = Calendar.getInstance()
            val picker = com.google.android.material.timepicker.MaterialTimePicker.Builder()
                .setTitleText("Select new goal deadline")
                .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_12H)
                .setHour(cal.get(Calendar.HOUR_OF_DAY))
                .setMinute(cal.get(Calendar.MINUTE))
                .build()

            picker.addOnPositiveButtonClickListener {
                val selectedHour = picker.hour
                val selectedMinute = picker.minute

                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                selectedDeadlineEpoch = todayStart + (selectedHour * 60 * 60 * 1000L) + (selectedMinute * 60 * 1000L)

                val display = String.format(
                    "%02d:%02d %s",
                    if (selectedHour % 12 == 0) 12 else selectedHour % 12,
                    selectedMinute,
                    if (selectedHour >= 12) "PM" else "AM"
                )
                tvDeadlinePreview.text = getString(R.string.goals_deadline_preview, display)
            }

            picker.show(supportFragmentManager, "deadline_edit_picker")
        }

        builder.setTitle("Edit ${goal.platform} Goal")
        builder.setView(view)

        builder.setPositiveButton("Save") { _, _ ->
            val newTarget = etTarget.text.toString().toIntOrNull()

            if (newTarget == null || newTarget <= 0) {
                Toast.makeText(ctx, "Invalid target value", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            lifecycleScope.launch {
                try {
                    val updatedGoal = goal.copy(
                        targetValue = newTarget,
                        deadline = selectedDeadlineEpoch
                    )
                    vm.updateGoal(updatedGoal)
                    Toast.makeText(ctx, "Goal updated", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(ctx, "Failed to update goal", Toast.LENGTH_SHORT).show()
                }
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
}
