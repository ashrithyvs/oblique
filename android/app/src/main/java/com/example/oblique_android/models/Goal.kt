package com.example.oblique_android.models

import com.example.oblique_android.network.api.GoalDto
import org.json.JSONObject

data class Goal(
    var id: String = "",
    var platform: String = "",
    var unit: String = "",
    var targetValue: Int = 0,
    var progress: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var deadline: Long = 0L,
    var status: String = "active"
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

    companion object {
        fun fromDto(dto: GoalDto): Goal {
            return Goal(
                id = dto.id,
                platform = dto.platform,
                unit = "", // backend doesn’t send unit
                targetValue = dto.targetValue,
                progress = 0,
                createdAt = System.currentTimeMillis(),
                deadline = 0L,
                status = dto.status
            )
        }
    }
}
