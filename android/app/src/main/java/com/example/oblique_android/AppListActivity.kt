package com.example.oblique_android

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnConfirm: Button

    private val allApps = mutableListOf<AppInfo>()
    private val selectedApps = mutableSetOf<String>()

    private lateinit var adapter: AppAdapter
    private lateinit var repo: BlockedAppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        recyclerView = findViewById(R.id.recyclerViewApps)
        etSearch = findViewById(R.id.etSearch)
        btnConfirm = findViewById(R.id.btnConfirm)

        repo = BlockedAppRepository.getInstance(this)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AppAdapter(selectedApps) { _ ->
            updateConfirmButton()
        }
        recyclerView.adapter = adapter

        // Load apps
        loadInstalledApps()

        // preload previously blocked
        lifecycleScope.launch {
            preloadSelections()
        }

        etSearch.imeOptions = EditorInfo.IME_ACTION_SEARCH
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                filterList(query)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnConfirm.setOnClickListener {
            lifecycleScope.launch {
                val entities = selectedApps.mapNotNull { pkg ->
                    val appInfo = allApps.find { it.packageName == pkg }
                    appInfo?.let {
                        BlockedAppEntity(
                            packageName = it.packageName,
                            appName = it.name,
                            isBlocked = true,
                            icon = it.icon.toByteArray()
                        )
                    }
                }
                repo.saveBlockedApps(entities)

                startActivity(Intent(this@AppListActivity, DashboardActivity::class.java))
                finish()
            }
        }
    }

    private suspend fun preloadSelections() {
        val blocked = repo.getBlockedApps()
        selectedApps.clear()
        selectedApps.addAll(blocked.map { it.packageName })
        updateConfirmButton()
        adapter.notifyDataSetChanged()
    }

    private fun loadInstalledApps() {
        lifecycleScope.launch(Dispatchers.IO) {
            val cached = AppCache.getCachedApps()
            if (cached != null) {
                Log.d("AppList", "Using cached apps (${cached.size})")
                withContext(Dispatchers.Main) {
                    allApps.clear()
                    allApps.addAll(cached)
                    filteredApps.clear()
                    filteredApps.addAll(allApps)
                    adapter.updateList(filteredApps)
                }
                return@launch
            }

            // fallback: load from package manager
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

            // Save in cache
            AppCache.setCachedApps(apps)

            Log.d("AppList", "Loaded ${apps.size} apps from system")
            withContext(Dispatchers.Main) {
                allApps.clear()
                allApps.addAll(apps)
                filteredApps.clear()
                filteredApps.addAll(allApps)
                adapter.updateList(filteredApps)
            }
        }
    }


    private fun filterList(query: String) {
        val filtered = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.name.lowercase().contains(query) || it.packageName.lowercase().contains(query)
            }
        }
        adapter.updateList(filtered)
    }

    private fun updateConfirmButton() {
        if (selectedApps.isEmpty()) {
            btnConfirm.isEnabled = false
            btnConfirm.text = "Select at least one app"
            btnConfirm.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF8E8E93.toInt())
            )
        } else {
            btnConfirm.isEnabled = true
            btnConfirm.text = "Block ${selectedApps.size} app(s)"
            btnConfirm.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF6A5ACD.toInt())
            )
        }
    }
}
