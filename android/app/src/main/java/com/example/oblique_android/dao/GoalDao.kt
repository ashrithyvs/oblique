package com.example.oblique_android.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.oblique_android.entities.GoalEntity

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllLive(): LiveData<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<GoalEntity>)

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    // --- Helpers used by GoalsRepository ---
    @Query("DELETE FROM goals")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(goals: List<GoalEntity>) {
        clearAll()
        insertAll(goals)
    }

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): GoalEntity?
}
