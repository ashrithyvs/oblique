/**
 * Unit tests for controllers/goals.controller.ts
 * - Mocks the service layer to focus on controller logic
 */

import { jest } from '@jest/globals';
import * as httpMocks from 'node-mocks-http';

jest.mock('../../../src/services/goals.service.ts', () => ({
    __esModule: true,
    createGoal: jest.fn(),
    listGoals: jest.fn(),
    updateGoal: jest.fn(),
    deleteGoal: jest.fn(),
    incrementProgress: jest.fn(),
    dashboardSummary: jest.fn(),
}));

import * as goalsController from '../../../src/controllers/goals.controller.ts';
import * as goalsService from '../../../src/services/goals.service.ts';

describe('goals.controller', () => {
    beforeEach(() => jest.clearAllMocks());

    it('createGoalHandler -> success returns 201 + created item', async () => {
        const createdDoc: any = { _id: 'g1', platform: 'LeetCode', unit: 'lessons', targetValue: 2, progress: 0 };
        (goalsService.createGoal as jest.MockedFunction<typeof goalsService.createGoal>).mockResolvedValue(createdDoc);

        const req: any = httpMocks.createRequest({
            method: 'POST',
            url: '/api/goals',
            body: {
                platform: 'LeetCode',
                unit: 'lessons',
                targetValue: 2,
            },
            user: { _id: 'user-1' },
        });
        const res: any = httpMocks.createResponse();
        await goalsController.createGoalHandler(req, res);

        expect(res.statusCode).toBe(201);
        const json = res._getJSONData();
        expect(json).toMatchObject({
            id: 'g1',
            platform: 'LeetCode',
            unit: 'lessons',
            targetValue: 2,
        });
        expect(goalsService.createGoal).toHaveBeenCalled();
    });

    it('listGoalsHandler -> returns list', async () => {
        (goalsService.listGoals as jest.MockedFunction<typeof goalsService.listGoals>).mockResolvedValue([{ _id: 'g1' }] as any);
        const req: any = httpMocks.createRequest({ method: 'GET', url: '/api/goals', user: { _id: 'user-1' } });
        const res: any = httpMocks.createResponse();
        await goalsController.listGoalsHandler(req, res);
        expect(res.statusCode).toBe(200);
        const json = res._getJSONData();
        expect(json).toHaveProperty('data');
        expect(Array.isArray(json.data)).toBe(true);
    });

    it('incrementProgressHandler -> invokes service and returns ok', async () => {
        (goalsService.incrementProgress as jest.MockedFunction<typeof goalsService.incrementProgress>).mockResolvedValue({ _id: 'g1', progress: 1 } as any);
        const req: any = httpMocks.createRequest({
            method: 'POST',
            url: '/api/goals/g1/verify',
            params: { id: 'g1' },
            user: { _id: 'user-1' },
        });
        const res: any = httpMocks.createResponse();
        await goalsController.incrementProgressHandler(req, res);
        expect(res.statusCode).toBe(200);
        expect(goalsService.incrementProgress).toHaveBeenCalledWith('user-1', 'g1', 1);
    });

    it('deleteGoalHandler -> calls service and returns 204', async () => {
        (goalsService.deleteGoal as jest.MockedFunction<typeof goalsService.deleteGoal>).mockResolvedValue(true as any);
        const req: any = httpMocks.createRequest({
            method: 'DELETE',
            url: '/api/goals/g1',
            params: { id: 'g1' },
            user: { _id: 'user-1' },
        });
        const res: any = httpMocks.createResponse();
        await goalsController.deleteGoalHandler(req, res);
        expect(res.statusCode).toBe(200);
        expect(goalsService.deleteGoal).toHaveBeenCalledWith('user-1', 'g1');
    });

    it('dashboardHandler -> returns summary', async () => {
        (goalsService.dashboardSummary as jest.MockedFunction<typeof goalsService.dashboardSummary>).mockResolvedValue({ totalGoals: 3, completedCount: 1, totalTarget: 10, totalProgress: 5, successRate: 0.33 });
        const req: any = httpMocks.createRequest({ method: 'GET', url: '/api/goals/dashboard', user: { _id: 'user-1' } });
        const res: any = httpMocks.createResponse();
        await goalsController.dashboardHandler(req, res);
        expect(res.statusCode).toBe(200);
        expect(res._getJSONData()).toMatchObject({ totalGoals: 3 });
    });
});
