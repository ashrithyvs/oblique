package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.oblique_android.R
import com.example.oblique_android.services.PINManager
import com.example.oblique_android.utils.OnboardingRouter
import com.example.oblique_android.utils.PermissionGuard
import com.example.oblique_android.utils.PinKeypadController
import com.example.oblique_android.utils.setupWindowInsets

class PinConfirmActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PIN = "PIN"
        const val EXTRA_RESET_PIN = "reset_pin"
    }

    private lateinit var keypad: PinKeypadController
    private var originalPin: String? = null
    private var isResetPin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_confirm)
        setupWindowInsets(R.id.rootPinConfirm)

        originalPin = intent.getStringExtra(EXTRA_PIN)
        isResetPin = intent.getBooleanExtra(EXTRA_RESET_PIN, false)
        if (originalPin.isNullOrBlank()) {
            finish()
            return
        }

        findViewById<TextView>(R.id.tvPinTitle).setText(R.string.pin_confirm_title)
        findViewById<TextView>(R.id.tvPinSubtitle).setText(R.string.pin_confirm_subtitle)

        keypad = PinKeypadController(findViewById(R.id.pinScreenRoot)) { confirmPin ->
            if (confirmPin != originalPin) {
                Toast.makeText(this, R.string.pin_mismatch, Toast.LENGTH_SHORT).show()
                keypad.clear()
                return@PinKeypadController
            }

            PINManager.savePin(applicationContext, confirmPin)

            if (isResetPin) {
                Toast.makeText(this, R.string.pin_reset_success, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                startActivity(Intent(this, OnboardingRouter.afterPin(this)))
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PermissionGuard.ensureGranted(this)
    }
}
