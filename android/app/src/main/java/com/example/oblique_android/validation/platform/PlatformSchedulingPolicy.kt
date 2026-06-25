package com.example.oblique_android.validation.platform

interface PlatformSchedulingPolicy {
    /** Returns delay offsets in milliseconds from [nowMs] for validation runs. */
    fun computeDelays(deadlineMs: Long, nowMs: Long): List<Long>
}
