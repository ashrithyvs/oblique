package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.oblique_android.R
import com.example.oblique_android.services.PINManager
import com.example.oblique_android.services.Prefs
import com.example.oblique_android.utils.OnboardingRouter
import com.example.oblique_android.utils.PinKeypadController
import com.example.oblique_android.utils.setupWindowInsets

class PinConfirmActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PIN = "PIN"
    }

    private lateinit var keypad: PinKeypadController
    private var originalPin: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_confirm)
        setupWindowInsets(R.id.rootPinConfirm)
        Prefs.init(this)

        originalPin = intent.getStringExtra(EXTRA_PIN)
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
            Prefs.setPinSet(true)

            val next = OnboardingRouter.next(this, validateToken = false)
            startActivity(Intent(this, next))
            finish()
        }
    }
}
