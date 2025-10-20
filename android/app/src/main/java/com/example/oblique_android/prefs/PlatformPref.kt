package com.example.oblique_android.prefs;

data class PlatformPref(
    val key: String,         // e.g. "leetcode"
    val displayName: String, // e.g. "LeetCode"
    val iconRes: Int         // drawable resource
)