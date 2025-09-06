package com.example.oblique_android

import android.content.Context

class BlockedAppRepository private constructor(private val dao: BlockedAppDao) {
    object AppCache {
        private var cachedApps: List<AppInfo>? = null

        fun getCachedApps(): List<AppInfo>? = cachedApps

        fun setCachedApps(apps: List<AppInfo>) {
            cachedApps = apps
        }
    }

    suspend fun getBlockedApps(): List<BlockedAppEntity> = dao.getAll()

    suspend fun saveBlockedApps(apps: List<BlockedAppEntity>) {
        dao.clearAll()
        dao.insertAll(apps)
    }

    suspend fun getBlockedPackageNames(): List<String> = dao.getBlockedPackageNames()

    companion object {
        @Volatile
        private var INSTANCE: BlockedAppRepository? = null

        fun getInstance(context: Context): BlockedAppRepository {
            return INSTANCE ?: synchronized(this) {
                val db = BlockedAppDatabase.getDatabase(context.applicationContext)
                val instance = BlockedAppRepository(db.blockedAppDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
