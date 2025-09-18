// src/routes/user.routes.ts
import { Router } from 'express';
import * as ctrl from '../controllers/user.controller';
import { requireAuth } from '../middleware/auth';
import multer from 'multer';

const router = Router();
const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 500 * 1024 } });

router.use(requireAuth);

router.post('/icon', upload.single('icon'), ctrl.uploadIcon);
router.get('/icon/:id', ctrl.getUserIconHandler);

// update blocked apps (body: { blockedApps: ["com.foo", "com.bar"] })
router.put('/blocked-apps', ctrl.updateBlockedAppsHandler);

export default router;
