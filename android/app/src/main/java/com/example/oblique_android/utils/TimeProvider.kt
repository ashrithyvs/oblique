package com.example.oblique_android.utils

import com.example.oblique_android.BuildConfig

interface TimeProvider {
    fun nowMs(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun nowMs(): Long = System.currentTimeMillis()
}

/**
 * Dev/test clock. [offsetMs] is added to real time when [BuildConfig.DEV_MODE] allows override.
 */
class OffsetTimeProvider(
    private val base: TimeProvider = SystemTimeProvider,
    private var offsetMs: Long = 0L,
) : TimeProvider {
    override fun nowMs(): Long = base.nowMs() + offsetMs

    fun currentOffsetMs(): Long = offsetMs

    fun setOffsetMs(value: Long) {
        offsetMs = value
    }

    fun advanceMs(delta: Long) {
        offsetMs += delta
    }
}

class FixedTimeProvider(@JvmField var fixedMs: Long) : TimeProvider {
    override fun nowMs(): Long = fixedMs

    fun advanceMs(delta: Long) {
        fixedMs += delta
    }
}

object AppTime {
    @Volatile
    private var provider: TimeProvider = SystemTimeProvider

    fun init(context: android.content.Context) {
        val offset = if (BuildConfig.DEV_MODE) {
            PrefsUtils.getValidationTimeOffsetMs(context)
        } else {
            0L
        }
        provider = OffsetTimeProvider(SystemTimeProvider, offset)
    }

    fun provider(): TimeProvider = provider

    fun setProvider(p: TimeProvider) {
        provider = p
    }

    fun nowMs(): Long = provider.nowMs()

    fun advanceDevTimeMs(context: android.content.Context, delta: Long) {
        val current = provider
        if (current is OffsetTimeProvider) {
            current.advanceMs(delta)
            PrefsUtils.saveValidationTimeOffsetMs(context, current.currentOffsetMs())
        }
    }
}
