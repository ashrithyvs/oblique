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
import com.example.oblique_android.prefs.PlatformPref
import com.example.oblique_android.utils.PrefsUtils
import com.example.oblique_android.utils.setupWindowInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserPreferencesActivity : AppCompatActivity() {

    private lateinit var etDisplayName: EditText
    private lateinit var rvPlatforms: RecyclerView
    private lateinit var btnSave: Button
    private lateinit var adapter: PlatformPrefsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_preference)
        setupWindowInsets(R.id.rootUserPreferences)

        etDisplayName = findViewById(R.id.etDisplayName)
        rvPlatforms = findViewById(R.id.rvPlatforms)
        btnSave = findViewById(R.id.btnSaveSettings)

        etDisplayName.setText(PrefsUtils.getDisplayName(this).orEmpty())

        val platforms = listOf(
            PlatformPref("leetcode", "LeetCode", R.drawable.ic_leetcode),
            PlatformPref("duolingo", "Duolingo", R.drawable.ic_duolingo),
        )

        adapter = PlatformPrefsAdapter(this, platforms)
        rvPlatforms.layoutManager = LinearLayoutManager(this)
        rvPlatforms.adapter = adapter

        btnSave.setOnClickListener {
            val displayName = etDisplayName.text.toString().trim()
            val usernames = adapter.getUsernames()

            if (displayName.isEmpty()) {
                Toast.makeText(this, "Please enter a display name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            PrefsUtils.saveDisplayName(this, displayName)
            adapter.saveUsernames()

            lifecycleScope.launch {
                try {
                    val request = UserPreferencesRequest(
                        displayName = displayName,
                        usernames = usernames
                    )

                    val updatedUser = ApiClient.getClient(this@UserPreferencesActivity)
                        .create(UserApi::class.java)
                        .updatePreferences(request)

                    for ((platform, uname) in usernames) {
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
