/**
 * Shared Goal → API response mapper used by controllers and services.
 */
export function toGoalDto(g: any) {
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
    };
}

export function toBlockedAppDtos(pkgs: string[]) {
    return pkgs.map((p) => ({ packageName: p }));
}
