package com.example.oblique_android.validation.platform

import com.example.oblique_android.models.Goal
import com.example.oblique_android.repository.GoalsRepository
import com.example.oblique_android.utils.GoalStatusConstants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalProgressSyncerTest {

    private val repo = mockk<GoalsRepository>()
    private val syncer = GoalProgressSyncer(repo)

    private fun goal(progress: Int = 1, target: Int = 5) = Goal(
        id = "g1",
        platform = "leetcode",
        targetValue = target,
        progress = progress,
        status = GoalStatusConstants.ACTIVE,
    )

    @Test
    fun sync_returnsNoChangeWhenProgressUnchanged() = runTest {
        val outcome = syncer.sync(goal(progress = 2), currentValue = 2)
        assertTrue(outcome is SyncOutcome.NoChange)
        coVerify(exactly = 0) { repo.updateGoalProgress(any(), any(), any()) }
    }

    @Test
    fun sync_returnsNoChangeWhenBaselineAdjustedProgressUnchanged() = runTest {
        val g = goal(progress = 2).copy(baselineValue = 3)
        val outcome = syncer.sync(g, currentValue = 5)
        assertTrue(outcome is SyncOutcome.NoChange)
        coVerify(exactly = 0) { repo.updateGoalProgress(any(), any(), any()) }
    }

    @Test
    fun sync_updatesWhenBaselineAdjustedProgressChanges() = runTest {
        val g = goal(progress = 0).copy(baselineValue = 3)
        val updated = g.copy(progress = 2)
        coEvery { repo.updateGoalProgress("g1", 5, any()) } returns updated

        val outcome = syncer.sync(g, currentValue = 5)
        assertTrue(outcome is SyncOutcome.ProgressUpdated)
        coVerify { repo.updateGoalProgress("g1", 5, any()) }
    }

    @Test
    fun sync_updatesProgressViaRepository() = runTest {
        val updated = goal(progress = 3)
        coEvery { repo.updateGoalProgress("g1", 3, any()) } returns updated

        val outcome = syncer.sync(goal(progress = 2), currentValue = 3, evidence = mapOf("k" to "v"))
        assertTrue(outcome is SyncOutcome.ProgressUpdated)
        coVerify { repo.updateGoalProgress("g1", 3, mapOf("k" to "v")) }
    }

    @Test
    fun sync_completesWhenTargetMet() = runTest {
        val completed = goal(progress = 5, target = 5).copy(status = GoalStatusConstants.COMPLETED)
        coEvery { repo.completeGoal("g1", any()) } returns completed

        val outcome = syncer.sync(goal(progress = 2, target = 5), currentValue = 5)
        assertTrue(outcome is SyncOutcome.Completed)
        coVerify { repo.completeGoal("g1", any()) }
    }

    @Test
    fun sync_returnsErrorOnRepositoryFailure() = runTest {
        coEvery { repo.updateGoalProgress(any(), any(), any()) } throws RuntimeException("network")

        val outcome = syncer.sync(goal(), currentValue = 3)
        assertTrue(outcome is SyncOutcome.Error)
    }

    @Test
    fun syncPeriod_updatesViaRecordPeriodProgress() = runTest {
        val g = goal(progress = 0, target = 2)
        val updated = g.copy(progress = 1)
        coEvery { repo.recordPeriodProgress("g1", 1000L, 1, any()) } returns updated

        val outcome = syncer.syncPeriod(g, periodDeadlineMs = 1000L, progress = 1)
        assertTrue(outcome is SyncOutcome.ProgressUpdated)
        coVerify { repo.recordPeriodProgress("g1", 1000L, 1, any()) }
    }

    @Test
    fun syncPeriod_marksPeriodSatisfiedWhenTargetMet() = runTest {
        val g = goal(progress = 1, target = 2)
        val updated = g.copy(progress = 2, lastSatisfiedPeriodDeadlineMs = 1000L)
        coEvery { repo.recordPeriodProgress("g1", 1000L, 2, any()) } returns updated

        val outcome = syncer.syncPeriod(g, periodDeadlineMs = 1000L, progress = 2)
        assertTrue(outcome is SyncOutcome.PeriodSatisfied)
    }
}
