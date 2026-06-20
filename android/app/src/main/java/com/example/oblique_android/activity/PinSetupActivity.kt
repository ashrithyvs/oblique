package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.oblique_android.R
import com.example.oblique_android.utils.PinKeypadController
import com.example.oblique_android.utils.setupWindowInsets

class PinSetupActivity : AppCompatActivity() {

    private lateinit var keypad: PinKeypadController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_setup)
        setupWindowInsets(R.id.rootPinSetup)

        findViewById<TextView>(R.id.tvPinTitle).setText(R.string.pin_create_title)
        findViewById<TextView>(R.id.tvPinSubtitle).setText(R.string.pin_setup_subtitle)

        keypad = PinKeypadController(findViewById(R.id.pinScreenRoot)) { pin ->
            val intent = Intent(this, PinConfirmActivity::class.java)
            intent.putExtra(PinConfirmActivity.EXTRA_PIN, pin)
            startActivity(intent)
            finish()
        }
    }
}
