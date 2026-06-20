package com.example.oblique_android.activity

import android.os.Bundle
import android.text.InputType
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chaos.view.PinView
import com.example.oblique_android.R
import com.example.oblique_android.services.PINManager
import com.example.oblique_android.utils.PinConstants
import com.example.oblique_android.utils.TempUnlockManager
import com.example.oblique_android.utils.setupWindowInsets
import com.google.android.material.button.MaterialButton

class PinUnlockActivity : AppCompatActivity() {
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_unlock)
        setupWindowInsets(R.id.rootPinUnlock)

        val pinView = findViewById<PinView>(R.id.pinViewUnlock)
        pinView.itemCount = PinConstants.LENGTH
        val togglePassword = findViewById<ImageView>(R.id.togglePassword)
        val btnUnlock = findViewById<MaterialButton>(R.id.btnUnlock)
        val blockedApp = intent.getStringExtra("blockedApp")

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
            if (!PinConstants.isValid(entered)) {
                Toast.makeText(
                    this,
                    getString(R.string.pin_enter_length, PinConstants.LENGTH),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!PINManager.isPinSet(applicationContext)) {
                Toast.makeText(this, "No PIN has been set yet!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (PINManager.verifyPin(applicationContext, entered!!)) {
                if (blockedApp != null) {
                    TempUnlockManager.setTempUnlock(this, blockedApp, 5 * 60 * 1000L)
                }
                Toast.makeText(this, "Unlocked successfully!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
