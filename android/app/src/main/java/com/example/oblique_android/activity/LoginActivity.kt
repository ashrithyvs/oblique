package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.oblique_android.R
import com.example.oblique_android.models.AuthViewModel
import com.example.oblique_android.utils.OnboardingRouter
import com.example.oblique_android.utils.setupWindowInsets
import com.example.oblique_android.utils.PermissionUtils

class LoginActivity : AppCompatActivity() {
    private val authVm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setupWindowInsets(R.id.rootLogin)

        val etEmail = findViewById<EditText>(R.id.inputEmail)
        val etPassword = findViewById<EditText>(R.id.inputPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnGoRegister)

        authVm.authResult.observe(this, Observer { outcome ->
            if (outcome.success) {
                val next = OnboardingRouter.next(this)
                startActivity(Intent(this, next))
                finish()
            } else {
                val message = outcome.errorMessage ?: getString(R.string.login_failed)
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        })

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            if (email.isNotBlank() && password.isNotBlank()) {
                authVm.login(email, password)
            }
        }
    }
}
