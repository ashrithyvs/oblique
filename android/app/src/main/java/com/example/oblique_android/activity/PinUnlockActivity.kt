package com.example.oblique_android.activity

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chaos.view.PinView
import com.example.oblique_android.R
import com.example.oblique_android.services.PINManager
import com.example.oblique_android.utils.TempUnlockManager
import com.google.android.material.button.MaterialButton

class PinUnlockActivity : AppCompatActivity() {
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_unlock)

        val pinView = findViewById<PinView>(R.id.pinViewUnlock)
        val togglePassword = findViewById<ImageView>(R.id.togglePassword)
        val btnUnlock = findViewById<MaterialButton>(R.id.btnUnlock)

        val blockedApp = intent.getStringExtra("blockedApp")

        // 👁️ Toggle show/hide PIN + icon swap
        togglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            pinView.inputType = if (isPasswordVisible) {
                InputType.TYPE_CLASS_NUMBER
            } else {
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            }
            pinView.setSelection(pinView.text?.length ?: 0)

            togglePassword.setImageResource(
                if (isPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
            )
        }

        btnUnlock.setOnClickListener {
            val entered = pinView.text?.toString()?.trim()
            if (entered.isNullOrEmpty() || entered.length != 6) {
                Toast.makeText(this, "Please enter a 6-digit PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (PINManager.verifyPin(this, entered)) {
                if (blockedApp != null) {
                    // ✅ Use TempUnlockManager instead of permanent flag
                    TempUnlockManager.setTempUnlock(this, blockedApp, 5 * 60 * 1000L) // 5 min unlock
                    Log.d("PinUnlock", "Unlocked $blockedApp for 5 minutes")
                }
                finish()
            } else {
                Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
