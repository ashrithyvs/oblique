// src/utils/validators.ts
import { z } from 'zod';

export const registerSchema = z.object({
    email: z.string().email().optional(), // optional to allow OAuth-only later
    name: z.string().min(1).optional(),
    password: z.string().min(8).optional(), // required for normal registration, optional for OAuth flows
    pin: z.string().min(4).max(12).optional()
});

export const loginSchema = z.object({
    email: z.string().email(),
    password: z.string().min(8).optional(),
    pin: z.string().min(4).max(12).optional()
});

export const createGoalSchema = z.object({
    platform: z.string().min(1).optional(),
    platformUsername: z.string().optional(),
    targetValue: z.number().int().positive(),
    deadline: z.union([z.number(), z.string()]).optional(),
    deadlineTimeOfDayMs: z.number().int().nonnegative().optional(),
    checkIntervalMs: z.number().int().positive().optional(),
    title: z.string().optional(),
    baselineValue: z.number().int().nonnegative().optional(),
    evidence: z.any().optional(),
    unit: z.string().optional(),
});

export const completeGoalSchema = z.object({
    completedAt: z.number().optional(), // epoch millis
    details: z.any().optional(),
    evidence: z.any().optional()
});

export const updateGoalProgressSchema = z.object({
    progress: z.number(),
    evidence: z.any().optional(),
});

export const setGoalBaselineSchema = z.object({
    baselineValue: z.number().int().nonnegative(),
    platformUsername: z.string().optional(),
    evidence: z.any().optional(),
});

export const updatePreferencesSchema = z.object({
    displayName: z.string().optional(),
    usernames: z.record(z.string(), z.string()).optional(),
    deadlineBufferMs: z.number().int().min(0).max(3 * 60 * 60 * 1000).optional(),
});

export const recordPeriodProgressSchema = z.object({
    periodDeadlineMs: z.number().int().positive(),
    progress: z.number().int().nonnegative(),
    evidence: z.any().optional(),
});
