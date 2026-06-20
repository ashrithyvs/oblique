package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.oblique_android.R
import com.example.oblique_android.models.AuthViewModel
import com.example.oblique_android.utils.AuthFormHelper
import com.example.oblique_android.utils.OnboardingRouter
import com.example.oblique_android.utils.PasswordFieldHelper
import com.example.oblique_android.utils.setupWindowInsets
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class RegisterActivity : AppCompatActivity() {
    private val authVm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        setupWindowInsets(R.id.rootRegister)

        val etName = findViewById<EditText>(R.id.inputName)
        val etEmail = findViewById<EditText>(R.id.inputEmail)
        val etPassword = findViewById<EditText>(R.id.inputPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.inputConfirmPassword)
        val btnRegister = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRegister)
        val btnGoLogin = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGoLogin)

        PasswordFieldHelper.wireToggle(etPassword, findViewById(R.id.togglePassword))
        PasswordFieldHelper.wireToggle(etConfirmPassword, findViewById(R.id.toggleConfirmPassword))
        val refreshRegisterForm = AuthFormHelper.wireRegisterForm(
            etName, etEmail, etPassword, etConfirmPassword, btnRegister
        )

        authVm.authResult.observe(this, Observer { outcome ->
            refreshRegisterForm()
            if (outcome.success) {
                lifecycleScope.launch {
                    val next = OnboardingRouter.nextSuspend(this@RegisterActivity, validateToken = false)
                    startActivity(Intent(this@RegisterActivity, next))
                    finish()
                }
            } else {
                val message = outcome.errorMessage ?: getString(R.string.registration_failed)
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        })

        btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                Toast.makeText(this, R.string.auth_fill_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 8) {
                Toast.makeText(this, R.string.auth_password_min_length, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, R.string.auth_password_mismatch, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            authVm.register(name, email, password)
        }
    }
}
