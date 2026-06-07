package com.example.oblique_android.repository

import android.content.Context
import com.example.oblique_android.models.Goal
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.GoalRequest
import com.example.oblique_android.network.api.GoalsApi

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
            "targetValue" to goal.targetValue
        )
        if (goal.deadline != null && goal.deadline > 0L) {
            body["deadline"] = goal.deadline
        }
        api.updateGoal(goal.id, body)
    }


    suspend fun deleteGoal(id: String) {
        api.deleteGoal(id)
    }

    suspend fun completeGoal(id: String): Goal {
        val body = mapOf("completedAt" to System.currentTimeMillis())
        return Goal.fromDto(api.completeGoal(id, body))
    }
}
