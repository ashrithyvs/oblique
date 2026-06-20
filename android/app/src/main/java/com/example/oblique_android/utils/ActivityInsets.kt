package com.example.oblique_android.utils

import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object ActivityInsets {

    fun enableEdgeToEdge(window: android.view.Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    /** Adds system bar insets on top of the view's layout padding. */
    fun applySystemBarPadding(view: View) {
        val initialLeft = view.paddingLeft
        val initialTop = view.paddingTop
        val initialRight = view.paddingRight
        val initialBottom = view.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialLeft + insets.left,
                initialTop + insets.top,
                initialRight + insets.right,
                initialBottom + insets.bottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(view)
    }

    fun setup(window: android.view.Window, rootView: View) {
        enableEdgeToEdge(window)
        applySystemBarPadding(rootView)
        WindowCompat.getInsetsController(window, rootView)?.apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}

fun AppCompatActivity.setupWindowInsets(@IdRes rootViewId: Int) {
    ActivityInsets.setup(window, findViewById(rootViewId))
}
