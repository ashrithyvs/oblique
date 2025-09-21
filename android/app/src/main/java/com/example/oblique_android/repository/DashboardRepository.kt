package com.example.oblique_android.repository

import android.content.Context
import com.example.oblique_android.dao.BlockedAppDao
import com.example.oblique_android.dao.GoalDao
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.DashboardApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DashboardModel(
    val goals: List<com.example.oblique_android.network.api.GoalDto>,
    val blockedApps: List<com.example.oblique_android.network.api.BlockedAppDto>
)

class DashboardRepository(
    private val context: Context,
    private val goalDao: GoalDao,
    private val blockedAppDao: BlockedAppDao
) {
    private val api: DashboardApi by lazy {
        ApiClient.getClient(context).create(DashboardApi::class.java)
    }

    /**
     * Fetch dashboard from server and hydrate local DB.
     * Returns DashboardModel or null on failure.
     */
    suspend fun fetchAndHydrate(): DashboardModel? = withContext(Dispatchers.IO) {
        try {
            val resp = api.getDashboard()
            // Convert remote goals -> GoalEntity and blocked apps -> BlockedAppEntity
            // For brevity reuse GoalsRepository / BlockedAppsRepository logic (or re-map here)
            // We'll simply call the individual repo refresh methods instead of duplicating mapping logic.
            val goalsRepo = GoalsRepository(context, goalDao)
            val blockedRepo = BlockedAppsRepository(context, blockedAppDao)
            goalsRepo.refreshFromServer()
            blockedRepo.refreshFromServer()
            DashboardModel(resp.goals, resp.blockedApps)
        } catch (ex: Exception) {
            null
        }
    }
}
