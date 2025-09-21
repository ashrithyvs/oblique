package com.example.oblique_android.models

import com.example.oblique_android.entities.GoalEntity
import org.json.JSONObject

data class Goal(
    var id: String = "",
    var title: String = "",
    var platform: String = "",
    var unit: String = "",
    var targetValue: Int = 0,
    var progress: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var deadline: Long = 0L
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("title", title)
        o.put("platform", platform)
        o.put("unit", unit)
        o.put("targetValue", targetValue)
        o.put("progress", progress)
        o.put("createdAt", createdAt)
        o.put("deadline", deadline)
        return o
    }

    companion object {
        fun fromEntity(e: GoalEntity): Goal {
            val derivedTitle = if (e.title.isNotEmpty()) e.title else if (e.unit.isNotEmpty()) "${e.platform} (${e.unit})" else e.platform
            return Goal(
                id = e.id,
                title = derivedTitle,
                platform = e.platform,
                unit = e.unit,
                targetValue = e.targetValue,
                progress = e.progress,
                createdAt = e.createdAt,
                deadline = e.deadline
            )
        }
    }
}
