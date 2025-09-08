package com.example.oblique_android.activity

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chaos.view.PinView
import com.example.oblique_android.services.PINManager
import com.example.oblique_android.R
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
                    val prefs = getSharedPreferences("temp_unlocked", MODE_PRIVATE)
                    prefs.edit().putBoolean(blockedApp, true).apply()
                    Log.d("PinUnlock", "Unlocked only $blockedApp temporarily")
                }
                finish()
            } else {
                Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
