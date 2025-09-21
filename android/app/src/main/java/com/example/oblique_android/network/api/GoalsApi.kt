package com.example.oblique_android.network.api

import retrofit2.http.*

data class GoalRequest(
    val platform: String,
    val platformUsername: String = "",
    val title: String,
    val unit: String = "",
    val targetValue: Int,
    val baselineValue: Int = 0,
    val deadline: String? = null,
    val checkIntervalMs: Long = 3600000L,
    val evidence: Map<String, Any>? = null
)

data class GoalDto(
    val id: String,
    val title: String,
    val platform: String,
    val platformUsername: String?,
    val unit: String?,
    val targetValue: Int,
    val baselineValue: Int,
    val status: String,
    val deadline: String?,
    val completedAt: String?,
    val evidence: Map<String, Any>?
)

interface GoalsApi {
    @GET("/api/goals")
    suspend fun listGoals(): List<GoalDto>

    @POST("/api/goals")
    suspend fun createGoal(@Body req: GoalRequest): GoalDto

    @GET("/api/goals/{id}")
    suspend fun getGoal(@Path("id") id: String): GoalDto

    @POST("/api/goals/{id}/complete")
    suspend fun completeGoal(@Path("id") id: String, @Body body: Map<String, Any>): GoalDto

    @DELETE("/api/goals/{id}")
    suspend fun deleteGoal(@Path("id") id: String): Map<String, Any>
}
