package com.example.oblique_android.validation

import java.util.Date

data class ValidationResult(
    val totalSolved: Int,
    val byDifficulty: Map<String, Int>,
    val earliest: Date? = null,
    val latest: Date? = null,
    val rawEvidence: Map<String, Any>? = null
)
