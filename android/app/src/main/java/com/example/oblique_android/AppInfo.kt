package com.example.oblique_android

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    var isSelected: Boolean = false
)
