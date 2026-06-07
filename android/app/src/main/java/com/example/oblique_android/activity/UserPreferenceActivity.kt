package com.example.oblique_android.activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.adapters.PlatformPrefsAdapter
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.UserApi
import com.example.oblique_android.network.request.UserPreferencesRequest
import com.example.oblique_android.network.response.UserPreferencesResponse
import com.example.oblique_android.prefs.PlatformPref
import com.example.oblique_android.utils.PrefsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import java.util.HashMap
import kotlin.collections.iterator
import kotlin.collections.toMap

class UserPreferencesActivity : AppCompatActivity() {

    private lateinit var etDisplayName: EditText
    private lateinit var rvPlatforms: RecyclerView
    private lateinit var btnSave: Button
    private lateinit var adapter: PlatformPrefsAdapter
    private lateinit var userApi: UserApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_preference)

        etDisplayName = findViewById(R.id.etDisplayName)
        rvPlatforms = findViewById(R.id.rvPlatforms)
        btnSave = findViewById(R.id.btnSaveSettings)

        userApi = ApiClient.getClient(this).create(UserApi::class.java)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Prefill existing display name
        val savedName = prefs.getString("displayName", "") ?: ""
        etDisplayName.setText(savedName)

        // Define supported platforms dynamically
        val platforms = listOf(
            PlatformPref("leetcode", "LeetCode", R.drawable.ic_leetcode),
            PlatformPref("duolingo", "Duolingo", R.drawable.ic_duolingo),
        )

        // Initialize adapter
        adapter = PlatformPrefsAdapter(platforms, prefs)
        rvPlatforms.layoutManager = LinearLayoutManager(this)
        rvPlatforms.adapter = adapter

        btnSave.setOnClickListener {
            lifecycleScope.launch {

            }
        }


        // Handle save button
        btnSave.setOnClickListener {
            val displayName = etDisplayName.text.toString().trim()
            val usernames = adapter.getUsernames() // Map<String, String>

            if (displayName.isEmpty()) {
                Toast.makeText(this, "Please enter a display name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save locally first for offline persistence
            prefs.edit {
                putString("displayName", displayName)
                adapter.saveUsernames(this)
            }
            PrefsUtils.saveDisplayName(this, displayName)

            // Sync to backend
            lifecycleScope.launch {
                try {
                    val request = UserPreferencesRequest(
                        displayName = displayName,
                        usernames = usernames
                    )

                    val updatedUser = ApiClient.getClient(this@UserPreferencesActivity)
                        .create(UserApi::class.java)
                        .updatePreferences(request)

                    // ✅ Store usernames locally in PrefsUtils for validators
                    val rawMap = adapter.getUsernames() // returns java.util.Map<String, String>
                    val usernameMap: Map<String, String> = HashMap(rawMap) // safely converted

                    for (entry in usernameMap.entries) {
                        val platform = entry.key
                        val uname = entry.value

                        if (uname.isNotBlank()) {
                            PrefsUtils.savePlatformUsername(this@UserPreferencesActivity, platform, uname)
                        }
                    }



                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@UserPreferencesActivity,
                            "Preferences saved for ${updatedUser.user.displayName ?: displayName}",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@UserPreferencesActivity,
                            "Failed to save preferences",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}
