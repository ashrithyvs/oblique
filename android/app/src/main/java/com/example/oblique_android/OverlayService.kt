package com.example.oblique_android

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.TextView

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var blockedAppPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        blockedAppPackage = intent?.getStringExtra("blockedApp")
        showOverlay()
        return START_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) return // already showing

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_blocked_app, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        overlayView?.findViewById<TextView>(R.id.blockMessage)?.text =
            "This app is blocked!"

        overlayView?.findViewById<Button>(R.id.btnDismiss)?.setOnClickListener {
            // Remove overlay temporarily to allow PIN input
            stopSelf()
            val intent = Intent(this, PinUnlockActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        windowManager?.addView(overlayView, params)
    }

    override fun onDestroy() {
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
