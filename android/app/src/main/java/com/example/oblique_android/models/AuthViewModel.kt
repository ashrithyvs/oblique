package com.example.oblique_android.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.oblique_android.repository.AuthRepository

/**
 * AuthViewModel — UI layer for register/login/logout.
 * No Room / AppDatabase dependency any more. AuthRepository is API-first.
 */
class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepository(app)

    private val _authResult = MutableLiveData<Boolean>()
    val authResult: LiveData<Boolean> get() = _authResult

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            val success = repo.register(name, email, password)
            _authResult.postValue(success)
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val success = repo.login(email, password)
            _authResult.postValue(success)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
        }
    }

    fun isLoggedIn(): Boolean = repo.isLoggedIn()
}
