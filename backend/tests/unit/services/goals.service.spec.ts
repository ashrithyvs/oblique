/**
 * Unit tests for services/goals.service.ts
 * - Mocks Mongoose Goal model methods
 */

import { jest } from '@jest/globals';

// mock the model module path exactly as in your service
const mockGoalModel: any = {
    create: jest.fn(),
    find: jest.fn(),
    findOne: jest.fn(),
    findOneAndUpdate: jest.fn(),
    findOneAndDelete: jest.fn(),
    aggregate: jest.fn(),
};

// Ensure both default and named export are available (your model file exports both)
jest.mock('../../../src/models/goal.model.ts', () => ({
    __esModule: true,
    default: mockGoalModel,
    Goal: mockGoalModel,
}));

// import the real service after mocking the model
import * as goalsService from '../../../src/services/goals.service.ts';

describe('goals.service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    const USER_ID = '507f1f77bcf86cd799439011'; // Valid ObjectId string
    const EXAMPLE_GOAL = {
        _id: 'goal-1',
        user: USER_ID,
        platform: 'LeetCode',
        unit: 'lessons',
        targetValue: 2,
        progress: 0,
    };

    it('createGoal should call Goal.create and return created doc', async () => {
        mockGoalModel.create.mockResolvedValue(EXAMPLE_GOAL);

        const payload = {
            platform: 'LeetCode',
            unit: 'lessons',
            targetValue: 2,
        };

        const created = await goalsService.createGoal(USER_ID, payload);
        expect(mockGoalModel.create).toHaveBeenCalledTimes(1);
        // ensure create called with same payload (or with expected shape)
        expect(mockGoalModel.create).toHaveBeenCalledWith(expect.objectContaining({
            platform: 'LeetCode',
            unit: 'lessons',
            targetValue: 2,
        }));
        expect(created).toBe(EXAMPLE_GOAL);
    });

    it('listGoals should call Goal.find with user filter and return list', async () => {
        const mockQuery = {
            sort: jest.fn().mockReturnThis(),
            skip: jest.fn().mockReturnThis(),
            limit: jest.fn().mockReturnThis(),
            select: jest.fn().mockReturnThis(),
            lean: jest.fn().mockReturnThis(),
            exec: jest.fn().mockImplementation(() => Promise.resolve([EXAMPLE_GOAL])),
        };
        (mockGoalModel.find as jest.Mock).mockReturnValue(mockQuery);

        const list = await goalsService.listGoals(USER_ID);
        expect(mockGoalModel.find).toHaveBeenCalledWith({ user: expect.any(Object) });
        expect(Array.isArray(list)).toBe(true);
        expect(list[0]).toMatchObject({ platform: 'LeetCode' });
    });

    it('updateGoal should call findOneAndUpdate and return updated', async () => {
        const mockQuery = {
            lean: jest.fn().mockReturnThis(),
            exec: jest.fn().mockImplementation(() => Promise.resolve({ ...EXAMPLE_GOAL, targetValue: 4 }))
        };
        (mockGoalModel.findOneAndUpdate as jest.Mock).mockReturnValue(mockQuery);
        const updated = await goalsService.updateGoal(USER_ID, 'goal-1', { targetValue: 4 } as any);
        expect(mockGoalModel.findOneAndUpdate).toHaveBeenCalledWith({
            _id: 'goal-1',
            user: USER_ID,
        }, { $set: { targetValue: 4 } }, { new: true });
        expect((updated as any).targetValue).toBe(4);
    });

    it('deleteGoal should call findOneAndDelete with correct filter', async () => {
        const mockQuery = {
            exec: jest.fn().mockImplementation(() => Promise.resolve({ deletedCount: 1 }))
        };
        (mockGoalModel.findOneAndDelete as jest.Mock).mockReturnValue(mockQuery);
        await goalsService.deleteGoal(USER_ID, 'goal-1');
        expect(mockGoalModel.findOneAndDelete).toHaveBeenCalledWith({ _id: 'goal-1', user: USER_ID });
    });

    it('incrementProgress should increment progress and return updated doc', async () => {
        const mockQuery = {
            lean: jest.fn().mockReturnThis(),
            exec: jest.fn().mockImplementation(() => Promise.resolve({ ...EXAMPLE_GOAL, progress: 1 }))
        };
        (mockGoalModel.findOneAndUpdate as jest.Mock).mockReturnValue(mockQuery);
        const out = await goalsService.incrementProgress(USER_ID, 'goal-1');
        expect(mockGoalModel.findOneAndUpdate).toHaveBeenCalledWith(
            { _id: 'goal-1', user: USER_ID },
            { $inc: { progress: 1 } },
            { new: true }
        );
        expect((out as any).progress).toBe(1);
    });

    it('dashboardSummary should call aggregate and return transformed result', async () => {
        const aggRes = [{ totalGoals: 3, completedCount: 1, totalTarget: 10, totalProgress: 5, successRate: 0.33 }];
        const mockQuery = {
            exec: jest.fn().mockImplementation(() => Promise.resolve(aggRes))
        };
        (mockGoalModel.aggregate as jest.Mock).mockReturnValue(mockQuery);
        const res = await goalsService.dashboardSummary(USER_ID);
        expect(mockGoalModel.aggregate).toHaveBeenCalled();
        expect(res.totalGoals).toBeDefined();
    });
});
