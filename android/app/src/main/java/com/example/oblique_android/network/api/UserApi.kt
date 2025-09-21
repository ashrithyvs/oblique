package com.example.oblique_android.network.api

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
    @GET("users/me")
    suspend fun getCurrentUser(): UserDto

    // List all blocked apps
    @GET("/api/user/me/blocked-apps")
    suspend fun listBlockedApps(): List<BlockedAppDto>

    // Add one or many apps (we’ll wrap a single one inside a list)
    @POST("/api/user/me/blocked-apps")
    suspend fun addBlockedApp(@Body body: BlockedAppsRequest): List<BlockedAppDto>

    // Remove one app by package
    @HTTP(method = "DELETE", path = "/api/user/me/blocked-apps", hasBody = true)
    suspend fun removeBlockedApp(@Body body: BlockedAppsRequest): List<BlockedAppDto>

    // Replace all apps with new set
    @PUT("/api/user/me/blocked-apps")
    suspend fun replaceBlockedApps(@Body body: BlockedAppsRequest): List<BlockedAppDto>
}
