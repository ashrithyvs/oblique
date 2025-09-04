package com.example.oblique_android

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class PromptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prompt)

        val dismissButton: Button = findViewById(R.id.buttonDismiss)
        dismissButton.setOnClickListener {
            finish()
        }
    }
}
