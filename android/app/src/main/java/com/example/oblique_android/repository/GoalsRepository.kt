package com.example.oblique_android.repository

import android.content.Context
import com.example.oblique_android.dao.GoalDao
import com.example.oblique_android.entities.GoalEntity
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.GoalRequest
import com.example.oblique_android.network.api.GoalsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.*

class GoalsRepository(
    context: Context,
    private val goalDao: GoalDao
) {
    private val api: GoalsApi by lazy {
        ApiClient.getClient(context).create(GoalsApi::class.java)
    }

    fun observeLocalGoals() = goalDao.getAllLive()

    suspend fun refreshFromServer() = withContext(Dispatchers.IO) {
        try {
            val remote = api.listGoals()
            val entities = remote.map { dto ->
                GoalEntity(
                    id = dto.id,
                    title = dto.title ?: "",
                    platform = dto.platform ?: "leetcode",
                    platformUsername = dto.platformUsername,
                    unit = dto.unit ?: "",
                    baselineValue = dto.baselineValue,
                    targetValue = dto.targetValue,
                    progress = 0,
                    status = dto.status,
                    checkIntervalMs = 3600000L,
                    deadline = dto.deadline?.let { ISOToEpoch(it) } ?: 0L,
                    evidenceJson = dto.evidence?.let { JSONObject(it).toString() },
                    completedAt = dto.completedAt?.let { ISOToEpoch(it) },
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    pendingSync = false
                )
            }
            goalDao.replaceAll(entities)
        } catch (ex: Exception) {
            // log or swallow — keep local cache
        }
    }

    suspend fun createGoal(request: GoalRequest): GoalEntity? = withContext(Dispatchers.IO) {
        try {
            val created = api.createGoal(request)
            val entity = GoalEntity(
                id = created.id,
                title = created.title ?: request.title,
                platform = created.platform ?: request.platform,
                platformUsername = created.platformUsername,
                unit = created.unit ?: request.unit,
                baselineValue = created.baselineValue,
                targetValue = created.targetValue,
                progress = 0,
                status = created.status,
                checkIntervalMs = request.checkIntervalMs,
                deadline = created.deadline?.let { ISOToEpoch(it) } ?: 0L,
                evidenceJson = created.evidence?.let { JSONObject(it).toString() },
                completedAt = created.completedAt?.let { ISOToEpoch(it) },
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                pendingSync = false
            )
            goalDao.insert(entity)
            entity
        } catch (ex: Exception) {
            // offline fallback
            val localId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val entity = GoalEntity(
                id = localId,
                title = request.title,
                platform = request.platform,
                platformUsername = request.platformUsername,
                unit = request.unit,
                baselineValue = request.baselineValue ?: 0,
                targetValue = request.targetValue,
                progress = 0,
                status = "active",
                checkIntervalMs = request.checkIntervalMs,
                deadline = request.deadline?.let { ISOToEpoch(it) } ?: 0L,
                evidenceJson = null,
                completedAt = null,
                createdAt = now,
                updatedAt = now,
                pendingSync = true
            )
            goalDao.insert(entity)
            entity
        }
    }

    suspend fun deleteGoal(goalId: String) = withContext(Dispatchers.IO) {
        try {
            api.deleteGoal(goalId)
        } catch (_: Exception) {
        } finally {
            goalDao.deleteById(goalId)
        }
    }

    suspend fun completeGoal(goalId: String, evidence: Map<String, Any>? = null) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        try {
            val body: MutableMap<String, Any> = mutableMapOf("completedAt" to now)
            if (evidence != null) body["evidence"] = evidence
            val updated = api.completeGoal(goalId, body)
            val entity = GoalEntity(
                id = updated.id,
                title = updated.title ?: "",
                platform = updated.platform ?: "leetcode",
                platformUsername = updated.platformUsername,
                unit = updated.unit ?: "",
                baselineValue = updated.baselineValue,
                targetValue = updated.targetValue,
                progress = 0,
                status = updated.status,
                checkIntervalMs = 3600000L,
                deadline = updated.deadline?.let { ISOToEpoch(it) } ?: 0L,
                evidenceJson = updated.evidence?.let { JSONObject(it).toString() },
                completedAt = updated.completedAt?.let { ISOToEpoch(it) } ?: now,
                createdAt = now,
                updatedAt = now,
                pendingSync = false
            )
            goalDao.insert(entity)
            entity
        } catch (ex: Exception) {
            val local = goalDao.findById(goalId)
            local?.copy(
                status = "completed",
                completedAt = now,
                updatedAt = now,
                pendingSync = true
            )?.also { goalDao.insert(it) }
        }
    }

    private fun ISOToEpoch(iso: String): Long =
        try { java.time.Instant.parse(iso).toEpochMilli() }
        catch (_: Exception) { System.currentTimeMillis() }
}
