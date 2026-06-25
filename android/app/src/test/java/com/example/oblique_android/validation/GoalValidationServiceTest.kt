package com.example.oblique_android.validation

import android.content.Context
import android.util.Log
import com.example.oblique_android.models.Goal
import com.example.oblique_android.repository.GoalsRepository
import com.example.oblique_android.utils.GoalStatusConstants
import com.example.oblique_android.utils.PlatformConstants
import com.example.oblique_android.utils.PrefsUtils
import com.example.oblique_android.validation.platform.DefaultValidationWindowPolicy
import com.example.oblique_android.validation.platform.GoalProgressSyncer
import com.example.oblique_android.validation.platform.PlatformGoalValidator
import com.example.oblique_android.validation.platform.PlatformValidationContext
import com.example.oblique_android.validation.platform.PlatformValidationResult
import com.example.oblique_android.validation.platform.PlatformValidatorRegistry
import com.example.oblique_android.validation.platform.SyncOutcome
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GoalValidationServiceTest {

    private val context = mockk<Context>(relaxed = true)
    private val mockValidator = mockk<PlatformGoalValidator> {
        every { platformKey } returns PlatformConstants.KEY_LEETCODE
    }
    private val mockSyncer = mockk<GoalProgressSyncer>()
    private val registry = PlatformValidatorRegistry(listOf(mockValidator))

    private val service = GoalValidationService(
        context = context,
        registry = registry,
        windowPolicy = DefaultValidationWindowPolicy(),
        syncer = mockSyncer,
        goalsRepo = mockk(relaxed = true),
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun activeGoal() = Goal(
        id = "g1",
        platform = PlatformConstants.KEY_LEETCODE,
        targetValue = 5,
        progress = 1,
        status = GoalStatusConstants.ACTIVE,
        deadline = Calendar.getInstance().apply {
            add(Calendar.HOUR, 2)
        }.timeInMillis,
    )

    @Test
    fun validateGoal_returnsNoChangeWhenValidatorReportsNoChange() = runTest {
        coEvery { mockValidator.validate(any()) } returns PlatformValidationResult.NoChange(1)

        val outcome = service.validateGoal(activeGoal(), "user1")
        assertTrue(outcome is ValidationOutcome.NoChange)
    }

    @Test
    fun validateGoal_syncsOnSuccess() = runTest {
        coEvery { mockValidator.validate(any()) } returns PlatformValidationResult.Success(
            currentValue = 3,
            evidence = mapOf("solved" to 3),
        )
        coEvery { mockSyncer.sync(any(), 3, any()) } returns SyncOutcome.ProgressUpdated(activeGoal().copy(progress = 3))

        val outcome = service.validateGoal(activeGoal(), "user1")
        assertTrue(outcome is ValidationOutcome.Updated)
    }

    @Test
    fun validateGoal_returnsFailedWhenNoValidator() = runTest {
        val emptyRegistry = PlatformValidatorRegistry(emptyList())
        val svc = GoalValidationService(
            context = context,
            registry = emptyRegistry,
            syncer = mockSyncer,
            goalsRepo = mockk(relaxed = true),
        )

        val outcome = svc.validateGoal(activeGoal(), "user1")
        assertTrue(outcome is ValidationOutcome.Failed)
    }

    @Test
    fun validateGoal_skipsNonActiveGoals() = runTest {
        val paused = activeGoal().copy(status = GoalStatusConstants.PAUSED)
        val outcome = service.validateGoal(paused, "user1")
        assertTrue(outcome is ValidationOutcome.Skipped)
    }

    @Test
    fun validateGoalById_normalizesPlatformKeyForUsernameLookup() = runTest {
        mockkStatic(PrefsUtils::class)
        val goalsRepo = mockk<GoalsRepository>()
        val goalWithWhitespace = activeGoal().copy(platform = " ${PlatformConstants.KEY_LEETCODE} ")
        coEvery { goalsRepo.listGoals() } returns listOf(goalWithWhitespace)
        every {
            PrefsUtils.getPlatformUsername(context, PlatformConstants.KEY_LEETCODE)
        } returns "user1"
        coEvery { mockValidator.validate(any()) } returns PlatformValidationResult.NoChange(1)

        val svc = GoalValidationService(
            context = context,
            registry = registry,
            syncer = mockSyncer,
            goalsRepo = goalsRepo,
        )

        svc.validateGoalById("g1")

        verify(exactly = 1) {
            PrefsUtils.getPlatformUsername(context, PlatformConstants.KEY_LEETCODE)
        }
    }
}
