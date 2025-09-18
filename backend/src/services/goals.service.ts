// src/services/goals.service.ts
import Goal, { IGoal } from '../models/goal.model';
import GoalCheckHistory from '../models/goalCheckHistory.model';
import mongoose, { Types } from 'mongoose';

/**
 * Create a new goal for a user.
 */
export async function createGoal(userId: string, data: {
    platform?: string;
    platformUsername?: string;
    baselineValue?: number | null;
    targetValue: number;
    deadline?: string | null;
    checkIntervalMs?: number;
    title?: string;
    evidence?: any;
}): Promise<IGoal> {
    const doc: Partial<IGoal> = {
        user: new Types.ObjectId(userId),
        platform: data.platform ?? 'leetcode',
        platformUsername: data.platformUsername ?? '',
        baselineValue: typeof data.baselineValue === 'number' ? data.baselineValue : 0,
        targetValue: data.targetValue,
        deadline: data.deadline ? new Date(data.deadline as any) : null,
        checkIntervalMs: data.checkIntervalMs ?? 3600000,
        title: data.title ?? '',
        evidence: data.evidence ?? null
    } as any;

    const created = await Goal.create(doc);
    return created;
}

/**
 * List goals for a user. Optional `since` returns only updated goals since that date.
 */
export async function listGoals(userId: string, since?: Date | undefined) {
    const q: any = { user: new Types.ObjectId(userId) };
    if (since) {
        q.updatedAt = { $gte: since };
    }
    const docs = await Goal.find(q).sort({ createdAt: -1 }).lean().exec();
    return docs.map(d => ({
        id: d._id,
        title: d.title,
        platform: d.platform,
        platformUsername: d.platformUsername,
        baselineValue: d.baselineValue,
        targetValue: d.targetValue,
        status: d.status,
        deadline: d.deadline,
        checkIntervalMs: d.checkIntervalMs,
        evidence: d.evidence,
        completedAt: d.completedAt ?? null,
        createdAt: d.createdAt,
        updatedAt: d.updatedAt
    }));
}

/**
 * Get a single goal by id (only if it belongs to the user).
 */
export async function getGoal(userId: string, goalId: string) {
    if (!mongoose.isValidObjectId(goalId)) return null;
    const g = await Goal.findOne({ _id: goalId, user: new Types.ObjectId(userId) }).lean().exec();
    if (!g) return null;
    return {
        id: g._id,
        title: g.title,
        platform: g.platform,
        platformUsername: g.platformUsername,
        baselineValue: g.baselineValue,
        targetValue: g.targetValue,
        status: g.status,
        deadline: g.deadline,
        checkIntervalMs: g.checkIntervalMs,
        evidence: g.evidence,
        completedAt: g.completedAt ?? null,
        createdAt: g.createdAt,
        updatedAt: g.updatedAt
    };
}

/**
 * Mark a goal as complete and record a history entry.
 * Non-transactional (works on standalone Mongo).
 */
export async function markComplete(
    userId: string,
    goalId: string,
    completedAt: Date,
    details: any,
    evidence: any = null
) {
    if (!mongoose.isValidObjectId(goalId)) throw new Error('Invalid goal id');
    const gid = new Types.ObjectId(goalId);
    const uid = new Types.ObjectId(userId);

    // Fetch and update goal (atomic per-document via save)
    const goal = await Goal.findOne({ _id: gid, user: uid }).exec();
    if (!goal) throw new Error('Goal not found');

    if (goal.status !== 'completed') {
        goal.status = 'completed';
        goal.completedAt = completedAt;
        goal.evidence = evidence ?? goal.evidence ?? null;
        await goal.save();
    } else {
        // If already completed, optionally update completedAt if provided and newer
        if (completedAt && (!goal.completedAt || completedAt > goal.completedAt)) {
            goal.completedAt = completedAt;
            goal.evidence = evidence ?? goal.evidence ?? null;
            await goal.save();
        }
    }

    // Insert history record (non-transactional)
    try {
        await GoalCheckHistory.create({
            goal: gid,
            user: uid,
            checkedAt: completedAt,
            result: 'completed',
            details,
            evidence
        });
    } catch (err) {
        // History insertion failure is non-fatal for completion. Log and continue.
        // If you have a logger, prefer logger.warn here. For now, console.warn.
        // eslint-disable-next-line no-console
        console.warn('Failed to write GoalCheckHistory:', err);
    }

    const fresh = await Goal.findById(gid).lean().exec();
    return {
        id: fresh?._id,
        status: fresh?.status,
        completedAt: fresh?.completedAt ?? null,
        evidence: fresh?.evidence ?? null,
        createdAt: fresh?.createdAt,
        updatedAt: fresh?.updatedAt
    };
}
