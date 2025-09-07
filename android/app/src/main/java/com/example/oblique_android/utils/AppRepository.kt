// app/src/main/java/com/example/oblique_android/AppRepository.kt
package com.example.oblique_android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository private constructor(private val appContext: Context) {

    private val pm: PackageManager = appContext.packageManager

    // In-memory caches
    @Volatile private var cachedList: List<AppInfo>? = null
    private val iconCache = object : LruCache<String, Drawable>(200) {}

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<AppInfo> =
        withContext(Dispatchers.Default) {
            if (!forceRefresh) {
                cachedList?.let { return@withContext it }
            }
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // user apps only
                .map { ai ->
                    val pkg = ai.packageName
                    val icon = iconCache[pkg] ?: pm.getApplicationIcon(ai).also { iconCache.put(pkg, it) }
                    val label = pm.getApplicationLabel(ai).toString()
                    AppInfo(name = label, packageName = pkg, icon = icon)
                }
                .sortedBy { it.name.lowercase() }
                .toList()

            cachedList = apps
            apps
        }

    fun clearMemoryCache() {
        cachedList = null
        iconCache.evictAll()
    }

    companion object {
        @Volatile private var INSTANCE: AppRepository? = null
        fun getInstance(context: Context): AppRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
