package com.example.oblique_android.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.google.android.material.button.MaterialButton

object AuthFormHelper {

    fun wireLoginForm(
        email: EditText,
        password: EditText,
        submitButton: MaterialButton
    ): () -> Unit {
        val validate = {
            submitButton.isEnabled =
                email.text.toString().trim().isNotBlank() &&
                    password.text.toString().isNotBlank()
        }
        val watcher = afterTextChangedWatcher { validate() }
        email.addTextChangedListener(watcher)
        password.addTextChangedListener(watcher)
        validate()
        return validate
    }

    fun wireRegisterForm(
        name: EditText,
        email: EditText,
        password: EditText,
        confirmPassword: EditText,
        submitButton: MaterialButton
    ): () -> Unit {
        val validate = {
            val pwd = password.text.toString()
            val confirm = confirmPassword.text.toString()
            submitButton.isEnabled =
                name.text.toString().trim().isNotBlank() &&
                    email.text.toString().trim().isNotBlank() &&
                    pwd.length >= 8 &&
                    pwd == confirm
        }
        val watcher = afterTextChangedWatcher { validate() }
        name.addTextChangedListener(watcher)
        email.addTextChangedListener(watcher)
        password.addTextChangedListener(watcher)
        confirmPassword.addTextChangedListener(watcher)
        validate()
        return validate
    }

    private fun afterTextChangedWatcher(onChange: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onChange()
            }
        }
    }
}
