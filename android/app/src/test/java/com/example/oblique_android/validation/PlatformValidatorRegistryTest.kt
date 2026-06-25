package com.example.oblique_android.validation.platform

import com.example.oblique_android.utils.PlatformConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlatformValidatorRegistryTest {

    @Test
    fun get_returnsLeetCodeValidator() {
        val registry = PlatformValidatorRegistry()
        val validator = registry.get(PlatformConstants.KEY_LEETCODE)
        assertNotNull(validator)
        assertEquals(PlatformConstants.KEY_LEETCODE, validator?.platformKey)
    }

    @Test
    fun get_returnsDuolingoValidator() {
        val registry = PlatformValidatorRegistry()
        val validator = registry.get(PlatformConstants.KEY_DUOLINGO)
        assertNotNull(validator)
    }

    @Test
    fun get_normalizesPlatformKey() {
        val registry = PlatformValidatorRegistry()
        assertNotNull(registry.get("LeetCode"))
    }

    @Test
    fun get_returnsNullForUnknownPlatform() {
        val registry = PlatformValidatorRegistry()
        assertNull(registry.get("unknown"))
    }

    @Test
    fun supportedPlatforms_includesKnownPlatforms() {
        val registry = PlatformValidatorRegistry()
        val platforms = registry.supportedPlatforms()
        assert(platforms.contains(PlatformConstants.KEY_LEETCODE))
        assert(platforms.contains(PlatformConstants.KEY_DUOLINGO))
    }
}
