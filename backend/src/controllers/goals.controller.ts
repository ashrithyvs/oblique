// src/controllers/goals.controller.ts
import { Response } from 'express';
import { Types } from 'mongoose';
import * as goalSvc from '../services/goals.service';

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

        const result = await goalSvc.markComplete(
            req.user._id,
            id,
            new Date(),
            { via: 'manual', completedByDevice: !!req.body.completedByDevice },
            req.body.evidence || null
        );
        return res.json(result);
    } catch (err: any) {
        console.error('completeGoal error', err);
        if (err.message === 'Goal not found') return res.status(404).json({ message: err.message });
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
        const { progress } = req.body;
        if (progress === undefined || typeof progress !== 'number') {
            return res.status(400).json({ message: 'Missing or invalid progress' });
        }
        const updated = await goalSvc.updateGoalProgress(id, progress, req.user._id);
        if (!updated) return res.status(404).json({ message: 'Goal not found' });
        return res.json(updated);
    } catch (err: any) {
        console.error('updateGoalProgress error', err);
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
        console.error('updateGoal error', err);
        const status = err.message === 'No valid fields to update' ? 400 : 500;
        return res.status(status).json({ message: err.message });
    }
}
