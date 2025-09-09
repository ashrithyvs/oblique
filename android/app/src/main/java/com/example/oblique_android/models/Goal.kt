package com.example.oblique_android.models

import com.example.oblique_android.entities.GoalEntity
import org.json.JSONObject

data class Goal(
    var id: Int = 0,
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
        o.put("platform", platform)
        o.put("unit", unit)
        o.put("targetValue", targetValue)
        o.put("progress", progress)
        o.put("createdAt", createdAt)
        o.put("deadline", deadline)
        return o
    }

    fun toEntity(): GoalEntity {
        return GoalEntity(
            id = id,
            platform = platform,
            unit = unit,
            targetValue = targetValue,
            progress = progress,
            createdAt = createdAt,
            deadline = deadline
        )
    }

    companion object {
        fun fromJson(obj: JSONObject): Goal {
            return Goal(
                id = obj.optInt("id", 0),
                platform = obj.optString("platform", ""),
                unit = obj.optString("unit", ""),
                targetValue = obj.optInt("targetValue", 0),
                progress = obj.optInt("progress", 0),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                deadline = obj.optLong("deadline", 0L)
            )
        }

        fun fromEntity(e: GoalEntity): Goal {
            return Goal(
                id = e.id,
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
