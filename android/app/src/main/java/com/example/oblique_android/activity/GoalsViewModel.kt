package com.example.oblique_android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.oblique_android.data.GoalDatabase
import com.example.oblique_android.models.Goal
import com.example.oblique_android.repository.GoalRepository
import com.example.oblique_android.utils.GoalUtils
import kotlinx.coroutines.flow.Flow

class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GoalDatabase.getDatabase(application)
    private val repo = GoalRepository(db.goalDao())

    fun getAllGoalsFlow(): Flow<List<Goal>> = repo.getAll()

    suspend fun addGoal(g: Goal) {
        repo.addGoal(g)
    }

    suspend fun deleteGoal(g: Goal) {
        repo.deleteById(g.id)
    }

    suspend fun markGoalComplete(g: Goal) {
        val updated = g.copy(progress = g.targetValue)
        repo.update(updated)
    }

    // seed if empty with helpful guidance examples
    suspend fun seedIfEmpty() {
        val all = repo.getAllOnce()
        if (all.isEmpty()) {
            val l1 = GoalUtils.defaultGoalForPlatform("LeetCode", "problems", 2)
            val d1 = GoalUtils.defaultGoalForPlatform("Duolingo", "time", 15)
            repo.addGoal(l1)
            repo.addGoal(d1)
        }
    }
}
