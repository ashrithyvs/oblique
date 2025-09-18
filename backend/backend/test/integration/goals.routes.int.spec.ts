/**
 * Integration test for goals routes:
 * - mounts the real router, but mocks auth middleware + services so tests are isolated
 */

import express from 'express';
import request from 'supertest';
import bodyParser from 'body-parser';
import { jest } from '@jest/globals';

// Make sure requireAuth middleware resolves immediately
jest.mock('../../../src/middleware/auth.ts', () => ({
    __esModule: true,
    requireAuth: (req: any, _res: any, next: any) => {
        // attach a fake user to simulate authenticated request
        req.user = { _id: 'user-1' };
        return next();
    },
}));

// Mock the service layer to avoid DB dependency
jest.mock('../../../src/services/goals.service.ts', () => ({
    __esModule: true,
    createGoal: jest.fn().mockResolvedValue({
        _id: 'g1',
        platform: 'LeetCode',
        unit: 'lessons',
        targetValue: 2,
        progress: 0,
    }),
    listGoals: jest.fn().mockResolvedValue([{ _id: 'g1', platform: 'LeetCode' }]),
    dashboardSummary: jest.fn().mockResolvedValue({ totalGoals: 1, completedCount: 0, totalTarget: 10, totalProgress: 5, successRate: 0.5 }),
}));

import goalsRouter from '../../../src/routes/goals.routes.ts'; // route module path as in your project

describe('goals routes (integration)', () => {
    let app: express.Express;

    beforeAll(() => {
        app = express();
        app.use(bodyParser.json());
        // mount the router under /api/goals like in your app
        app.use('/api/goals', goalsRouter);
    });

    it('GET /api/goals should return list', async () => {
        const res = await request(app).get('/api/goals');
        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('data');
        expect(Array.isArray(res.body.data)).toBe(true);
    });

    it('POST /api/goals should create goal', async () => {
        const payload = { platform: 'LeetCode', unit: 'lessons', targetValue: 2 };
        const res = await request(app).post('/api/goals').send(payload);
        expect(res.status).toBe(201);
        expect(res.body).toHaveProperty('id', 'g1');
    });

    it('GET /api/goals/dashboard should return summary', async () => {
        const res = await request(app).get('/api/goals/dashboard');
        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('totalGoals');
    });
});
