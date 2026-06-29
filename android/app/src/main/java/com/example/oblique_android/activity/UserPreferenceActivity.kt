package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.oblique_android.R
import com.example.oblique_android.repository.GoalsRepository
import com.example.oblique_android.services.Prefs
import com.example.oblique_android.ui.loading.LoadingOverlayController
import com.example.oblique_android.ui.preferences.PlatformPreferencesFragment
import com.example.oblique_android.utils.GoalStatusConstants
import com.example.oblique_android.utils.OnboardingRouter
import com.example.oblique_android.utils.PermissionGuard
import com.example.oblique_android.utils.PlatformConstants
import com.example.oblique_android.utils.setupWindowInsets
import com.example.oblique_android.validation.BaselineSetupResult
import com.example.oblique_android.validation.PlatformBaselineService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserPreferencesActivity : AppCompatActivity(), PlatformPreferencesFragment.Listener {

    private lateinit var loadingOverlay: LoadingOverlayController
    private lateinit var baselineService: PlatformBaselineService
    private var mode: String = PlatformPreferencesFragment.MODE_SETTINGS
    private var platformFilter: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_preference)
        setupWindowInsets(R.id.rootUserPreferences)

        Prefs.init(this)
        mode = intent.getStringExtra(EXTRA_MODE) ?: PlatformPreferencesFragment.MODE_SETTINGS
        baselineService = PlatformBaselineService(this)
        loadingOverlay = LoadingOverlayController.bind(findViewById(R.id.rootUserPreferences))

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvSubtitle)
        if (mode == PlatformPreferencesFragment.MODE_ONBOARDING) {
            tvTitle.setText(R.string.platform_setup_title)
            tvSubtitle.setText(R.string.platform_setup_subtitle)
            tvSubtitle.visibility = View.VISIBLE
            lifecycleScope.launch {
                platformFilter = loadRequiredPlatforms()
                attachFragment()
            }
        } else {
            tvTitle.setText(R.string.save_settings)
            tvSubtitle.visibility = View.GONE
            attachFragment()
        }
    }

    private suspend fun loadRequiredPlatforms(): List<String> {
        return withContext(Dispatchers.IO) {
            GoalsRepository(this@UserPreferencesActivity)
                .listGoals()
                .filter { it.status == GoalStatusConstants.ACTIVE }
                .map { PlatformConstants.normalizePlatform(it.platform) }
                .distinct()
        }
    }

    private fun attachFragment() {
        if (supportFragmentManager.findFragmentById(R.id.platformPrefsContainer) != null) return
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.platformPrefsContainer,
                PlatformPreferencesFragment.newInstance(mode, platformFilter.takeIf { it.isNotEmpty() }),
            )
            .commit()
    }

    override fun onPreferencesActionStarted() {
        loadingOverlay.show(getString(R.string.loading_saving_preferences))
    }

    override fun onPreferencesSaved(displayName: String, usernames: Map<String, String>) {
        if (mode == PlatformPreferencesFragment.MODE_ONBOARDING) {
            runBaselineSetup(usernames)
        } else {
            loadingOverlay.hide()
            finish()
        }
    }

    override fun onPreferencesActionFailed(message: String) {
        loadingOverlay.hide()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun runBaselineSetup(usernames: Map<String, String>) {
        lifecycleScope.launch {
            try {
                val goals = withContext(Dispatchers.IO) {
                    GoalsRepository(this@UserPreferencesActivity).listGoals()
                }
                when (
                    val result = baselineService.setupBaselinesForGoals(
                        goals = goals,
                        usernames = usernames,
                        onStatus = { status -> loadingOverlay.updateStatus(status) },
                    )
                ) {
                    is BaselineSetupResult.Success -> {
                        Prefs.setPlatformSetupDone(true)
                        loadingOverlay.hide()
                        val next = OnboardingRouter.afterPlatformSetup(this@UserPreferencesActivity)
                        startActivity(Intent(this@UserPreferencesActivity, next))
                        finish()
                    }
                    is BaselineSetupResult.Failure -> {
                        loadingOverlay.hide()
                        Toast.makeText(
                            this@UserPreferencesActivity,
                            getString(R.string.baseline_setup_failed, result.message),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            } catch (e: Exception) {
                loadingOverlay.hide()
                Toast.makeText(
                    this@UserPreferencesActivity,
                    getString(R.string.baseline_setup_failed, e.message ?: "Unknown error"),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PermissionGuard.ensureGranted(this)
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
    }
}
