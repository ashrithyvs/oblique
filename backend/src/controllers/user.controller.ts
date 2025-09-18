// src/controllers/user.controller.ts
import { Request, Response } from 'express';
import * as userSvc from '../services/user.service';
import User from '../models/user.model';

export const uploadIcon = (req: any, res: Response) => {
    if (!req.file?.buffer) return res.status(400).json({ message: 'Missing file' });
    userSvc.uploadUserIcon(req.user._id, req.file.buffer);
    return res.json({ ok: true });
};

export async function getUserIconHandler(req: any, res: Response) {
    const id = req.params.id;
    const u = await User.findById(id).select('icon').exec();
    if (!u?.icon) return res.status(404).send();
    res.setHeader('Content-Type', 'image/png');
    return res.send(u.icon);
}

export async function updateBlockedAppsHandler(req: any, res: Response) {
    try {
        const apps: string[] = Array.isArray(req.body.blockedApps) ? req.body.blockedApps : [];
        const sanitized = apps.filter(a => typeof a === 'string' && a.trim().length > 0);
        await userSvc.updateBlockedApps(req.user._id, sanitized);
        return res.json({ ok: true, blockedApps: sanitized });
    } catch (err: any) {
        console.error('updateBlockedAppsHandler', err);
        return res.status(400).json({ message: err?.message || 'Invalid payload' });
    }
}
