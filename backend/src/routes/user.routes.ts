// src/routes/user.routes.ts
import { Router } from 'express';
import * as ctrl from '../controllers/user.controller';

const router = Router();

router.get('/me', ctrl.getCurrentUser);
router.get('/me/blocked-apps', ctrl.listBlockedApps);
router.post('/me/blocked-apps', ctrl.addBlockedApp);
router.delete('/me/blocked-apps/:pkg', ctrl.removeBlockedApp);
router.put('/me/blocked-apps', ctrl.replaceBlockedApps);
router.put('/me/preferences', ctrl.updatePreferences);

export default router;
