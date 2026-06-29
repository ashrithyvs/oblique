package com.example.oblique_android.validation.platform

import com.example.oblique_android.utils.PlatformConstants
import com.example.oblique_android.utils.ValidationConstants

class DuolingoPlatformValidator : PlatformGoalValidator {
    override val platformKey: String = PlatformConstants.KEY_DUOLINGO

    override suspend fun validate(context: PlatformValidationContext): PlatformValidationResult {
        return PlatformValidationResult.Failure(
            ValidationConstants.ERROR_UNSUPPORTED,
            "Duolingo validation is not yet implemented",
        )
    }

    override suspend fun verifyUser(username: String): PlatformUserVerification =
        PlatformUserVerification.NotSupported("Duolingo verification is not yet implemented")
}
