// src/routes/dashboard.routes.ts
import { Router } from 'express';
import * as ctrl from '../controllers/dashboard.controller';

const router = Router();
router.get('/', ctrl.getDashboard);

export default router;
