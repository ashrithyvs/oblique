// src/services/goals.service.ts
import Goal from '../models/goal.model';
import GoalCheckHistory from '../models/goalCheckHistory.model';
import mongoose, { Types } from 'mongoose';
import { toGoalDto } from '../utils/goalDto';
import { createGoalSchema } from '../utils/validators';

export type GoalDtoResponse = ReturnType<typeof toGoalDto>;

export class GoalServiceError extends Error {
    constructor(
        message: string,
        public readonly code: string = 'GOAL_SERVICE_ERROR',
    ) {
        super(message);
        this.name = 'GoalServiceError';
    }
}

export async function createGoal(userId: string, body: unknown): Promise<GoalDtoResponse> {
    const parsed = createGoalSchema.parse(body);
    const doc = await Goal.create({
        user: new Types.ObjectId(userId),
        title: parsed.title || `${parsed.platform || 'goal'} goal`,
        platform: parsed.platform ?? 'leetcode',
        platformUsername: parsed.platformUsername ?? null,
        unit: (body as any).unit ?? '',
        baselineValue: parsed.baselineValue ?? 0,
        targetValue: parsed.targetValue,
        progress: 0,
        status: 'active',
        deadline: (body as any).deadline ?? null,
        checkIntervalMs: parsed.checkIntervalMs ?? 3600000,
        evidence: parsed.evidence ?? null,
    });
    return toGoalDto(doc.toObject());
}

export async function listGoalsForUser(userId: string): Promise<GoalDtoResponse[]> {
    const docs = await Goal.find({ user: new Types.ObjectId(userId) })
        .sort({ createdAt: -1 })
        .lean()
        .exec();
    return docs.map(toGoalDto);
}

export async function getGoalForUser(userId: string, goalId: string): Promise<GoalDtoResponse | null> {
    if (!mongoose.isValidObjectId(goalId)) return null;
    const g = await Goal.findOne({ _id: goalId, user: new Types.ObjectId(userId) }).lean().exec();
    return g ? toGoalDto(g) : null;
}

export async function deleteGoalForUser(userId: string, goalId: string): Promise<boolean> {
    if (!mongoose.isValidObjectId(goalId)) return false;
    const result = await Goal.deleteOne({ _id: goalId, user: new Types.ObjectId(userId) }).exec();
    return result.deletedCount > 0;
}

export async function updateGoalForUser(
    userId: string,
    goalId: string,
    body: Record<string, unknown>,
): Promise<GoalDtoResponse | null> {
    if (!mongoose.isValidObjectId(goalId)) return null;

    const existing = await Goal.findOne({ _id: goalId, user: new Types.ObjectId(userId) }).exec();
    if (!existing) return null;
    if (existing.status === 'completed') {
        throw new GoalServiceError('Cannot update completed goal', 'GOAL_COMPLETED');
    }

    const allowedFields = ['title', 'targetValue', 'deadline', 'unit', 'checkIntervalMs'];
    const updatePayload: Record<string, unknown> = {};
    for (const key of allowedFields) {
        if (body[key] !== undefined) updatePayload[key] = body[key];
    }
    if (Object.keys(updatePayload).length === 0) {
        throw new GoalServiceError('No valid fields to update', 'NO_VALID_FIELDS');
    }
    updatePayload.updatedAt = new Date();

    const updated = await Goal.findOneAndUpdate(
        { _id: goalId, user: new Types.ObjectId(userId), status: { $ne: 'completed' } },
        { $set: updatePayload },
        { new: true },
    ).lean();
    return updated ? toGoalDto(updated) : null;
}

export async function markComplete(
    userId: string,
    goalId: string,
    completedAt: Date,
    details: Record<string, unknown>,
    evidence: unknown = null,
): Promise<GoalDtoResponse> {
    if (!mongoose.isValidObjectId(goalId)) throw new GoalServiceError('Invalid goal id', 'INVALID_ID');
    const gid = new Types.ObjectId(goalId);
    const uid = new Types.ObjectId(userId);

    const goal = await Goal.findOne({ _id: gid, user: uid }).exec();
    if (!goal) throw new GoalServiceError('Goal not found', 'NOT_FOUND');

    const wasAlreadyCompleted = goal.status === 'completed';

    if (!wasAlreadyCompleted) {
        goal.status = 'completed';
        goal.completedAt = completedAt;
        goal.evidence = (evidence ?? goal.evidence ?? null) as any;
        goal.progress = Math.max(goal.progress, goal.targetValue);
        goal.lastCheckedAt = completedAt;
        await goal.save();

        try {
            await GoalCheckHistory.create({
                goal: gid,
                user: uid,
                checkedAt: completedAt,
                result: 'completed',
                details,
                evidence,
            });
        } catch (err) {
            console.warn('Failed to write GoalCheckHistory:', err);
        }
    } else if (evidence != null) {
        goal.evidence = evidence as any;
        await goal.save();
    }

    return toGoalDto(goal.toObject());
}

export async function updateGoalProgress(
    goalId: string,
    currentValue: number,
    userId: string,
    evidence?: unknown,
): Promise<GoalDtoResponse | null> {
    const goal = await Goal.findOne({ _id: goalId, user: new Types.ObjectId(userId) }).exec();
    if (!goal) return null;

    if (goal.status === 'completed') {
        throw new GoalServiceError('Cannot update progress on completed goal', 'GOAL_COMPLETED');
    }

    const computedProgress = Math.max(0, currentValue - goal.baselineValue);

    if (computedProgress < goal.progress) {
        throw new GoalServiceError('Progress regression not allowed', 'PROGRESS_REGRESSION');
    }

    goal.progress = computedProgress;
    goal.lastCheckedAt = new Date();
    if (evidence !== undefined) {
        goal.evidence = evidence as any;
    }

    if (computedProgress >= goal.targetValue) {
        await goal.save();
        return markComplete(userId, goalId, new Date(), { via: 'progressUpdate' }, evidence ?? null);
    }

    await goal.save();
    return toGoalDto(goal.toObject());
}
