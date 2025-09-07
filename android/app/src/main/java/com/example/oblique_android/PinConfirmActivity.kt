package com.example.oblique_android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chaos.view.PinView
import com.google.android.material.button.MaterialButton

class PinConfirmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_confirm)

        // Ensure Prefs is initialized
        Prefs.init(this)

        val pinViewConfirm = findViewById<PinView>(R.id.pinViewConfirm)
        val btnSave = findViewById<MaterialButton>(R.id.btnSavePin)

        val originalPin = intent.getStringExtra("PIN")

        btnSave.setOnClickListener {
            val confirmPin = pinViewConfirm.text?.toString()?.trim()

            if (confirmPin.isNullOrEmpty() || confirmPin.length != 6) {
                Toast.makeText(this, "Please enter a 6-digit PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (confirmPin != originalPin) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Save PIN securely only after confirmation
            PINManager.savePin(this, confirmPin)
            Prefs.setPinSet(true) // now works properly
            Toast.makeText(this, "PIN saved successfully", Toast.LENGTH_SHORT).show()

            // Move to selecting apps
            startActivity(Intent(this, AppListActivity::class.java))
            finish()
        }
    }
}
