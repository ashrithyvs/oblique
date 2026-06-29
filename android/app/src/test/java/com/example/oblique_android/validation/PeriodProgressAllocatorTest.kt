package com.example.oblique_android.validation

import com.example.oblique_android.models.Goal
import com.example.oblique_android.utils.GoalStatusConstants
import com.example.oblique_android.validation.platform.GoalPeriodPolicy
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class PeriodProgressAllocatorTest {

    private val policy = GoalPeriodPolicy()
    private val allocator = PeriodProgressAllocator(policy)
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

    @Test
    fun fifoAssignsOldestOpenPeriodFirst() {
        val monday = dayStart(2025, Calendar.JUNE, 16)
        val tuesday = monday + GoalPeriodPolicy.DAY_MS
        val deadline745 = 7 * 3_600_000L + 45 * 60_000L
        val monDeadline = at(monday, 7, 45)
        val tueDeadline = at(tuesday, 7, 45)
        val dayBuffer = GoalPeriodPolicy.DAY_MS

        val goal = Goal(
            id = "g1",
            platform = "leetcode",
            targetValue = 2,
            progress = 0,
            status = GoalStatusConstants.ACTIVE,
            deadlineTimeOfDayMs = deadline745,
        )

        val now = at(tuesday, 6, 0)
        val submissions = listOf(
            TimestampedSubmission(at(monday, 8, 0) / 1000),
            TimestampedSubmission(at(monday, 9, 0) / 1000),
            TimestampedSubmission(at(tuesday, 5, 0) / 1000),
        )

        val result = allocator.allocate(goal, submissions, dayBuffer, now)
        assertEquals(2, result.progressByDeadline[monDeadline])
        assertEquals(1, result.progressByDeadline[tueDeadline])
        assertEquals(1, result.currentPeriodProgress)
    }
}
