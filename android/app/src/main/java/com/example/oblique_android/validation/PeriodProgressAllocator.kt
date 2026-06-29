package com.example.oblique_android.validation

import com.example.oblique_android.models.Goal
import com.example.oblique_android.validation.platform.GoalPeriod
import com.example.oblique_android.validation.platform.GoalPeriodPolicy

data class TimestampedSubmission(
    val timestampSec: Long,
    val slug: String = "",
)

data class AllocationResult(
    val progressByDeadline: Map<Long, Int>,
    val currentPeriodProgress: Int,
    val currentPeriodDeadlineMs: Long,
    val totalAssigned: Int,
)

class PeriodProgressAllocator(
    private val periodPolicy: GoalPeriodPolicy = GoalPeriodPolicy(),
) {
    fun allocate(
        goal: Goal,
        submissions: List<TimestampedSubmission>,
        bufferMs: Long,
        nowMs: Long,
        existingProgressByDeadline: Map<Long, Int> = emptyMap(),
    ): AllocationResult {
        val open = periodPolicy.openPeriods(goal, bufferMs, nowMs)
        if (open.isEmpty()) {
            val current = periodPolicy.currentPeriod(goal, bufferMs, nowMs)
            return AllocationResult(emptyMap(), goal.progress, current.deadlineMs, 0)
        }

        val credited = open.associate { it.deadlineMs to (existingProgressByDeadline[it.deadlineMs] ?: 0) }
            .toMutableMap()
        val remaining = open.associate { it.deadlineMs to (goal.targetValue - (credited[it.deadlineMs] ?: 0)) }
            .toMutableMap()

        val sorted = submissions
            .filter { sub ->
                open.any { period ->
                    val (start, end) = periodPolicy.creditWindowForPeriod(period, nowMs)
                    val tsMs = sub.timestampSec * 1000
                    tsMs in (start + 1)..end
                }
            }
            .sortedBy { it.timestampSec }

        var totalAssigned = 0
        for (sub in sorted) {
            val tsMs = sub.timestampSec * 1000
            for (period in open) {
                val need = remaining[period.deadlineMs] ?: 0
                if (need <= 0) continue
                val (start, end) = periodPolicy.creditWindowForPeriod(period, nowMs)
                if (tsMs in (start + 1)..end) {
                    credited[period.deadlineMs] = (credited[period.deadlineMs] ?: 0) + 1
                    remaining[period.deadlineMs] = need - 1
                    totalAssigned++
                    break
                }
            }
        }

        val currentDeadline = periodPolicy.currentPeriodDeadlineMs(goal, nowMs, bufferMs)
        val currentProgress = credited[currentDeadline] ?: 0

        return AllocationResult(
            progressByDeadline = credited,
            currentPeriodProgress = currentProgress,
            currentPeriodDeadlineMs = currentDeadline,
            totalAssigned = totalAssigned,
        )
    }
}
