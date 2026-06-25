package com.example.oblique_android.validation.platform

interface ValidationWindowPolicy {
    /** Returns (startMs, endMs) or null when the window is invalid (start >= end). */
    fun window(deadlineMs: Long, nowMs: Long): Pair<Long, Long>?
}
