package com.example.oblique_android.utils

import com.example.oblique_android.R

data class PlatformEntry(
    val key: String,
    val displayName: String,
    val iconRes: Int,
)

object PlatformCatalog {
    val all: List<PlatformEntry> = listOf(
        PlatformEntry(PlatformConstants.KEY_LEETCODE, "LeetCode", R.drawable.ic_leetcode),
        PlatformEntry(PlatformConstants.KEY_DUOLINGO, "Duolingo", R.drawable.ic_duolingo),
    )

    fun findByKey(key: String): PlatformEntry? =
        all.find { PlatformConstants.normalizePlatform(it.key) == PlatformConstants.normalizePlatform(key) }

    fun findByDisplayName(name: String): PlatformEntry? =
        all.find { it.displayName.equals(name, ignoreCase = true) }

    fun displayNameFor(platform: String): String =
        findByKey(platform)?.displayName ?: platform

    fun iconFor(platform: String): Int =
        findByKey(platform)?.iconRes ?: R.drawable.ic_placeholder
}
