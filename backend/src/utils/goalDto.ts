/**
 * Shared Goal → API response mapper used by controllers and services.
 */
export const MAX_DEADLINE_BUFFER_MS = 3 * 60 * 60 * 1000;

export function deriveDeadlineTimeOfDayMs(deadline: number | null | undefined): number | null {
    if (deadline == null || deadline <= 0) return null;
    const d = new Date(deadline);
    return (
        d.getHours() * 3_600_000 +
        d.getMinutes() * 60_000 +
        d.getSeconds() * 1_000 +
        d.getMilliseconds()
    );
}

export function isCurrentPeriodSatisfied(g: {
    progress?: number;
    targetValue?: number;
    lastSatisfiedPeriodDeadlineMs?: number | null;
    deadlineTimeOfDayMs?: number | null;
    deadline?: number | null;
}, nowMs: number = Date.now()): boolean {
    const target = g.targetValue ?? 0;
    const progress = g.progress ?? 0;
    const timeOfDay = g.deadlineTimeOfDayMs ?? deriveDeadlineTimeOfDayMs(g.deadline ?? null);
    if (timeOfDay == null) return progress >= target;

    const dayStart = new Date(nowMs);
    dayStart.setHours(0, 0, 0, 0);
    const todayDeadline = dayStart.getTime() + timeOfDay;
    const currentDeadline = nowMs >= todayDeadline
        ? todayDeadline
        : todayDeadline - 86_400_000;

    if ((g.lastSatisfiedPeriodDeadlineMs ?? 0) >= currentDeadline) return true;
    return progress >= target;
}

export function toGoalDto(g: any) {
    const deadlineTimeOfDayMs =
        g.deadlineTimeOfDayMs ?? deriveDeadlineTimeOfDayMs(g.deadline ?? null);
    const lastSatisfiedPeriodDeadlineMs = g.lastSatisfiedPeriodDeadlineMs ?? null;
    return {
        id: g._id ?? g.id,
        title: g.title,
        platform: g.platform,
        platformUsername: g.platformUsername ?? null,
        unit: g.unit ?? '',
        baselineValue: g.baselineValue ?? 0,
        targetValue: g.targetValue,
        progress: g.progress ?? 0,
        status: g.status,
        checkIntervalMs: g.checkIntervalMs,
        lastCheckedAt: g.lastCheckedAt ?? null,
        completedAt: g.completedAt ?? null,
        completedByDevice: g.completedByDevice ?? false,
        evidence: g.evidence ?? null,
        createdAt: g.createdAt,
        updatedAt: g.updatedAt,
        deadline: g.deadline ?? null,
        deadlineTimeOfDayMs,
        lastSatisfiedPeriodDeadlineMs,
        isCurrentPeriodSatisfied: isCurrentPeriodSatisfied({
            progress: g.progress,
            targetValue: g.targetValue,
            lastSatisfiedPeriodDeadlineMs,
            deadlineTimeOfDayMs,
            deadline: g.deadline,
        }),
    };
}

export function toBlockedAppDtos(pkgs: string[]) {
    return pkgs.map((p) => ({ packageName: p }));
}
