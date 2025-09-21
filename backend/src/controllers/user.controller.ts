// src/controllers/user.controller.ts
import { Request, Response } from 'express';
import * as userSvc from '../services/user.service';
import Goal from '../models/goal.model';


/**
 * GET /api/users/me
 * Returns the current user with:
 *  - id, email, name
 *  - hasPin (boolean)
 *  - blockedApps (string[])
 *  - goals (list of goals)
 */
export async function getCurrentUser(req: any, res: Response) {
    try {
        const userDoc = await userSvc.getUserDocumentById(req.user._id);
        if (!userDoc) {
            return res.status(404).json({ message: 'User not found' });
        }

        // ⚡ Build DTO for client
        const goals = await Goal.find({ user: req.user._id })
            .select('_id platform targetValue unit progress')
            .lean()
            .exec();

        const out = {
            id: userDoc._id,
            email: userDoc.email,
            name: userDoc.name,
            hasPin: !!userDoc.hashedPin,
            blockedApps: userDoc.blockedApps || [],
            goals: goals.map(g => ({
                id: g._id,
                platform: g.platform,
                targetValue: g.targetValue,
                unit: g.unit,
                progress: g.progress ?? 0,
            })),
        };

        return res.json(out);
    } catch (err: any) {
        console.error('getCurrentUser error', err);
        return res.status(500).json({ message: err.message });
    }
}
/**
 * GET /api/users/me/blocked-apps
 * Returns an array of { packageName } objects for compatibility with older clients.
 */
export async function listBlockedApps(req: any, res: Response) {
    try {
        const pkgs = await userSvc.getBlockedApps(req.user._id);
        const out = pkgs.map(p => ({ packageName: p }));
        return res.json(out);
    } catch (err: any) {
        console.error('listBlockedApps error', err);
        return res.status(500).json({ message: err.message });
    }
}

/**
 * POST /api/users/me/blocked-apps
 * body: { packageName: string, reason?: string }  (reason is ignored in user-embedded model)
 * Returns updated list as array of { packageName }.
 */
export async function addBlockedApp(req: any, res: Response) {
    try {
        const pkg = (req.body && (req.body.packageName || req.body.pkg));
        if (!pkg || typeof pkg !== 'string') {
            return res.status(400).json({ message: 'Missing packageName' });
        }

        await userSvc.addBlockedApp(req.user._id, pkg);
        const pkgs = await userSvc.getBlockedApps(req.user._id);
        return res.json(pkgs);
    } catch (err: any) {
        console.error('addBlockedApp error', err);
        return res.status(500).json({ message: err.message });
    }
}

/**
 * DELETE /api/users/me/blocked-apps/:pkg
 * Removes the package name and returns { ok: true }.
 */
export async function removeBlockedApp(req: any, res: Response) {
    try {
        const pkg = req.params.pkg;
        if (!pkg || typeof pkg !== 'string') {
            return res.status(400).json({ message: 'Missing packageName' });
        }
        await userSvc.removeBlockedApp(req.user._id, pkg);
        return res.json({ ok: true });
    } catch (err: any) {
        console.error('removeBlockedApp error', err);
        return res.status(500).json({ message: err.message });
    }
}

/**
 * PUT /api/users/me/blocked-apps
 * body: { blockedApps: string[] } - replace entire list
 */
export async function replaceBlockedApps(req: any, res: Response) {
    try {
        const list = Array.isArray(req.body.blockedApps) ? req.body.blockedApps : [];
        await userSvc.updateBlockedApps(req.user._id, list);
        const pkgs = await userSvc.getBlockedApps(req.user._id);
        return res.json(pkgs.map(p => ({ packageName: p })));
    } catch (err: any) {
        console.error('replaceBlockedApps error', err);
        return res.status(500).json({ message: err.message });
    }
}
