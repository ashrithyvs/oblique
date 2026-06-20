// src/controllers/dashboard.controller.ts
import { Response } from 'express';
import * as goalSvc from '../services/goals.service';
import * as userSvc from '../services/user.service';
import { toBlockedAppDtos } from '../utils/goalDto';

export async function getDashboard(req: any, res: Response) {
    try {
        const userId = req.user._id;
        const goals = await goalSvc.listGoalsForUser(userId);
        const blockedApps = await userSvc.getBlockedApps(userId);

        return res.json({
            goals,
            blockedApps: toBlockedAppDtos(blockedApps),
        });
    } catch (err: any) {
        console.error('getDashboard error', err);
        return res.status(500).json({ message: err.message });
    }
}
