package com.example.oblique_android.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oblique_android.repository.AuthRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.oblique_android.utils.AppDatabase

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getDatabase(app)
    private val repo = AuthRepository(app, db.goalDao(), db.blockedAppDao())

    private val _authResult = MutableLiveData<Boolean>()
    val authResult: LiveData<Boolean> get() = _authResult

    fun register(name:String, email: String, password: String) {
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

    fun logout(){
        viewModelScope.launch {
            repo.logout()
        }
    }
    fun isLoggedIn() = repo.isLoggedIn()
}
