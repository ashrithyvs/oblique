package com.example.oblique_android.network.api

import com.example.oblique_android.repository.BlockedAppRequest
import com.example.oblique_android.repository.BlockedAppsRequest
import retrofit2.http.*

data class UserDto(
    val id: String,
    val email: String?,
    val name: String?,
    val hasPin: Boolean?,
    val blockedApps: List<String>?,
    val goals: List<GoalDto>?
)

data class BlockedAppDto(
    val packageName: String,
)
interface UserApi {
    @GET("/api/users/me")
    suspend fun getCurrentUser(): UserDto

    // List all blocked apps
    @GET("/api/user/me/blocked-apps")
    suspend fun listBlockedApps(): List<BlockedAppDto>

    // Add one or many apps (we’ll wrap a single one inside a list)
    @POST("/api/user/me/blocked-apps")
    suspend fun addBlockedApp(@Body body: BlockedAppRequest): List<BlockedAppDto>

    // UserApi.kt
    @DELETE("/api/user/me/blocked-apps/{pkg}")
    suspend fun removeBlockedApp(@Path("pkg") pkg: String): List<String>

    // Replace all apps with new set
    @PUT("/api/user/me/blocked-apps")
    suspend fun replaceBlockedApps(@Body body: BlockedAppsRequest): List<BlockedAppDto>
}
