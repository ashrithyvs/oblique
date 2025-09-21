package com.example.oblique_android.repository

import android.content.Context
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.UserApi


data class BlockedAppsRequest(
    val blockedApps: List<String>
)

class BlockedAppsRepository(context: Context) {
    private val api: UserApi = ApiClient.getClient(context).create(UserApi::class.java)

    suspend fun listBlockedApps(): List<String> {
        return api.listBlockedApps().map { it.packageName }
    }

    suspend fun addBlockedApp(pkg: String): List<String> {
        return api.addBlockedApp(BlockedAppsRequest(listOf(pkg))).map { it.packageName }
    }

    suspend fun removeBlockedApp(pkg: String): List<String> {
        return api.removeBlockedApp(BlockedAppsRequest(listOf(pkg))).map { it.packageName }
    }

    suspend fun replaceBlockedApps(apps: List<String>): List<String> {
        return api.replaceBlockedApps(BlockedAppsRequest(apps)).map { it.packageName }
    }
}
