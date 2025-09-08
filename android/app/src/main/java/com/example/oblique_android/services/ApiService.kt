package com.example.oblique_android

import retrofit2.http.Body
import retrofit2.http.POST

data class VerifyRequest(val goalPlatform: String, val userIdentifier: String)
data class VerifyResponse(val success: Boolean, val value: Int, val message: String? = null)

interface ApiService {

    @POST("api/verify/leetcode")
    suspend fun verifyLeetCode(@Body req: VerifyRequest): VerifyResponse

    @POST("api/verify/duolingo")
    suspend fun verifyDuolingo(@Body req: VerifyRequest): VerifyResponse
}
