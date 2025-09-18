// src/routes/blockedApps.routes.ts
import { Router } from 'express';
import * as ctrl from '../controllers/blockedApps.controller.ts';
import { requireAuth } from '../middleware/auth';

const router = Router();
router.use(requireAuth);

router.get('/', ctrl.listBlockedApps);
router.post('/', ctrl.addBlockedApp);
router.delete('/:id', ctrl.removeBlockedApp);

export default router;
