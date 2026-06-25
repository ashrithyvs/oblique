package com.example.oblique_android.models

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.oblique_android.network.api.GoalRequest
import com.example.oblique_android.repository.BlockedAppsRepository
import com.example.oblique_android.repository.DashboardRepository
import com.example.oblique_android.repository.GoalsRepository
import com.example.oblique_android.validation.GoalValidationScheduleManager
import kotlinx.coroutines.launch

class GoalsViewModel(application: Application) : AndroidViewModel(application) {

    private val goalsRepo = GoalsRepository(application)
    private val blockedRepo = BlockedAppsRepository(application)
    private val dashboardRepo = DashboardRepository(application)
    private val scheduleManager = GoalValidationScheduleManager(application)

    private val _goals = MutableLiveData<List<Goal>>(emptyList())
    val allGoals: LiveData<List<Goal>> = _goals

    private val _blockedApps = MutableLiveData<List<String>>(emptyList())
    val allBlockedApps: LiveData<List<String>> = _blockedApps

    fun refreshGoals() {
        viewModelScope.launch {
            try {
                _goals.postValue(goalsRepo.listGoals())
            } catch (_: Exception) { }
        }
    }

    fun createGoal(req: GoalRequest, onResult: ((Goal?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val newGoal = goalsRepo.createGoal(req)
                Log.i("GoalsViewModel", newGoal.toString())
                _goals.postValue((_goals.value ?: emptyList()) + newGoal)
                scheduleManager.scheduleGoal(newGoal)
                onResult?.invoke(newGoal)
            } catch (e: Exception) {
                Log.e("GoalsViewModel", "Failed to create goal", e)
                onResult?.invoke(null)
            }
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            try {
                goalsRepo.updateGoal(goal)
                refreshGoals()
                scheduleManager.rescheduleGoal(goal)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteGoal(id: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                goalsRepo.deleteGoal(id)
                scheduleManager.cancelGoal(id)
                _goals.postValue(_goals.value?.filter { it.id != id })
                onComplete?.invoke()
            } catch (_: Exception) { }
        }
    }

    @Deprecated(
        message = "Use GoalValidationService + GoalProgressSyncer for completion with evidence",
        replaceWith = ReplaceWith("GoalProgressSyncer(GoalsRepository(getApplication())).sync(goal, currentValue, evidence)"),
    )
    fun completeGoal(id: String, onComplete: ((Goal?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val updated = goalsRepo.completeGoal(id)
                scheduleManager.cancelGoal(id)
                _goals.postValue(_goals.value?.map { if (it.id == id) updated else it })
                onComplete?.invoke(updated)
            } catch (_: Exception) {
                onComplete?.invoke(null)
            }
        }
    }

    fun refreshBlockedApps() {
        viewModelScope.launch {
            try {
                _blockedApps.postValue(blockedRepo.listBlockedApps())
            } catch (_: Exception) { }
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            try {
                val dash = dashboardRepo.fetchDashboard() ?: return@launch
                _goals.postValue(dash.goals.map { Goal.fromDto(it) })
                _blockedApps.postValue(dash.blockedApps)
            } catch (_: Exception) {
                refreshGoals()
                refreshBlockedApps()
            }
        }
    }

    fun addBlockedApp(pkg: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                _blockedApps.postValue(blockedRepo.addBlockedApp(pkg))
                onComplete?.invoke()
            } catch (_: Exception) { }
        }
    }

    fun removeBlockedApp(pkg: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                blockedRepo.removeBlockedApp(pkg)
                _blockedApps.postValue(_blockedApps.value?.filter { it != pkg })
                onComplete?.invoke()
            } catch (_: Exception) { }
        }
    }

    fun replaceBlockedApps(apps: List<String>) {
        viewModelScope.launch {
            try {
                val updated = blockedRepo.replaceBlockedApps(apps)
                _blockedApps.postValue(updated)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
