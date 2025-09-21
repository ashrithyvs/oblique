package com.example.oblique_android.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.oblique_android.entities.BlockedAppEntity
import com.example.oblique_android.entities.GoalEntity
import com.example.oblique_android.network.api.GoalRequest
import com.example.oblique_android.repository.BlockedAppsRepository
import com.example.oblique_android.repository.GoalsRepository
import com.example.oblique_android.utils.AppDatabase
import kotlinx.coroutines.launch

class GoalsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val goalDao = db.goalDao()
    private val blockedDao = db.blockedAppDao()

    private val goalsRepo = GoalsRepository(application, goalDao)
    private val blockedRepo = BlockedAppsRepository(application, blockedDao)

    val allGoals: LiveData<List<Goal>> = goalDao.getAllLive().map { list ->
        list.map { Goal.fromEntity(it) }
    }
    val allBlockedApps: LiveData<List<BlockedAppEntity>> = blockedDao.getAllLive()

    // Refresh (server -> local)
    fun refreshGoals(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            goalsRepo.refreshFromServer()
            onComplete?.invoke(true)
        }
    }

    // Create via GoalRequest (used by GoalsActivity)
    fun createGoal(req: GoalRequest, onResult: ((Goal?) -> Unit)? = null) {
        viewModelScope.launch {
            val entity = goalsRepo.createGoal(req)
            onResult?.invoke(entity?.let { Goal.fromEntity(it) })
        }
    }

    // Helper: add from Goal model
    fun addGoal(goal: Goal, onResult: ((Goal?) -> Unit)? = null) {
        viewModelScope.launch {
            val req = GoalRequest(
                platform = goal.platform,
                platformUsername = "",
                title = if (goal.title.isNotEmpty()) goal.title else "${goal.platform} ${if (goal.unit.isNotEmpty()) "(${goal.unit})" else ""}",
                unit = goal.unit,
                targetValue = goal.targetValue,
                baselineValue = 0,
                deadline = null,
                checkIntervalMs = 3600000L
            )
            val entity = goalsRepo.createGoal(req)
            onResult?.invoke(entity?.let { Goal.fromEntity(it) })
        }
    }

    // Update (delete+create for now)
    fun updateGoal(goal: Goal, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            goalsRepo.deleteGoal(goal.id)
            val req = GoalRequest(
                platform = goal.platform,
                platformUsername = "",
                title = goal.title,
                unit = goal.unit,
                targetValue = goal.targetValue,
                baselineValue = 0,
                deadline = null,
                checkIntervalMs = 3600000L
            )
            goalsRepo.createGoal(req)
            onResult?.invoke(true)
        }
    }

    // Delete by id
    fun deleteGoal(id: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            goalsRepo.deleteGoal(id)
            onComplete?.invoke()
        }
    }

    fun deleteGoal(goal: Goal, onComplete: (() -> Unit)? = null) {
        deleteGoal(goal.id, onComplete)
    }

    // Complete by id
    fun completeGoal(id: String, evidence: Map<String, Any>? = null, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val updated = goalsRepo.completeGoal(id, evidence)
            onComplete?.invoke(updated != null)
        }
    }

    fun markGoalComplete(goal: Goal, onComplete: ((Boolean) -> Unit)? = null) {
        completeGoal(goal.id, null, onComplete)
    }

    // Blocked apps
    fun refreshBlockedApps(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            blockedRepo.refreshFromServer()
            onComplete?.invoke(true)
        }
    }

    fun addBlockedApp(pkg: String, reason: String? = null, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            blockedRepo.addBlockedApp(pkg, reason)
            onComplete?.invoke(true)
        }
    }

    fun deleteBlockedApp(pkg: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val all = blockedDao.getAll()
            val entity = all.firstOrNull { it.packageName == pkg }
            if (entity != null) {
                blockedRepo.deleteBlockedApp(entity)
            }
            onComplete?.invoke()
        }
    }
}
