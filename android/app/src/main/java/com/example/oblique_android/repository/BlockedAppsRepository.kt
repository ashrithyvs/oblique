package com.example.oblique_android.repository

import android.content.Context
import com.example.oblique_android.dao.BlockedAppDao
import com.example.oblique_android.entities.BlockedAppEntity
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.BlockedAppRequest
import com.example.oblique_android.network.api.BlockedAppsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BlockedAppsRepository(
    context: Context,
    private val dao: BlockedAppDao
) {
    private val api: BlockedAppsApi by lazy {
        ApiClient.getClient(context).create(BlockedAppsApi::class.java)
    }

    /**
     * Pull server list and replace local cache.
     */
    suspend fun refreshFromServer() = withContext(Dispatchers.IO) {
        try {
            val remote = api.listBlockedApps()
            val entities = remote.map { dto ->
                BlockedAppEntity(
                    packageName = dto.packageName,
                    appName = dto.packageName, // backend may not provide a friendly name
                    isBlocked = true,
                    icon = null, // icons are local-only
                    serverId = dto.id,
                    pendingSync = false,
                    createdAt = System.currentTimeMillis()
                )
            }
            dao.replaceAll(entities)
        } catch (ex: Exception) {
            // network error — leave local cache intact
        }
    }

    /**
     * Add blocked app: try server first, fall back to local pendingSync entry.
     */
    suspend fun addBlockedApp(pkg: String, reason: String? = null): BlockedAppEntity = withContext(Dispatchers.IO) {
        try {
            val req = BlockedAppRequest(packageName = pkg, reason = reason)
            val dto = api.addBlockedApp(req)
            val entity = BlockedAppEntity(
                packageName = dto.packageName,
                appName = dto.packageName,
                isBlocked = true,
                icon = null,
                serverId = dto.id,
                pendingSync = false,
                createdAt = System.currentTimeMillis()
            )
            dao.insert(entity)
            return@withContext entity
        } catch (ex: Exception) {
            // fallback: create local pending record to be synced later
            val entity = BlockedAppEntity(
                packageName = pkg,
                appName = pkg,
                isBlocked = true,
                icon = null,
                serverId = null,
                pendingSync = true,
                createdAt = System.currentTimeMillis()
            )
            dao.insert(entity)
            return@withContext entity
        }
    }

    /**
     * Delete blocked app. If the entity has a serverId, call server delete; otherwise delete locally.
     */
    suspend fun deleteBlockedApp(entity: BlockedAppEntity) = withContext(Dispatchers.IO) {
        if (entity.serverId != null) {
            try {
                api.removeBlockedApp(entity.serverId) // server expects Mongo _id
            } catch (_: Exception) {
                // ignore network failure (we'll still remove locally for UX)
            }
            // remove by server id to avoid duplicates
            dao.deleteByServerId(entity.serverId)
        } else {
            // never synced: delete by packageName
            dao.deleteByPackage(entity.packageName)
        }
    }

    /**
     * Convenience: delete by packageName (ViewModel can use this).
     * It will find an entity and then call deleteBlockedApp(entity).
     */
    suspend fun deleteByPackageName(pkg: String) = withContext(Dispatchers.IO) {
        val list = dao.getAll()
        val entity = list.firstOrNull { it.packageName == pkg }
        if (entity != null) {
            deleteBlockedApp(entity)
        } else {
            // nothing to do
        }
    }
}
