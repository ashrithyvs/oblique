// com/example/oblique_android/network/api/UserPreferencesRequest.kt
package com.example.oblique_android.network.response

data class UserPreferencesResponse(
    val message: String,
    val user: UpdatedUser
)

data class UpdatedUser(
    val id: String,
    val displayName: String?,
    val platformUsernames: Map<String, String>?
)
