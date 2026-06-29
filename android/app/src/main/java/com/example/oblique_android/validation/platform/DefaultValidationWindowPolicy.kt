package com.example.oblique_android.validation.platform

import java.util.Calendar

/**
 * Calendar-day validation window for progress counting.
 *
 * On the deadline's local calendar day, the window runs from midnight through [now] so
 * solves after the deadline clock time still count toward progress. The deadline timestamp
 * is the goal's due time, not a cutoff that stops counting submissions.
 *
 * Before the deadline day: no valid window. After the deadline day: full deadline day.
 */
class DefaultValidationWindowPolicy : ValidationWindowPolicy {

    override fun window(deadlineMs: Long, nowMs: Long): Pair<Long, Long>? {
        if (deadlineMs <= 0L) {
            val dayStart = startOfLocalDay(nowMs)
            return if (dayStart >= nowMs) null else dayStart to nowMs
        }

        val deadlineDayStart = startOfLocalDay(deadlineMs)
        val nowDayStart = startOfLocalDay(nowMs)

        val windowEnd = when {
            nowDayStart < deadlineDayStart -> return null
            nowDayStart > deadlineDayStart -> endOfLocalDay(deadlineMs)
            else -> nowMs
        }

        return if (deadlineDayStart >= windowEnd) null else deadlineDayStart to windowEnd
    }

    private fun startOfLocalDay(timeMs: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun endOfLocalDay(timeMs: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
}
