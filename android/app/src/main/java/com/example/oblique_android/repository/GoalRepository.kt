package com.example.oblique_android.repository

import com.example.oblique_android.data.GoalDao
import com.example.oblique_android.models.Goal
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val dao: GoalDao) {

    // Continuous stream of all goals
    fun getAll(): Flow<List<Goal>> = dao.getAllGoals()

    // One-shot (suspend) query of all goals
    suspend fun getAllOnce(): List<Goal> = dao.getAllGoalsOnce()

    suspend fun addGoal(goal: Goal) {
        dao.insert(goal)
    }

    suspend fun update(goal: Goal) {
        dao.update(goal)
    }

    suspend fun deleteById(id: Int) {
        val g = dao.getGoalById(id)
        if (g != null) {
            dao.delete(g)
        }
    }
}
