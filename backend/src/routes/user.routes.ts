// src/routes/user.routes.ts
import { Router } from 'express';
import * as ctrl from '../controllers/user.controller';
import { requireAuth } from '../middleware/auth';

const router = Router();

// all routes here require auth
router.use(requireAuth);

router.get('/me', ctrl.getCurrentUser);

// blocked-apps endpoints (user-embedded)
router.get('/me/blocked-apps', ctrl.listBlockedApps);
router.post('/me/blocked-apps', ctrl.addBlockedApp);
router.delete('/me/blocked-apps/:pkg', ctrl.removeBlockedApp);
router.put('/me/blocked-apps', ctrl.replaceBlockedApps);

export default router;
