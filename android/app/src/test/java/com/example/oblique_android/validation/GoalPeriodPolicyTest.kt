package com.example.oblique_android.validation.platform

import com.example.oblique_android.models.Goal
import com.example.oblique_android.utils.GoalStatusConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class GoalPeriodPolicyTest {

    private val policy = GoalPeriodPolicy()
    private val bufferMs = 3 * 3_600_000L

    private fun dayStart(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun at(dayStart: Long, hour: Int, minute: Int): Long =
        dayStart + hour * 3_600_000L + minute * 60_000L

    private fun goal(
        timeOfDayMs: Long,
        lastSatisfied: Long = 0L,
        progress: Int = 0,
        target: Int = 2,
    ) = Goal(
        id = "g1",
        platform = "leetcode",
        targetValue = target,
        progress = progress,
        status = GoalStatusConstants.ACTIVE,
        deadlineTimeOfDayMs = timeOfDayMs,
        lastSatisfiedPeriodDeadlineMs = lastSatisfied,
    )

    @Test
    fun postDeadlineSolveStillCreditsDuringGrace() {
        val monday = dayStart(2025, Calendar.JUNE, 16)
        val deadline745 = 7 * 3_600_000L + 45 * 60_000L
        val g = goal(deadline745)
        val now = at(monday, 7, 50)

        val period = policy.currentPeriod(g, bufferMs, now)
        assertEquals(at(monday, 7, 45), period.deadlineMs)
        assertFalse(period.forfeited)
    }

    @Test
    fun forfeitAfterBufferExpires() {
        val monday = dayStart(2025, Calendar.JUNE, 16)
        val deadline745 = 7 * 3_600_000L + 45 * 60_000L
        val g = goal(deadline745)
        val monDeadline = at(monday, 7, 45)
        val now = monDeadline + bufferMs + 60_000L

        val period = policy.buildPeriod(g, monDeadline, bufferMs, now)
        assertTrue(period.forfeited)
    }

    @Test
    fun unblockUntilIsNextDeadlinePlusBuffer() {
        val monday = dayStart(2025, Calendar.JUNE, 16)
        val deadline745 = 7 * 3_600_000L + 45 * 60_000L
        val satisfiedDeadline = at(monday, 7, 45)
        val until = policy.unblockUntilMs(satisfiedDeadline, bufferMs)
        assertEquals(at(monday + GoalPeriodPolicy.DAY_MS, 7, 45) + bufferMs, until)
    }

    @Test
    fun satisfiedPeriodUnblocksUntilNextDeadlinePlusBuffer() {
        val monday = dayStart(2025, Calendar.JUNE, 16)
        val deadline745 = 7 * 3_600_000L + 45 * 60_000L
        val g = goal(deadline745, lastSatisfied = at(monday, 7, 45), progress = 2)
        val now = at(monday, 8, 0)
        val until = policy.unblockUntilMs(g, bufferMs, now)
        assertTrue(now < until)
        assertEquals(at(monday + GoalPeriodPolicy.DAY_MS, 7, 45) + bufferMs, until)
    }

    @Test
    fun onlyTodayPeriodOpenAfterSevenDayMiss() {
        val monday = dayStart(2025, Calendar.JUNE, 16)
        val deadline745 = 7 * 3_600_000L + 45 * 60_000L
        val g = goal(deadline745)
        val now = at(monday + 7 * GoalPeriodPolicy.DAY_MS, 8, 0)
        val open = policy.openPeriods(g, bufferMs, now)
        assertEquals(1, open.size)
    }
}
