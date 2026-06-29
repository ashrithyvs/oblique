package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.adapters.AppAdapter
import com.example.oblique_android.repository.AppRepository
import com.example.oblique_android.repository.BlockedAppsRepository
import com.example.oblique_android.services.Prefs
import com.example.oblique_android.utils.OnboardingRouter
import com.example.oblique_android.utils.PermissionGuard
import com.example.oblique_android.utils.PrefsUtils
import com.example.oblique_android.utils.setupWindowInsets
import kotlinx.coroutines.launch

class AppListActivity : AppCompatActivity() {

    private lateinit var repo: AppRepository
    private lateinit var adapter: AppAdapter
    private lateinit var selectedApps: MutableSet<String>
    private lateinit var btnConfirm: MaterialButton
    private lateinit var etSearch: EditText
    private lateinit var loadingOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)
        setupWindowInsets(R.id.rootAppList)

        Prefs.init(this)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingOverlay.visibility = View.VISIBLE
        repo = AppRepository.getInstance(this)
        selectedApps = PrefsUtils.loadBlockedSet(this).toMutableSet()

        btnConfirm = findViewById(R.id.btnConfirm)
        etSearch = findViewById(R.id.etSearch)

        val rv = findViewById<RecyclerView>(R.id.recyclerViewApps)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = AppAdapter(
            selectedApps = selectedApps,
            onSelectionChanged = { app ->
                toggleSelection(app.packageName)
                updateConfirmButton()
            }
        )
        rv.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        lifecycleScope.launch {
            try {
                val apps = repo.getInstalledApps()
                adapter.setOriginalList(apps)
                adapter.updateList(apps)
                updateConfirmButton()
            } finally {
                loadingOverlay.visibility = View.GONE
            }
        }

        btnConfirm.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val blockedRepo = BlockedAppsRepository(this@AppListActivity)
                    val updated = blockedRepo.replaceBlockedApps(selectedApps.toList())
                    saveSelectedApps(updated.toSet())
                    Prefs.setAppSelectionDone(true)
                    Log.d("AppListActivity", "Synced blocked apps: $updated")
                    val next = OnboardingRouter.afterApps(this@AppListActivity)
                    startActivity(OnboardingRouter.navigationIntent(this@AppListActivity, next))
                    finish()
                } catch (e: Exception) {
                    Log.e("AppListActivity", "Failed syncing blocked apps", e)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PermissionGuard.ensureGranted(this)
    }

    private fun toggleSelection(pkg: String) {
        if (selectedApps.contains(pkg)) selectedApps.remove(pkg) else selectedApps.add(pkg)
        saveSelectedApps(selectedApps)
    }

    private fun saveSelectedApps(set: Set<String>) {
        PrefsUtils.saveBlockedSet(this, set)
        Prefs.setSelectedApps(set)
    }

    private fun updateConfirmButton() {
        if (selectedApps.isEmpty()) {
            btnConfirm.text = "Select at least one app"
            btnConfirm.isEnabled = false
        } else {
            btnConfirm.text = "Confirm (${selectedApps.size})"
            btnConfirm.isEnabled = true
        }
    }
}
