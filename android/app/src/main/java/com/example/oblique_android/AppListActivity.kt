package com.example.oblique_android

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class AppListActivity : AppCompatActivity() {

    private lateinit var repo: AppRepository
    private lateinit var adapter: AppAdapter
    private lateinit var selectedApps: MutableSet<String>
    private lateinit var btnConfirm: Button
    private lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        repo = AppRepository.getInstance(this)
        selectedApps = loadSelectedApps()

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
            val apps = repo.getInstalledApps()
            adapter.setOriginalList(apps)
            adapter.updateList(apps)
            updateConfirmButton()
        }

        btnConfirm.setOnClickListener {
            saveSelectedApps(selectedApps)
            finish()
        }
    }

    private fun toggleSelection(pkg: String) {
        if (selectedApps.contains(pkg)) {
            Log.d("AppListActivity", "Removing $pkg from selection")
            selectedApps.remove(pkg)
        } else {
            Log.d("AppListActivity", "Adding $pkg to selection")
            selectedApps.add(pkg)
        }
        saveSelectedApps(selectedApps)
    }

    private fun loadSelectedApps(): MutableSet<String> {
        val p = getSharedPreferences("blocked_apps", MODE_PRIVATE)
        val set = p.getStringSet("pkgs", emptySet())!!.toMutableSet()
        Log.d("AppListActivity", "Loaded selected apps: $set")
        return set
    }

    private fun saveSelectedApps(set: Set<String>) {
        val p = getSharedPreferences("blocked_apps", MODE_PRIVATE)
        p.edit().putStringSet("pkgs", set.toSet()).apply()
        Log.d("AppListActivity", "Saved selected apps: $set")
    }

    private fun updateConfirmButton() {
        if (selectedApps.isEmpty()) {
            btnConfirm.text = "Select at least one app"
            btnConfirm.isEnabled = false
            btnConfirm.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
        } else {
            btnConfirm.text = "Confirm (${selectedApps.size})"
            btnConfirm.isEnabled = true
            btnConfirm.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
        }
    }
}
