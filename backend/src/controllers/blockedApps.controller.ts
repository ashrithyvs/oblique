// src/controllers/blockedApps.controller.ts
import { Request, Response } from 'express';
import BlockedApp from '../models/blockedApp.model';
import { Types } from 'mongoose';

export async function listBlockedApps(req: any, res: Response) {
    try {
        const apps = await BlockedApp.find({ user: new Types.ObjectId(req.user._id) })
            .sort({ createdAt: -1 })
            .lean()
            .exec();
        return res.json(apps);
    } catch (err: any) {
        return res.status(500).json({ message: err.message });
    }
}

export async function addBlockedApp(req: any, res: Response) {
    try {
        const { packageName, reason } = req.body;
        if (!packageName) return res.status(400).json({ message: 'packageName required' });

        const app = await BlockedApp.findOneAndUpdate(
            { user: req.user._id, packageName },
            { user: req.user._id, packageName, reason },
            { new: true, upsert: true }
        );

        return res.status(201).json(app);
    } catch (err: any) {
        return res.status(400).json({ message: err.message });
    }
}

export async function removeBlockedApp(req: any, res: Response) {
    try {
        const { id } = req.params;
        await BlockedApp.deleteOne({ _id: id, user: req.user._id });
        return res.json({ ok: true });
    } catch (err: any) {
        return res.status(500).json({ message: err.message });
    }
}
