package com.example.oblique_android.network.api

import retrofit2.http.*

data class BlockedAppRequest(val packageName: String, val reason: String?)
data class BlockedAppDto(val id: String, val packageName: String, val reason: String?)

interface BlockedAppsApi {
    @GET("/api/blocked-apps")
    suspend fun listBlockedApps(): List<BlockedAppDto>

    @POST("/api/blocked-apps")
    suspend fun addBlockedApp(@Body req: BlockedAppRequest): BlockedAppDto

    @DELETE("/api/blocked-apps/{id}")
    suspend fun removeBlockedApp(@Path("id") id: String): Map<String, Any>
}
