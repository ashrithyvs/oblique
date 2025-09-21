package com.example.oblique_android.network.api

import retrofit2.http.Body
import retrofit2.http.POST

// Request / Response DTOs for Auth endpoints
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val pin: String? = null  // optional: will be provided from PINManager if available
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserDto(
    val id: String,
    val email: String,
    val name: String
)

data class AuthResponse(
    val token: String,
    val user: UserDto? = null
)

interface AuthApi {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
}
