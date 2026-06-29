package com.example.oblique_android.validation.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class DefaultValidationWindowPolicyTest {

    private val policy = DefaultValidationWindowPolicy()

    @Test
    fun window_withZeroDeadline_usesStartOfToday() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 26, 15, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val result = policy.window(deadlineMs = 0L, nowMs = now)
        assertNotNull(result)

        val (start, end) = result!!
        val expectedStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals(expectedStart, start)
        assertEquals(now, end)
    }

    @Test
    fun window_withDeadline_onSameDayAfterDeadlineTime_usesNowAsEnd() {
        val deadline = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 26, 18, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = deadline + 3_600_000L

        val result = policy.window(deadline, now)
        assertNotNull(result)

        val (start, end) = result!!
        val expectedStart = Calendar.getInstance().apply {
            timeInMillis = deadline
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals(expectedStart, start)
        assertEquals(now, end)
    }

    @Test
    fun window_withDeadline_beforeDeadlineDay_returnsNull() {
        val deadline = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 28, 18, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 27, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertNull(policy.window(deadline, now))
    }

    @Test
    fun window_returnsNullWhenStartEqualsEnd() {
        val deadline = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 26, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val result = policy.window(deadline, deadline)
        assertNull(result)
    }

    private fun assertNotNull(value: Any?) {
        org.junit.Assert.assertNotNull(value)
    }

    private fun assertNull(value: Any?) {
        org.junit.Assert.assertNull(value)
    }
}
