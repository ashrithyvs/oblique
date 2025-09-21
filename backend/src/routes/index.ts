// src/routes/index.ts
import express from 'express';
import authRoutes from './auth.routes';
import goalsRoutes from './goals.routes';
import userRoutes from './user.routes';
import dashboardRoutes from './dashboard.routes';
import { requireAuth } from '../middleware/auth';

const router = express.Router();

router.get('/health', (req, res) => res.json({ ok: true }));

router.use('/auth', authRoutes);
router.use('/user', requireAuth, userRoutes);
router.use('/goals', requireAuth, goalsRoutes);
router.use('/dashboard', requireAuth, dashboardRoutes);

export default router;
