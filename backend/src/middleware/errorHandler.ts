// src/middleware/errorHandler.ts
import { Request, Response, NextFunction } from 'express';
import { logger } from '../utils/logger';

export function errorHandler(err: any, req: Request, res: Response, next: NextFunction) {
    logger.error('Unhandled error', err);
    const code = err?.status || 500;
    const message = err?.message || 'Internal server error';
    res.status(code).json({ message });
}
