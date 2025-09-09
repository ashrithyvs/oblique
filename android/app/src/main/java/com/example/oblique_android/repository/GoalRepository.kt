package com.example.oblique_android.repository

import android.content.Context
import com.example.oblique_android.models.Goal
import org.json.JSONArray
import java.util.concurrent.atomic.AtomicInteger

class GoalsRepository private constructor(private val ctx: Context) {

    companion object {
        private const val PREFS = "goals_prefs"
        private const val KEY_GOALS = "goals_json"
        @Volatile
        private var INSTANCE: GoalsRepository? = null

        fun getInstance(context: Context): GoalsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoalsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val idCounter = AtomicInteger(calculateInitialId())

    private fun calculateInitialId(): Int {
        val list = getAll()
        return list.maxOfOrNull { it.id }?.plus(1) ?: 1
    }

    fun getAll(): List<Goal> {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val s = prefs.getString(KEY_GOALS, "[]") ?: "[]"
        val arr = JSONArray(s)
        val out = mutableListOf<Goal>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            out.add(Goal.fromJson(obj))
        }
        return out
    }

    private fun persist(list: List<Goal>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GOALS, arr.toString()).apply()
    }

    fun add(goal: Goal): Goal {
        val current = getAll().toMutableList()
        goal.id = idCounter.getAndIncrement()
        current.add(goal)
        persist(current)
        return goal
    }

    fun update(goal: Goal) {
        val current = getAll().toMutableList()
        val idx = current.indexOfFirst { it.id == goal.id }
        if (idx >= 0) {
            current[idx] = goal
            persist(current)
        }
    }

    fun delete(goal: Goal) {
        val current = getAll().toMutableList()
        val idx = current.indexOfFirst { it.id == goal.id }
        if (idx >= 0) {
            current.removeAt(idx)
            persist(current)
        }
    }
}
