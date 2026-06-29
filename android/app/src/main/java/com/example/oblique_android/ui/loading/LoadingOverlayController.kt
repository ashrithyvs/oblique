package com.example.oblique_android.ui.loading

import android.view.View
import android.widget.TextView
import com.example.oblique_android.R

class LoadingOverlayController(
    private val overlayRoot: View,
    private val statusView: TextView,
) {
    val isVisible: Boolean
        get() = overlayRoot.visibility == View.VISIBLE

    fun show(status: String) {
        statusView.text = status
        overlayRoot.visibility = View.VISIBLE
    }

    fun updateStatus(status: String) {
        statusView.text = status
    }

    fun hide() {
        overlayRoot.visibility = View.GONE
    }

    companion object {
        fun bind(root: View): LoadingOverlayController {
            val overlay = root.findViewById<View>(R.id.loadingOverlay)
                ?: throw IllegalArgumentException("loadingOverlay not found in layout")
            val status = overlay.findViewById<TextView>(R.id.tvLoadingStatus)
                ?: throw IllegalArgumentException("tvLoadingStatus not found in loading overlay")
            return LoadingOverlayController(overlay, status)
        }
    }
}
