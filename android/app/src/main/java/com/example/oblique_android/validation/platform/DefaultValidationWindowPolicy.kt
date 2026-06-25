package com.example.oblique_android.validation.platform

import java.util.Calendar

/**
 * Calendar-day validation window: start of local day containing deadline → min(deadline, now).
 * When deadline <= 0, window is start of today → now.
 */
class DefaultValidationWindowPolicy : ValidationWindowPolicy {

    override fun window(deadlineMs: Long, nowMs: Long): Pair<Long, Long>? {
        val dayStart = if (deadlineMs <= 0L) {
            Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else {
            Calendar.getInstance().apply {
                timeInMillis = deadlineMs
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val windowEnd = if (deadlineMs <= 0L) nowMs else minOf(deadlineMs, nowMs)
        return if (dayStart >= windowEnd) null else dayStart to windowEnd
    }
}
