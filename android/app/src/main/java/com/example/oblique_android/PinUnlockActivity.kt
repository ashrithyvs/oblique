package com.example.oblique_android

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PinUnlockActivity : AppCompatActivity() {

    private val correctPin = "1234" // TODO: Store securely

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_unlock)

        val edtPin = findViewById<EditText>(R.id.edtPin)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            if (edtPin.text.toString() == correctPin) {
                Toast.makeText(this, "Unlocked!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
