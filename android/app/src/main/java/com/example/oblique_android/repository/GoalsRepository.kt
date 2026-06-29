package com.example.oblique_android.repository

import android.content.Context
import com.example.oblique_android.models.Goal
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.GoalRequest
import com.example.oblique_android.network.api.GoalsApi
import com.example.oblique_android.utils.ValidationDevLogger

class GoalsRepository(context: Context) {
    private val api: GoalsApi = ApiClient.getClient(context).create(GoalsApi::class.java)

    suspend fun listGoals(): List<Goal> {
        return api.listGoals().map { Goal.fromDto(it) }
    }

    suspend fun createGoal(req: GoalRequest): Goal {
        return Goal.fromDto(api.createGoal(req))
    }

    suspend fun updateGoal(goal: Goal) {
        val body = mutableMapOf<String, Any>(
            "targetValue" to goal.targetValue,
        )
        if (goal.deadline != null && goal.deadline > 0L) {
            body["deadline"] = goal.deadline
        }
        api.updateGoal(goal.id, body)
    }

    suspend fun deleteGoal(id: String) {
        api.deleteGoal(id)
    }

    suspend fun completeGoal(id: String, evidence: Map<String, Any>? = null): Goal {
        val body = mutableMapOf<String, Any>(
            "completedAt" to System.currentTimeMillis(),
        )
        if (evidence != null) {
            body["evidence"] = evidence
        }
        val path = "/api/goals/$id/complete"
        ValidationDevLogger.logApiRequest("POST", path, body)
        return try {
            val dto = api.completeGoal(id, body)
            ValidationDevLogger.logApiResponse("POST", path, dto)
            Goal.fromDto(dto)
        } catch (e: Exception) {
            ValidationDevLogger.logApiError("POST", path, body, e)
            throw e
        }
    }

    suspend fun updateGoalProgress(
        id: String,
        currentValue: Int,
        evidence: Map<String, Any>? = null,
    ): Goal {
        val body = mutableMapOf<String, Any>("progress" to currentValue)
        if (evidence != null) {
            body["evidence"] = evidence
        }
        val path = "/api/goals/$id/progress"
        ValidationDevLogger.logApiRequest("PATCH", path, body)
        return try {
            val dto = api.updateGoalProgress(id, body)
            ValidationDevLogger.logApiResponse("PATCH", path, dto)
            Goal.fromDto(dto)
        } catch (e: Exception) {
            ValidationDevLogger.logApiError("PATCH", path, body, e)
            throw e
        }
    }

    suspend fun recordPeriodProgress(
        id: String,
        periodDeadlineMs: Long,
        progress: Int,
        evidence: Map<String, Any>? = null,
    ): Goal {
        val body = mutableMapOf<String, Any>(
            "periodDeadlineMs" to periodDeadlineMs,
            "progress" to progress,
        )
        if (evidence != null) {
            body["evidence"] = evidence
        }
        val path = "/api/goals/$id/period-progress"
        ValidationDevLogger.logApiRequest("PATCH", path, body)
        return try {
            val dto = api.recordPeriodProgress(id, body)
            ValidationDevLogger.logApiResponse("PATCH", path, dto)
            Goal.fromDto(dto)
        } catch (e: Exception) {
            ValidationDevLogger.logApiError("PATCH", path, body, e)
            throw e
        }
    }

    suspend fun setGoalBaseline(
        id: String,
        baselineValue: Int,
        platformUsername: String? = null,
        evidence: Map<String, Any>? = null,
    ): Goal {
        val body = mutableMapOf<String, Any>("baselineValue" to baselineValue)
        if (!platformUsername.isNullOrBlank()) {
            body["platformUsername"] = platformUsername
        }
        if (evidence != null) {
            body["evidence"] = evidence
        }
        val path = "/api/goals/$id/baseline"
        ValidationDevLogger.logApiRequest("PATCH", path, body)
        return try {
            val dto = api.setGoalBaseline(id, body)
            ValidationDevLogger.logApiResponse("PATCH", path, dto)
            Goal.fromDto(dto)
        } catch (e: Exception) {
            ValidationDevLogger.logApiError("PATCH", path, body, e)
            throw e
        }
    }
}
