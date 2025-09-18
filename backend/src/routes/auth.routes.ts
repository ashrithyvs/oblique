// src/routes/auth.routes.ts
import { Router } from 'express';
import * as ctrl from '../controllers/auth.controller.ts';

const router = Router();

router.post('/register', ctrl.register);
router.post('/login', ctrl.loginByEmail);

export default router;
