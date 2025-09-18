// src/routes/dashboard.routes.ts
import { Router } from 'express';
import { requireAuth } from '../middleware/auth';
import * as ctrl from '../controllers/dashboard.controller';

const router = Router();
router.use(requireAuth);

router.get('/', ctrl.getDashboard);

export default router;
