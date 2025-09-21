// src/controllers/goal.controller.ts
import { Request, Response } from 'express';
import Goal from '../models/goal.model';
import { Types } from 'mongoose';
import * as goalSvc from '../services/goals.service';

/**
 * GET /api/goals
 * List goals for authenticated user.
 */
export async function listGoals(req: any, res: Response) {
    try {
        // Ensure valid user id
        const userId = new Types.ObjectId(req.user._id);

        const docs = await Goal.find({ user: userId }).sort({ createdAt: -1 }).lean().exec();

        const out = docs.map((g: any) => ({
            id: g._id,
            title: g.title,
            platform: g.platform,
            platformUsername: g.platformUsername,
            unit: g.unit,
            baselineValue: g.baselineValue,
            targetValue: g.targetValue,
            progress: g.progress,
            status: g.status,
            checkIntervalMs: g.checkIntervalMs,
            lastCheckedAt: g.lastCheckedAt,
            completedAt: g.completedAt,
            completedByDevice: g.completedByDevice,
            evidence: g.evidence,
            createdAt: g.createdAt,
            updatedAt: g.updatedAt
        }));

        return res.json(out);
    } catch (err: any) {
        console.error('listGoals error', err);
        return res.status(500).json({ message: err.message });
    }
}

/**
 * POST /api/goals
 * Body: { title, platform, platformUsername?, unit?, baselineValue?, targetValue, checkIntervalMs?, deadline?, evidence? }
 * Creates a goal tied to the authenticated user.
 */
export async function createGoal(req: any, res: Response) {
    try {
        const userId = new Types.ObjectId(req.user._id);

        // be permissive: pick fields from body but always set user
        const payload: any = {
            user: userId,
            title: req.body.title || `${req.body.platform || 'goal'} goal`,
            platform: req.body.platform || 'leetcode',
            platformUsername: req.body.platformUsername || null,
            unit: req.body.unit || req.body.unit || '',
            baselineValue: typeof req.body.baselineValue === 'number' ? req.body.baselineValue : 0,
            targetValue: typeof req.body.targetValue === 'number' ? req.body.targetValue : 0,
            progress: typeof req.body.progress === 'number' ? req.body.progress : 0,
            status: req.body.status || 'active',
            checkIntervalMs: typeof req.body.checkIntervalMs === 'number' ? req.body.checkIntervalMs : 3600000,
            evidence: req.body.evidence || null
            // deadline handled by service if needed
        };

        // Basic validation
        if (!payload.targetValue || payload.targetValue <= 0) {
            return res.status(400).json({ message: 'targetValue is required and must be > 0' });
        }

        const g = await Goal.create(payload);

        return res.status(201).json({
            id: g._id,
            title: g.title,
            platform: g.platform,
            platformUsername: g.platformUsername,
            unit: g.unit,
            baselineValue: g.baselineValue,
            targetValue: g.targetValue,
            progress: g.progress,
            status: g.status,
            checkIntervalMs: g.checkIntervalMs,
            lastCheckedAt: g.lastCheckedAt,
            completedAt: g.completedAt,
            evidence: g.evidence,
            createdAt: g.createdAt,
            updatedAt: g.updatedAt
        });
    } catch (err: any) {
        console.error('createGoal error', err);
        return res.status(400).json({ message: err.message });
    }
}

/**
 * DELETE /api/goals/:id
 */
export async function deleteGoal(req: any, res: Response) {
    try {
        const userId = new Types.ObjectId(req.user._id);
        const id = req.params.id;
        if (!Types.ObjectId.isValid(id)) return res.status(400).json({ message: 'Invalid id' });

        await Goal.deleteOne({ _id: id, user: userId }).exec();
        return res.json({ ok: true });
    } catch (err: any) {
        console.error('deleteGoal error', err);
        return res.status(500).json({ message: err.message });
    }
}

export async function completeGoal(req: any, res: Response) {
    try {
        const userId = req.user._id;
        const id = req.params.id;
        if (!Types.ObjectId.isValid(id)) return res.status(400).json({ message: 'Invalid id' });

        const now = new Date();
        const result = await goalSvc.markComplete(
            userId,
            id,
            now,
            { via: 'manual', completedByDevice: !!req.body.completedByDevice },
            req.body.evidence || null
        );

        if (!result) return res.status(404).json({ message: 'Goal not found' });
        return res.json(result);
    } catch (err: any) {
        console.error('completeGoal error', err);
        return res.status(500).json({ message: err.message });
    }
}



export async function getGoal(req: any, res: Response) {
    try {
        const userId = new Types.ObjectId(req.user._id);
        const id = req.params.id;
        if (!Types.ObjectId.isValid(id)) return res.status(400).json({ message: 'Invalid id' });

        const goal = await Goal.findOne({ _id: id, user: userId }).lean().exec();
        if (!goal) return res.status(404).json({ message: 'Goal not found' });

        return res.json({
            id: goal._id,
            title: goal.title,
            platform: goal.platform,
            platformUsername: goal.platformUsername,
            unit: goal.unit,
            baselineValue: goal.baselineValue,
            targetValue: goal.targetValue,
            status: goal.status,
            createdAt: goal.createdAt,
            updatedAt: goal.updatedAt
        });
    } catch (err: any) {
        console.error('getGoal error', err);
        return res.status(500).json({ message: err.message });
    }
}


export async function updateGoalProgress(req: any, res: Response) {
    try {
        const userId = req.user._id;
        const id = req.params.id;
        const { currentValue } = req.body;

        if (!currentValue || typeof currentValue !== 'number') {
            return res.status(400).json({ message: 'Missing or invalid currentValue' });
        }

        const updated = await goalSvc.updateGoalProgress(id, currentValue, userId);
        if (!updated) return res.status(404).json({ message: 'Goal not found' });

        return res.json(updated);
    } catch (err: any) {
        console.error('updateGoalProgress error', err);
        return res.status(500).json({ message: err.message });
    }
}


