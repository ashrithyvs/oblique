package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chaos.view.PinView
import com.example.oblique_android.services.PINManager
import com.example.oblique_android.services.Prefs
import com.example.oblique_android.R
import com.example.oblique_android.utils.OnboardingRouter
import com.example.oblique_android.utils.PinConstants
import com.example.oblique_android.utils.setupWindowInsets
import com.google.android.material.button.MaterialButton

class PinConfirmActivity : AppCompatActivity() {
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_confirm)
        setupWindowInsets(R.id.rootPinConfirm)

        // Ensure Prefs is initialized
        Prefs.init(this)

        val pinViewConfirm = findViewById<PinView>(R.id.pinViewConfirm)
        pinViewConfirm.itemCount = PinConstants.LENGTH
        val togglePassword = findViewById<ImageView>(R.id.togglePasswordConfirm)
        val btnSave = findViewById<MaterialButton>(R.id.btnSavePin)

        val originalPin = intent.getStringExtra("PIN")

        // 👁️ toggle visibility
        togglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            pinViewConfirm.inputType = if (isPasswordVisible) {
                InputType.TYPE_CLASS_NUMBER
            } else {
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            }
            pinViewConfirm.setSelection(pinViewConfirm.text?.length ?: 0)
        }

        btnSave.setOnClickListener {
            val confirmPin = pinViewConfirm.text?.toString()?.trim()

            if (!PinConstants.isValid(confirmPin)) {
                Toast.makeText(
                    this,
                    getString(R.string.pin_enter_length, PinConstants.LENGTH),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (confirmPin != originalPin) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Save PIN securely only after confirmation
            PINManager.savePin(applicationContext, confirmPin!!)
            Prefs.setPinSet(true)

            val next = OnboardingRouter.next(this, validateToken = false)
            startActivity(Intent(this, next))
            finish()
        }
    }
}
