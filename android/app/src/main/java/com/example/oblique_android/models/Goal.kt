package com.example.oblique_android.models

import com.example.oblique_android.network.api.GoalDto
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Goal(
    var id: String = "",
    var platform: String = "",
    var unit: String = "",
    var targetValue: Int = 0,
    var baselineValue: Int = 0,
    var progress: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var deadline: Long = 0L,
    var deadlineTimeOfDayMs: Long = 0L,
    var lastSatisfiedPeriodDeadlineMs: Long = 0L,
    var status: String = "active",
) {
    fun computedProgress(currentValue: Int): Int =
        (currentValue - baselineValue).coerceAtLeast(0)

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

        // Robust parser for both numeric and ISO 8601 string deadlines
        private fun parseDeadline(deadline: Any?): Long {
            if (deadline == null) return 0L

            return when (deadline) {
                is Number -> deadline.toLong() // ✅ backend sends Long
                is String -> {
                    if (deadline.isBlank()) return 0L
                    try {
                        deadline.toLong() // numeric string
                    } catch (e: NumberFormatException) {
                        try {
                            // ISO 8601 string fallback
                            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                            fmt.timeZone = TimeZone.getTimeZone("UTC")
                            fmt.parse(deadline)?.time ?: 0L
                        } catch (e2: Exception) {
                            0L
                        }
                    }
                }
                else -> 0L
            }
        }

        fun fromDto(dto: GoalDto): Goal {
            val deadlineMs = parseDeadline(dto.deadline)
            val timeOfDay = dto.deadlineTimeOfDayMs?.takeIf { it > 0 }
                ?: deriveTimeOfDayFromDeadline(deadlineMs)
            val progressValue = when (dto.status.lowercase(Locale.ROOT)) {
                "completed" -> dto.targetValue
                else -> dto.progress.coerceAtLeast(0)
            }

            return Goal(
                id = dto.id,
                platform = dto.platform,
                unit = dto.unit.ifBlank { "unknown" },
                targetValue = dto.targetValue,
                baselineValue = dto.baselineValue.coerceAtLeast(0),
                progress = progressValue,
                createdAt = System.currentTimeMillis(),
                deadline = deadlineMs,
                deadlineTimeOfDayMs = timeOfDay,
                lastSatisfiedPeriodDeadlineMs = dto.lastSatisfiedPeriodDeadlineMs ?: 0L,
                status = dto.status,
            )
        }

        private fun deriveTimeOfDayFromDeadline(deadlineMs: Long): Long {
            if (deadlineMs <= 0L) return 0L
            val cal = Calendar.getInstance().apply { timeInMillis = deadlineMs }
            return (
                cal.get(Calendar.HOUR_OF_DAY) * 3_600_000L +
                    cal.get(Calendar.MINUTE) * 60_000L +
                    cal.get(Calendar.SECOND) * 1_000L +
                    cal.get(Calendar.MILLISECOND)
                )
        }
    }
}
