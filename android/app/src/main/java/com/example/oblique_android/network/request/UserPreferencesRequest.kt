// com/example/oblique_android/network/api/UserPreferencesRequest.kt
package com.example.oblique_android.network.request

data class UserPreferencesRequest(
    val displayName: String?,
    val usernames:java.util.Map<String, String>? = null
)
