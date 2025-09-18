// src/controllers/dashboard.controller.ts
import { Request, Response } from 'express';
import Goal from '../models/goal.model';
import BlockedApp from '../models/blockedApp.model';
import { Types } from 'mongoose';

export async function getDashboard(req: any, res: Response) {
    try {
        const userId = new Types.ObjectId(req.user._id);

        const goals = await Goal.find({ user: userId })
            .sort({ createdAt: -1 })
            .lean()
            .exec();

        const blockedApps = await BlockedApp.find({ user: userId })
            .sort({ createdAt: -1 })
            .lean()
            .exec();

        return res.json({
            goals: goals.map(g => ({
                id: g._id,
                title: g.title,
                platform: g.platform,
                platformUsername: g.platformUsername,
                targetValue: g.targetValue,
                baselineValue: g.baselineValue,
                status: g.status,
                deadline: g.deadline,
                completedAt: g.completedAt,
                createdAt: g.createdAt
            })),
            blockedApps: blockedApps.map(b => ({
                id: b._id,
                packageName: b.packageName,
                reason: b.reason,
                createdAt: b.createdAt
            }))
        });
    } catch (err: any) {
        return res.status(500).json({ message: err.message });
    }
}
