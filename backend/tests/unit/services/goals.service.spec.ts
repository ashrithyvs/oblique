import * as goalSvc from '../../../src/services/goals.service';
import { toBlockedAppDtos, toGoalDto } from '../../../src/utils/goalDto';

jest.mock('../../../src/models/goal.model', () => ({
  __esModule: true,
  default: {
    create: jest.fn(),
    find: jest.fn(),
    findOne: jest.fn(),
    findOneAndUpdate: jest.fn(),
    deleteOne: jest.fn(),
    findById: jest.fn(),
  },
}));

jest.mock('../../../src/models/goalCheckHistory.model', () => ({
  __esModule: true,
  default: { create: jest.fn().mockResolvedValue({}) },
}));

import Goal from '../../../src/models/goal.model';

describe('goals.service', () => {
  beforeEach(() => jest.clearAllMocks());

  test('toGoalDto maps all expected fields', () => {
    const dto = toGoalDto({
      _id: 'abc',
      title: 'Test',
      platform: 'leetcode',
      platformUsername: 'user1',
      unit: 'problems',
      baselineValue: 0,
      targetValue: 3,
      progress: 1,
      status: 'active',
      deadline: 1234567890,
      checkIntervalMs: 3600000,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
    expect(dto.platform).toBe('leetcode');
    expect(dto.targetValue).toBe(3);
    expect(dto.progress).toBe(1);
  });

  test('updateGoalProgress auto-completes when target met', async () => {
    const goalId = '507f1f77bcf86cd799439011';
    const userId = '507f1f77bcf86cd799439012';
    const save = jest.fn().mockResolvedValue(undefined);
    const goalDoc = {
      _id: goalId,
      user: userId,
      baselineValue: 0,
      targetValue: 2,
      progress: 0,
      status: 'active',
      title: 't',
      platform: 'leetcode',
      unit: 'problems',
      save,
      toObject: () => ({
        _id: goalId,
        title: 't',
        platform: 'leetcode',
        unit: 'problems',
        baselineValue: 0,
        targetValue: 2,
        progress: 2,
        status: 'completed',
      }),
    };
    (Goal.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue(goalDoc),
    });

    const result = await goalSvc.updateGoalProgress(goalId, 2, userId);
    expect(result?.status).toBe('completed');
    expect(save).toHaveBeenCalled();
  });
});

describe('goalDto helpers', () => {
  test('toBlockedAppDtos wraps package names', () => {
    expect(toBlockedAppDtos(['com.app'])).toEqual([{ packageName: 'com.app' }]);
  });
});
