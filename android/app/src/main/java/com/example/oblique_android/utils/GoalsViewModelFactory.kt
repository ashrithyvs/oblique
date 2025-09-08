package com.example.oblique_android.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.oblique_android.GoalsViewModel

/**
 * Correct, minimal ViewModel factory for GoalsViewModel.
 * Adjust constructor parameters if your GoalsViewModel takes extra deps.
 */
class GoalsViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(GoalsViewModel::class.java)) {
            GoalsViewModel(app) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
