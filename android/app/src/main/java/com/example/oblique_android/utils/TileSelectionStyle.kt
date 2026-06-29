package com.example.oblique_android.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.oblique_android.R
import com.google.android.material.card.MaterialCardView

object TileSelectionStyle {

    fun apply(card: MaterialCardView, selected: Boolean, context: Context) {
        val strokePx = (2 * context.resources.displayMetrics.density).toInt()
        if (selected) {
            card.strokeWidth = strokePx
            card.strokeColor = ContextCompat.getColor(context, R.color.theme_secondary)
            card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.tile_selected_bg))
            card.cardElevation = 4f
        } else {
            card.strokeWidth = 0
            card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            card.cardElevation = 2f
        }
    }
}
