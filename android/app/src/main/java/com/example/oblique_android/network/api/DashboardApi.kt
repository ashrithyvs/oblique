package com.example.oblique_android.network.api

import retrofit2.http.GET

data class DashboardResponse(
    val goals: List<GoalDto>,
    val blockedApps: List<BlockedAppDto>
)

interface DashboardApi {
    @GET("/api/dashboard")
    suspend fun getDashboard(): DashboardResponse
}
