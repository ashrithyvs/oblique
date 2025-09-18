// src/routes/goals.routes.ts
import { Router } from 'express';
import * as ctrl from '../controllers/goals.controller';
import rateLimit from 'express-rate-limit';
import { GOAL_COMPLETE_RATE_LIMIT_MAX, GOAL_COMPLETE_RATE_LIMIT_WINDOW_MS } from '../config';

const router = Router();

// Rate limit only the complete endpoint
const completeLimiter = rateLimit({
    windowMs: GOAL_COMPLETE_RATE_LIMIT_WINDOW_MS,
    max: GOAL_COMPLETE_RATE_LIMIT_MAX,
    message: { message: 'Too many goal complete requests, slow down.' }
});

router.get('/', ctrl.listGoalsHandler);
router.post('/', ctrl.createGoalHandler);
router.get('/:id', ctrl.getGoalHandler);
router.post('/:id/complete', completeLimiter, ctrl.completeGoalHandler);
router.delete('/:id', ctrl.removeGoalHandler);

export default router;
