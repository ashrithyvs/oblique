package com.example.oblique_android.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.oblique_android.repository.AuthRepository
import com.example.oblique_android.models.AuthOutcome

/**
 * AuthViewModel — UI layer for register/login/logout.
 * No Room / AppDatabase dependency any more. AuthRepository is API-first.
 */
class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepository(app)

    private val _authResult = MutableLiveData<AuthOutcome>()
    val authResult: LiveData<AuthOutcome> get() = _authResult

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _authResult.postValue(repo.register(name, email, password))
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authResult.postValue(repo.login(email, password))
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
        }
    }

    fun isLoggedIn(): Boolean = repo.isLoggedIn()
}
