package com.example.oblique_android.utils

object PinConstants {
    const val LENGTH = 6

    fun isValid(pin: String?): Boolean =
        !pin.isNullOrBlank() && pin.length == LENGTH && pin.all { it.isDigit() }
}
