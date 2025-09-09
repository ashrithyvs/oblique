package com.example.oblique_android.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.oblique_android.models.GoalsViewModel

class GoalsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoalsViewModel::class.java)) {
            return GoalsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
