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

            // Check if any PIN exists first
            val storedExists = PINManager.isPinSet(applicationContext)
            Log.d("PinUnlock", "Entered PIN: $entered, PIN exists: $storedExists")

            if (!storedExists) {
                Toast.makeText(this, "No PIN has been set yet!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Read stored pin directly for debugging (⚠️ remove later in prod)
            val storedPin = PINManager.getPin(applicationContext)
            Log.d("PinUnlock", "Stored PIN (encrypted check): ${storedPin ?: "null"}")

            val verified = PINManager.verifyPin(applicationContext, entered)
            Log.d("PinUnlock", "Verification result = $verified")

            if (verified) {
                if (blockedApp != null) {
                    TempUnlockManager.setTempUnlock(this, blockedApp, 5 * 60 * 1000L) // 5 min unlock
                    Log.d("PinUnlock", "✅ Unlocked $blockedApp for 5 minutes")
                } else {
                    Log.d("PinUnlock", "✅ Correct PIN, no specific blocked app")
                }
                Toast.makeText(this, "Unlocked successfully!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Log.w("PinUnlock", "❌ Wrong PIN entered! Stored vs Entered may mismatch or decryption failed.")
                Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
