// src/routes/goals.routes.ts
import { Router } from 'express';
import * as ctrl from '../controllers/goals.controller';
import rateLimit from 'express-rate-limit';
import { GOAL_COMPLETE_RATE_LIMIT_MAX, GOAL_COMPLETE_RATE_LIMIT_WINDOW_MS, GOAL_PROGRESS_RATE_LIMIT_MAX, GOAL_PROGRESS_RATE_LIMIT_WINDOW_MS } from '../config';

const router = Router();

// Rate limit only the complete endpoint
const completeLimiter = rateLimit({
    windowMs: GOAL_COMPLETE_RATE_LIMIT_WINDOW_MS,
    max: GOAL_COMPLETE_RATE_LIMIT_MAX,
    message: { message: 'Too many goal complete requests, slow down.' }
});

const progressLimiter = rateLimit({
    windowMs: GOAL_PROGRESS_RATE_LIMIT_WINDOW_MS,
    max: GOAL_PROGRESS_RATE_LIMIT_MAX,
    message: { message: 'Too many progress update requests, slow down.' }
});

router.get('/', ctrl.listGoals);
router.post('/', ctrl.createGoal);
router.get('/:id', ctrl.getGoal);

router.post('/:id/complete', completeLimiter, ctrl.completeGoal);

router.delete('/:id', ctrl.deleteGoal);

router.patch('/:id/progress', progressLimiter, ctrl.updateGoalProgress);

router.put('/:id', ctrl.updateGoal); // ✅ NEW ROUTE

export default router;
