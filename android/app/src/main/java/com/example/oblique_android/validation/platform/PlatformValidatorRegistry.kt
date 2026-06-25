package com.example.oblique_android.validation.platform

import com.example.oblique_android.utils.PlatformConstants

class PlatformValidatorRegistry(
    validators: List<PlatformGoalValidator> = defaultValidators(),
) {
    private val byKey: Map<String, PlatformGoalValidator> =
        validators.associateBy { PlatformConstants.normalizePlatform(it.platformKey) }

    fun get(platform: String): PlatformGoalValidator? =
        byKey[PlatformConstants.normalizePlatform(platform)]

    fun supportedPlatforms(): Set<String> = byKey.keys

    companion object {
        fun defaultValidators(): List<PlatformGoalValidator> = listOf(
            LeetCodePlatformValidator(),
            DuolingoPlatformValidator(),
        )
    }
}
