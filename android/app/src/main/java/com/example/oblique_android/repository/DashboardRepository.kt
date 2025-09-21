package com.example.oblique_android.repository

import android.content.Context
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.DashboardApi
import com.example.oblique_android.network.api.GoalDto

/**
 * Local model for dashboard API response.
 * Matches backend: goals + blockedApps.
 */
data class DashboardModel(
    val goals: List<GoalDto>,
    val blockedApps: List<String> // backend now sends packageName[] in user
)

/**
 * DashboardRepository — API-only.
 *
 * Responsibilities:
 * - fetch dashboard data from backend
 * - return as DashboardModel
 * - no Room/DAOs involved
 */
class DashboardRepository(
    private val context: Context
) {
    private val api: DashboardApi by lazy {
        ApiClient.getClient(context).create(DashboardApi::class.java)
    }

    /**
     * Fetch dashboard from server.
     * Returns DashboardModel or null on failure.
     */
    suspend fun fetchDashboard(): DashboardModel? {
        return try {
            val resp = api.getDashboard()

            // Adapt response to local DashboardModel
            DashboardModel(
                goals = resp.goals,
                blockedApps = resp.blockedApps.map { it.packageName }
            )
        } catch (ex: Exception) {
            null
        }
    }
}
