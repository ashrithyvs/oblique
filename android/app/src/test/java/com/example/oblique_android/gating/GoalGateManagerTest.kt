package com.example.oblique_android.gating

import android.content.Context
import com.example.oblique_android.models.Goal
import com.example.oblique_android.utils.GoalStatusConstants
import com.example.oblique_android.validation.platform.GoalPeriodPolicy
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GoalGateManagerTest {

    private val policy = GoalPeriodPolicy()
    private val bufferMs = 3 * 3_600_000L
    private val context = mockk<Context>(relaxed = true)
    private val gateManager = GoalGateManager(context)

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

    @Test
    fun satisfiedGoalDoesNotBlockUntilNextDeadlinePlusBuffer() {
        val monday = dayStart(2025, Calendar.JUNE, 16)
        val deadline745 = 7 * 3_600_000L + 45 * 60_000L
        val goal = Goal(
            id = "g1",
            platform = "leetcode",
            targetValue = 2,
            progress = 2,
            status = GoalStatusConstants.ACTIVE,
            deadlineTimeOfDayMs = deadline745,
            lastSatisfiedPeriodDeadlineMs = at(monday, 7, 45),
        )
        val now = at(monday, 8, 0)
        assertFalse(gateManager.shouldBlockGoal(goal, bufferMs, now))
    }

    @Test
    fun reblocksAfterUnblockWindowExpires() {
        val monday = dayStart(2025, Calendar.JUNE, 16)
        val tuesday = monday + GoalPeriodPolicy.DAY_MS
        val deadline745 = 7 * 3_600_000L + 45 * 60_000L
        val goal = Goal(
            id = "g1",
            platform = "leetcode",
            targetValue = 2,
            progress = 2,
            status = GoalStatusConstants.ACTIVE,
            deadlineTimeOfDayMs = deadline745,
            lastSatisfiedPeriodDeadlineMs = at(monday, 7, 45),
        )
        val reblockAt = at(tuesday, 7, 45) + bufferMs
        assertTrue(gateManager.shouldBlockGoal(goal, bufferMs, reblockAt))
    }
}
