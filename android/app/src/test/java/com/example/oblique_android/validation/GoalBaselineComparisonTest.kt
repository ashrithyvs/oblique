package com.example.oblique_android.validation

import com.example.oblique_android.models.Goal
import com.example.oblique_android.utils.GoalStatusConstants
import com.example.oblique_android.validation.platform.GoalProgressSyncer
import com.example.oblique_android.validation.platform.PlatformValidationResult
import com.example.oblique_android.validation.platform.SyncOutcome
import com.example.oblique_android.repository.GoalsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalBaselineComparisonTest {

    @Test
    fun computedProgress_subtractsBaseline() {
        val goal = Goal(baselineValue = 3, progress = 1)
        assertEquals(2, goal.computedProgress(5))
        assertEquals(0, goal.computedProgress(2))
    }

    @Test
    fun syncer_treatsBaselineAdjustedProgressAsUnchanged() = runTest {
        val repo = mockk<GoalsRepository>()
        val syncer = GoalProgressSyncer(repo)
        val goal = Goal(
            id = "g1",
            platform = "leetcode",
            baselineValue = 3,
            progress = 2,
            targetValue = 5,
            status = GoalStatusConstants.ACTIVE,
        )

        val outcome = syncer.sync(goal, currentValue = 5)
        assertTrue(outcome is SyncOutcome.NoChange)
        coVerify(exactly = 0) { repo.updateGoalProgress(any(), any(), any()) }
    }

    @Test
    fun noChange_whenComputedProgressMatchesStored() {
        val goal = Goal(baselineValue = 4, progress = 1, targetValue = 5)
        val currentValue = 5
        val computed = goal.computedProgress(currentValue)
        val result = if (computed == goal.progress) {
            PlatformValidationResult.NoChange(currentValue)
        } else {
            PlatformValidationResult.Success(currentValue)
        }
        assertTrue(result is PlatformValidationResult.NoChange)
    }

    @Test
    fun success_whenComputedProgressDiffersFromStored() {
        val goal = Goal(baselineValue = 4, progress = 0, targetValue = 5)
        val currentValue = 5
        val computed = goal.computedProgress(currentValue)
        val result = if (computed == goal.progress) {
            PlatformValidationResult.NoChange(currentValue)
        } else {
            PlatformValidationResult.Success(currentValue)
        }
        assertTrue(result is PlatformValidationResult.Success)
        assertEquals(1, computed)
    }
}
