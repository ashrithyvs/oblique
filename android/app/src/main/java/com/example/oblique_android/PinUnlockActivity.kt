package com.example.oblique_android

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chaos.view.PinView
import com.google.android.material.button.MaterialButton

class PinUnlockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_unlock) // <-- your XML above

        val pinView = findViewById<PinView>(R.id.pinViewUnlock)
        val btnUnlock = findViewById<MaterialButton>(R.id.btnUnlock)

        val blockedApp = intent.getStringExtra("blockedApp")

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
