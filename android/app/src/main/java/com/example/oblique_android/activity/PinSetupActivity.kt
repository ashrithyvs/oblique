package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.oblique_android.R
import com.example.oblique_android.utils.PermissionGuard
import com.example.oblique_android.utils.PinKeypadController
import com.example.oblique_android.utils.setupWindowInsets

class PinSetupActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESET_PIN = "reset_pin"
    }

    private lateinit var keypad: PinKeypadController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_setup)
        setupWindowInsets(R.id.rootPinSetup)

        val isReset = intent.getBooleanExtra(EXTRA_RESET_PIN, false)

        findViewById<TextView>(R.id.tvPinTitle).setText(R.string.pin_create_title)
        findViewById<TextView>(R.id.tvPinSubtitle).setText(
            if (isReset) R.string.pin_reset_setup_subtitle else R.string.pin_setup_subtitle
        )

        keypad = PinKeypadController(findViewById(R.id.pinScreenRoot)) { pin ->
            startActivity(
                Intent(this, PinConfirmActivity::class.java).apply {
                    putExtra(PinConfirmActivity.EXTRA_PIN, pin)
                    putExtra(PinConfirmActivity.EXTRA_RESET_PIN, isReset)
                }
            )
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        PermissionGuard.ensureGranted(this)
    }
}
