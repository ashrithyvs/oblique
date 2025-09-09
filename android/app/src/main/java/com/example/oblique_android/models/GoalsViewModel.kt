package com.example.oblique_android.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.oblique_android.data.AppDatabase
import com.example.oblique_android.entities.GoalEntity
import com.example.oblique_android.models.Goal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel bridging Room GoalEntity <-> domain model Goal.
 * Exposes LiveData<List<Goal>> for easy UI observation.
 */
class GoalsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).goalDao()

    private val _allGoals = MutableLiveData<List<Goal>>(emptyList())
    val allGoals: LiveData<List<Goal>> = _allGoals

    init {
        // Observe Flow from Room and map to domain models
        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllGoals().collect { entities ->
                val models = entities.map { Goal.fromEntity(it) }
                withContext(Dispatchers.Main) {
                    _allGoals.value = models
                }
            }
        }
    }

    fun addGoal(goal: Goal) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(goal.toEntity())
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(goal.toEntity())
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(goal.toEntity())
        }
    }

    fun markGoalComplete(goal: Goal) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = goal.copy(progress = goal.progress + 1)
            dao.update(updated.toEntity())
        }
    }
}
