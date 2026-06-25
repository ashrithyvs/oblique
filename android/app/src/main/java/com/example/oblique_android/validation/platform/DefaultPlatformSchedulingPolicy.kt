package com.example.oblique_android.validation.platform

import com.example.oblique_android.utils.WorkConstants
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Hourly from T−6h until T−10m, plus runs at T−10m, T−5m, T−1m, T−0.
 */
class DefaultPlatformSchedulingPolicy : PlatformSchedulingPolicy {

    override fun computeDelays(deadlineMs: Long, nowMs: Long): List<Long> {
        if (deadlineMs <= nowMs) return emptyList()

        val delays = mutableListOf<Long>()
        val start = deadlineMs - WorkConstants.SCHEDULE_LEAD_MS
        var next = max(nowMs, start)

        while (next < deadlineMs - TimeUnit.MINUTES.toMillis(10)) {
            val delay = next - nowMs
            if (delay > 0) delays.add(delay)
            next += WorkConstants.SCHEDULE_HOURLY_INTERVAL_MS
        }

        for (minutesBefore in WorkConstants.SCHEDULE_FINAL_OFFSETS_MINUTES) {
            val runAt = deadlineMs - TimeUnit.MINUTES.toMillis(minutesBefore)
            val delay = runAt - nowMs
            if (delay > 0) delays.add(delay)
        }

        return delays.distinct().sorted()
    }
}
