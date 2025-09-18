// src/controllers/goals.controller.ts
import { Request, Response } from 'express';
import Goal from '../models/goal.model';
import { createGoalSchema, completeGoalSchema } from '../utils/validators';
import { Types } from 'mongoose';

export async function listGoalsHandler(req: any, res: Response) {
    try {
        const goals = await Goal.find({ user: req.user._id })
            .sort({ createdAt: -1 })
            .lean()
            .exec();
        return res.json(goals);
    } catch (err: any) {
        return res.status(500).json({ message: err.message });
    }
}

export async function createGoalHandler(req: any, res: Response) {
    try {
        const parsed = createGoalSchema.parse(req.body);

        const goal = await Goal.create({
            user: req.user._id,
            title: parsed.title,
            platform: parsed.platform || 'leetcode',
            platformUsername: parsed.platformUsername || 'unknown',
            targetValue: parsed.targetValue,
            baselineValue: parsed.baselineValue ?? 0,
            deadline: parsed.deadline ? new Date(parsed.deadline) : undefined,
            checkIntervalMs: parsed.checkIntervalMs ?? 3600000,
            evidence: parsed.evidence
        });

        return res.status(201).json({ id: goal._id, ...goal.toObject() });
    } catch (err: any) {
        return res.status(400).json({ message: err.message });
    }
}

export async function getGoalHandler(req: any, res: Response) {
    try {
        const { id } = req.params;
        if (!Types.ObjectId.isValid(id)) return res.status(400).json({ message: 'Invalid id' });

        const goal = await Goal.findOne({ _id: id, user: req.user._id }).lean().exec();
        if (!goal) return res.status(404).json({ message: 'Not found' });

        return res.json(goal);
    } catch (err: any) {
        return res.status(500).json({ message: err.message });
    }
}

export async function completeGoalHandler(req: any, res: Response) {
    try {
        const { id } = req.params;
        if (!Types.ObjectId.isValid(id)) return res.status(400).json({ message: 'Invalid id' });

        const parsed = completeGoalSchema.parse(req.body);

        const update = {
            status: 'completed' as const,
            completedAt: parsed.completedAt ? new Date(parsed.completedAt) : new Date(),
            evidence: parsed.evidence ?? {},
        };

        const goal = await Goal.findOneAndUpdate(
            { _id: id, user: req.user._id },
            { $set: update },
            { new: true }
        ).exec();

        if (!goal) return res.status(404).json({ message: 'Not found' });

        return res.json(goal);
    } catch (err: any) {
        return res.status(400).json({ message: err.message });
    }
}

export async function removeGoalHandler(req: any, res: Response) {
    try {
        const { id } = req.params;
        await Goal.deleteOne({ _id: id, user: req.user._id });
        return res.json({ ok: true });
    } catch (err: any) {
        return res.status(500).json({ message: err.message });
    }
}
