package com.example.oblique_android.network.api

import retrofit2.http.*

data class GoalRequest(
    val platform: String,
    val platformUsername: String,
    val targetValue: Int,
    val baselineValue: Int,
    val deadline: String?,
    val title: String,
    val checkIntervalMs: Long = 3600000,
    val evidence: Map<String, Any>? = null,
    val unit: String
)

data class GoalDto(
    val id: String,
    val title: String,
    val platform: String,
    val platformUsername: String,
    val targetValue: Int,
    val baselineValue: Int,
    val status: String,
    val deadline: String?,
    val completedAt: String?,
    val createdAt: String?,
    val unit: String
)

interface GoalsApi {
    @GET("/api/goals")
    suspend fun listGoals(): List<GoalDto>

    @POST("/api/goals")
    suspend fun createGoal(@Body req: GoalRequest): GoalDto

    @DELETE("/api/goals/{id}")
    suspend fun deleteGoal(@Path("id") id: String): Map<String, Any>

    @POST("/api/goals/{id}/complete")
    suspend fun completeGoal(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): GoalDto
}
