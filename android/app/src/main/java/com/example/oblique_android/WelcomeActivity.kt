package com.example.oblique_android

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)

        findViewById<MaterialButton>(R.id.btnGetStarted).setOnClickListener {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }
    }
}
