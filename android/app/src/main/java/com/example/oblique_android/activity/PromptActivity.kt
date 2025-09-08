package com.example.oblique_android.activity

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.oblique_android.R

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
