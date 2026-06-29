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

    fun currentOffsetMs(): Long {
        val current = provider
        return if (current is OffsetTimeProvider) current.currentOffsetMs() else 0L
    }

    fun formatNowForDisplay(): String =
        java.text.SimpleDateFormat("EEE, MMM d h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(nowMs()))

    fun formatOffsetForDisplay(offsetMs: Long = currentOffsetMs()): String {
        if (offsetMs == 0L) return "0h"
        val sign = if (offsetMs >= 0) "+" else "-"
        val abs = kotlin.math.abs(offsetMs)
        val hours = abs / 3_600_000L
        val mins = (abs % 3_600_000L) / 60_000L
        return when {
            mins == 0L -> "$sign${hours}h"
            hours == 0L -> "$sign${mins}m"
            else -> "$sign${hours}h ${mins}m"
        }
    }

    fun advanceDevTimeMs(context: android.content.Context, delta: Long) {
        val current = provider
        if (current is OffsetTimeProvider) {
            current.advanceMs(delta)
            PrefsUtils.saveValidationTimeOffsetMs(context, current.currentOffsetMs())
        }
    }

    fun resetDevTimeMs(context: android.content.Context) {
        PrefsUtils.saveValidationTimeOffsetMs(context, 0L)
        provider = OffsetTimeProvider(SystemTimeProvider, 0L)
    }
}
