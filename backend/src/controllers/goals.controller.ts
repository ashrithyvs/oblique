// src/controllers/goals.controller.ts
import { Response } from 'express';
import { Types } from 'mongoose';
import * as goalSvc from '../services/goals.service';
import { completeGoalSchema, updateGoalProgressSchema } from '../utils/validators';
import { GoalServiceError } from '../services/goals.service';

function mapServiceError(err: unknown, res: Response, context: string): Response | null {
    if (err instanceof GoalServiceError) {
        const status =
            err.code === 'NOT_FOUND' ? 404 :
            err.code === 'GOAL_COMPLETED' || err.code === 'PROGRESS_REGRESSION' || err.code === 'NO_VALID_FIELDS' || err.code === 'INVALID_ID'
                ? 400 : 500;
        return res.status(status).json({ message: err.message, code: err.code });
    }
    console.error(`${context} error`, err);
    return null;
}

export async function listGoals(req: any, res: Response) {
    try {
        const goals = await goalSvc.listGoalsForUser(req.user._id);
        return res.json(goals);
    } catch (err: any) {
        console.error('listGoals error', err);
        return res.status(500).json({ message: err.message });
    }
}

export async function createGoal(req: any, res: Response) {
    try {
        const goal = await goalSvc.createGoal(req.user._id, req.body);
        return res.status(201).json(goal);
    } catch (err: any) {
        console.error('createGoal error', err);
        return res.status(400).json({ message: err.message });
    }
}

export async function deleteGoal(req: any, res: Response) {
    try {
        const id = req.params.id;
        if (!Types.ObjectId.isValid(id)) return res.status(400).json({ message: 'Invalid id' });
        const deleted = await goalSvc.deleteGoalForUser(req.user._id, id);
        if (!deleted) return res.status(404).json({ message: 'Goal not found' });
        return res.json({ ok: true });
    } catch (err: any) {
        console.error('deleteGoal error', err);
        return res.status(500).json({ message: err.message });
    }
}

export async function completeGoal(req: any, res: Response) {
    try {
        const id = req.params.id;
        if (!Types.ObjectId.isValid(id)) return res.status(400).json({ message: 'Invalid id' });

        const parsed = completeGoalSchema.parse(req.body ?? {});
        const completedAt = parsed.completedAt ? new Date(parsed.completedAt) : new Date();

        const result = await goalSvc.markComplete(
            req.user._id,
            id,
            completedAt,
            parsed.details ?? { via: 'manual', completedByDevice: !!req.body?.completedByDevice },
            parsed.evidence ?? null,
        );
        return res.json(result);
    } catch (err: any) {
        const mapped = mapServiceError(err, res, 'completeGoal');
        if (mapped) return mapped;
        return res.status(500).json({ message: err.message });
    }
}

export async function getGoal(req: any, res: Response) {
    try {
        const id = req.params.id;
        if (!Types.ObjectId.isValid(id)) return res.status(400).json({ message: 'Invalid id' });
        const goal = await goalSvc.getGoalForUser(req.user._id, id);
        if (!goal) return res.status(404).json({ message: 'Goal not found' });
        return res.json(goal);
    } catch (err: any) {
        console.error('getGoal error', err);
        return res.status(500).json({ message: err.message });
    }
}

export async function updateGoalProgress(req: any, res: Response) {
    try {
        const id = req.params.id;
        const parsed = updateGoalProgressSchema.parse(req.body ?? {});

        const updated = await goalSvc.updateGoalProgress(
            id,
            parsed.progress,
            req.user._id,
            parsed.evidence,
        );
        if (!updated) return res.status(404).json({ message: 'Goal not found' });
        return res.json(updated);
    } catch (err: any) {
        const mapped = mapServiceError(err, res, 'updateGoalProgress');
        if (mapped) return mapped;
        if (err?.name === 'ZodError') {
            return res.status(400).json({ message: 'Missing or invalid progress' });
        }
        return res.status(500).json({ message: err.message });
    }
}

export async function updateGoal(req: any, res: Response) {
    try {
        const id = req.params.id;
        if (!Types.ObjectId.isValid(id)) {
            return res.status(400).json({ message: 'Invalid goal id' });
        }
        const updatedGoal = await goalSvc.updateGoalForUser(req.user._id, id, req.body);
        if (!updatedGoal) return res.status(404).json({ message: 'Goal not found' });
        return res.json(updatedGoal);
    } catch (err: any) {
        const mapped = mapServiceError(err, res, 'updateGoal');
        if (mapped) return mapped;
        return res.status(500).json({ message: err.message });
    }
}
