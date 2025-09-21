package com.example.oblique_android.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.ByteArrayOutputStream

object BitmapUtils {

    /**
     * Convert a Drawable (app icon, etc.) into a Bitmap.
     * Handles BitmapDrawable quickly, otherwise draws into a Bitmap.
     */
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        // fallback: create bitmap using intrinsic size (or 1x1 if unspecified)
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Convert Drawable -> PNG/JPEG byte array (used by parts of the app that still store icon bytes).
     */
    fun drawableToByteArray(
        drawable: Drawable,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 90
    ): ByteArray {
        val bmp = drawableToBitmap(drawable)
        val baos = ByteArrayOutputStream()
        bmp.compress(format, quality, baos)
        return baos.toByteArray()
    }
}
