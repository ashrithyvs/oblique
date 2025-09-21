// src/controllers/dashboard.controller.ts
import { Request, Response } from 'express';
import Goal from '../models/goal.model';
import User from '../models/user.model';
import { Types } from 'mongoose';

export async function getDashboard(req: any, res: Response) {
    try {
        const userId = new Types.ObjectId(req.user._id);

        // fetch goals tied to this user
        const goals = await Goal.find({ user: userId }).sort({ createdAt: -1 }).lean().exec();

        // fetch blocked apps from user doc (embedded)
        const user = await User.findById(userId).select('blockedApps').lean().exec();
        const blockedAppsArr: string[] = (user && (user as any).blockedApps) || [];

        return res.json({
            goals: goals.map((g: any) => ({
                id: g._id,
                title: g.title,
                platform: g.platform,
                platformUsername: g.platformUsername,
                unit: g.unit,
                baselineValue: g.baselineValue,
                targetValue: g.targetValue,
                progress: g.progress,
                status: g.status,
                lastCheckedAt: g.lastCheckedAt,
                completedAt: g.completedAt,
                evidence: g.evidence,
                createdAt: g.createdAt
            })),
            blockedApps: blockedAppsArr.map(pkg => ({ packageName: pkg }))
        });
    } catch (err: any) {
        console.error('getDashboard error', err);
        return res.status(500).json({ message: err.message });
    }
}
