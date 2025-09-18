// src/middleware/auth.ts
import { Request, Response, NextFunction } from 'express';
import { verifyJwt } from '../utils/jwt';
import { logger } from '../utils/logger';

/**
 * AuthedRequest attaches user info onto request
 */
export interface AuthedRequest extends Request {
    user?: { _id: string, [k: string]: any };
}

export async function requireAuth(req: AuthedRequest, res: Response, next: NextFunction) {
    try {
        const auth = (req.header('Authorization') || '').trim();
        const token = auth.startsWith('Bearer ') ? auth.slice(7) : auth;
        if (!token) {
            return res.status(401).json({ message: 'Unauthorized: missing token' });
        }
        try {
            const payload = verifyJwt<any>(token);
            if (!payload || !payload.sub) {
                return res.status(401).json({ message: 'Unauthorized: invalid token' });
            }
            // attach minimal user object
            req.user = { _id: String(payload.sub), ...payload };
            return next();
        } catch (err: any) {
            logger.warn('JWT verification failed', (err && err.message) || err);
            return res.status(401).json({ message: 'Unauthorized: invalid token' });
        }
    } catch (err: any) {
        logger.error('requireAuth middleware error', err);
        return res.status(500).json({ message: 'Internal error' });
    }
}
