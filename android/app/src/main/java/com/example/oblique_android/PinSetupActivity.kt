package com.example.oblique_android

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chaos.view.PinView
import com.google.android.material.button.MaterialButton

class PinSetupActivity : AppCompatActivity() {

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_setup)

        val pinView = findViewById<PinView>(R.id.pinView)
        val togglePassword = findViewById<ImageView>(R.id.togglePassword)
        val btnContinue = findViewById<MaterialButton>(R.id.btnContinuePinSetup)

        // Toggle show/hide PIN
        togglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                pinView.inputType = InputType.TYPE_CLASS_NUMBER
                togglePassword.setImageResource(R.drawable.ic_eye)
            } else {
                pinView.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                togglePassword.setImageResource(R.drawable.ic_eye)
            }
            pinView.setSelection(pinView.text?.length ?: 0)
        }

        // Continue to confirm
        btnContinue.setOnClickListener {
            val pin = pinView.text?.toString()?.trim()

            if (pin.isNullOrEmpty() || pin.length != 6) {
                Toast.makeText(this, "Please enter a 6-digit PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Send entered PIN to confirmation screen
            val intent = Intent(this, PinConfirmActivity::class.java)
            intent.putExtra("PIN", pin)
            startActivity(intent)
            finish()
        }
    }
}
