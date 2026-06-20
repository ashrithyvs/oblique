package com.example.oblique_android.utils

import android.text.InputType
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.oblique_android.R

class PinKeypadController(
    root: View,
    private val onPinComplete: (String) -> Unit
) {
    private val dotViews: List<ImageView> = listOf(
        root.findViewById(R.id.pinDot0),
        root.findViewById(R.id.pinDot1),
        root.findViewById(R.id.pinDot2),
        root.findViewById(R.id.pinDot3),
        root.findViewById(R.id.pinDot4),
        root.findViewById(R.id.pinDot5),
    )
    private val digitViews: List<TextView> = listOf(
        root.findViewById(R.id.pinDigit0),
        root.findViewById(R.id.pinDigit1),
        root.findViewById(R.id.pinDigit2),
        root.findViewById(R.id.pinDigit3),
        root.findViewById(R.id.pinDigit4),
        root.findViewById(R.id.pinDigit5),
    )
    private val togglePassword: ImageView = root.findViewById(R.id.togglePassword)

    private val pin = StringBuilder()
    private var isVisible = false

    init {
        val toggleTint = togglePassword.context.getColor(R.color.auth_label)
        togglePassword.setColorFilter(toggleTint, android.graphics.PorterDuff.Mode.SRC_IN)

        val digitButtons = listOf(
            R.id.keypad1 to '1', R.id.keypad2 to '2', R.id.keypad3 to '3',
            R.id.keypad4 to '4', R.id.keypad5 to '5', R.id.keypad6 to '6',
            R.id.keypad7 to '7', R.id.keypad8 to '8', R.id.keypad9 to '9',
            R.id.keypad0 to '0',
        )
        digitButtons.forEach { (id, digit) ->
            root.findViewById<View>(id).setOnClickListener { appendDigit(digit) }
        }
        root.findViewById<View>(R.id.keypadBackspace).setOnClickListener { backspace() }
        togglePassword.setOnClickListener {
            isVisible = !isVisible
            togglePassword.setImageResource(
                if (isVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
            )
            togglePassword.setColorFilter(toggleTint, android.graphics.PorterDuff.Mode.SRC_IN)
            updateDisplay()
        }
        updateDisplay()
    }

    fun clear() {
        pin.clear()
        updateDisplay()
    }

    fun currentPin(): String = pin.toString()

    private fun appendDigit(digit: Char) {
        if (pin.length >= PinConstants.LENGTH) return
        pin.append(digit)
        updateDisplay()
        if (pin.length == PinConstants.LENGTH) {
            onPinComplete(pin.toString())
        }
    }

    private fun backspace() {
        if (pin.isEmpty()) return
        pin.deleteCharAt(pin.length - 1)
        updateDisplay()
    }

    private fun updateDisplay() {
        for (i in 0 until PinConstants.LENGTH) {
            if (i < pin.length) {
                if (isVisible) {
                    dotViews[i].visibility = View.INVISIBLE
                    digitViews[i].visibility = View.VISIBLE
                    digitViews[i].text = pin[i].toString()
                } else {
                    dotViews[i].visibility = View.VISIBLE
                    dotViews[i].setImageResource(R.drawable.pin_dot_filled)
                    digitViews[i].visibility = View.INVISIBLE
                }
            } else {
                dotViews[i].visibility = View.VISIBLE
                dotViews[i].setImageResource(R.drawable.pin_dot_empty)
                digitViews[i].visibility = View.INVISIBLE
            }
        }
    }
}

object PasswordFieldHelper {
    fun wireToggle(passwordField: android.widget.EditText, toggle: ImageView) {
        var visible = false
        val toggleTint = toggle.context.getColor(R.color.auth_label)
        toggle.setColorFilter(toggleTint, android.graphics.PorterDuff.Mode.SRC_IN)
        toggle.setOnClickListener {
            visible = !visible
            passwordField.inputType = if (visible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordField.setSelection(passwordField.text?.length ?: 0)
            toggle.setImageResource(if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye)
            toggle.setColorFilter(toggleTint, android.graphics.PorterDuff.Mode.SRC_IN)
        }
    }
}
