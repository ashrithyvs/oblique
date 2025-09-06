package com.example.oblique_android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chaos.view.PinView
import com.google.android.material.button.MaterialButton

class PinUnlockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_unlock)

        val pinView = findViewById<PinView>(R.id.pinViewUnlock)
        val btnUnlock = findViewById<MaterialButton>(R.id.btnUnlock)

        btnUnlock.setOnClickListener {
            val enteredPin = pinView.text?.toString()?.trim()

            if (enteredPin.isNullOrEmpty() || enteredPin.length != 6) {
                Toast.makeText(this, "Please enter your 6-digit PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val savedPin = PINManager.getPin(this)

            if (savedPin == null) {
                Toast.makeText(this, "No PIN set. Please set up a PIN first.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, PinSetupActivity::class.java))
                finish()
                return@setOnClickListener
            }

            if (enteredPin == savedPin) {
                Toast.makeText(this, "Unlocked successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Incorrect PIN. Try again.", Toast.LENGTH_SHORT).show()
                pinView.text?.clear()
            }
        }
    }
}
