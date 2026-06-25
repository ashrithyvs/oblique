package com.example.oblique_android.utils

import com.example.oblique_android.R

object PlatformConstants {
    const val KEY_LEETCODE = "leetcode"
    const val KEY_DUOLINGO = "duolingo"

    fun normalizePlatform(platform: String): String =
        platform.lowercase().trim()

    val iconMap: Map<String, Int> = mapOf(
        KEY_LEETCODE to R.drawable.ic_leetcode,
        KEY_DUOLINGO to R.drawable.ic_duolingo,
    )

    fun iconFor(platform: String): Int =
        iconMap[normalizePlatform(platform)] ?: R.drawable.ic_meditation
}
