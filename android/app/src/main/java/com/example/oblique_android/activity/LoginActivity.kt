package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.oblique_android.R
import com.example.oblique_android.models.AuthViewModel
import androidx.lifecycle.lifecycleScope
import com.example.oblique_android.utils.AuthFormHelper
import com.example.oblique_android.utils.OnboardingRouter
import com.example.oblique_android.utils.PasswordFieldHelper
import com.example.oblique_android.utils.setupWindowInsets
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private val authVm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setupWindowInsets(R.id.rootLogin)

        val etEmail = findViewById<EditText>(R.id.inputEmail)
        val etPassword = findViewById<EditText>(R.id.inputPassword)
        val btnLogin = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLogin)
        val btnRegister = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGoRegister)
        val togglePassword = findViewById<ImageView>(R.id.togglePassword)

        PasswordFieldHelper.wireToggle(etPassword, togglePassword)
        val refreshLoginForm = AuthFormHelper.wireLoginForm(etEmail, etPassword, btnLogin)

        authVm.authResult.observe(this, Observer { outcome ->
            refreshLoginForm()
            if (outcome.success) {
                lifecycleScope.launch {
                    val next = OnboardingRouter.nextSuspend(this@LoginActivity, validateToken = false)
                    startActivity(Intent(this@LoginActivity, next))
                    finish()
                }
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
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, R.string.auth_fill_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnLogin.isEnabled = false
            authVm.login(email, password)
        }
    }
}
