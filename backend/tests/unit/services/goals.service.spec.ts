import * as goalSvc from '../../../src/services/goals.service';
import { GoalServiceError } from '../../../src/services/goals.service';
import { toBlockedAppDtos, toGoalDto } from '../../../src/utils/goalDto';
import GoalCheckHistory from '../../../src/models/goalCheckHistory.model';

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

const goalId = '507f1f77bcf86cd799439011';
const userId = '507f1f77bcf86cd799439012';

function makeGoalDoc(overrides: Record<string, unknown> = {}) {
  const save = jest.fn().mockResolvedValue(undefined);
  const doc = {
    _id: goalId,
    user: userId,
    baselineValue: 0,
    targetValue: 5,
    progress: 2,
    status: 'active',
    title: 'Test goal',
    platform: 'leetcode',
    unit: 'problems',
    evidence: null,
    lastCheckedAt: null,
    save,
    toObject: () => ({
      _id: goalId,
      title: 'Test goal',
      platform: 'leetcode',
      unit: 'problems',
      baselineValue: 0,
      targetValue: 5,
      progress: (doc as any).progress,
      status: (doc as any).status,
      lastCheckedAt: (doc as any).lastCheckedAt,
      evidence: (doc as any).evidence,
    }),
    ...overrides,
  };
  return doc;
}

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
    const goalDoc = makeGoalDoc({ progress: 0, targetValue: 2 });
    (Goal.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue(goalDoc),
    });

    const result = await goalSvc.updateGoalProgress(goalId, 2, userId);
    expect(result?.status).toBe('completed');
    expect(goalDoc.save).toHaveBeenCalled();
    expect(GoalCheckHistory.create).toHaveBeenCalledTimes(1);
  });

  test('updateGoalProgress rejects regression', async () => {
    const goalDoc = makeGoalDoc({ progress: 3 });
    (Goal.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue(goalDoc),
    });

    await expect(goalSvc.updateGoalProgress(goalId, 2, userId)).rejects.toMatchObject({
      code: 'PROGRESS_REGRESSION',
    });
    expect(goalDoc.save).not.toHaveBeenCalled();
  });

  test('updateGoalProgress rejects completed goal', async () => {
    const goalDoc = makeGoalDoc({ status: 'completed', progress: 5 });
    (Goal.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue(goalDoc),
    });

    await expect(goalSvc.updateGoalProgress(goalId, 5, userId)).rejects.toMatchObject({
      code: 'GOAL_COMPLETED',
    });
  });

  test('updateGoalProgress sets lastCheckedAt and evidence', async () => {
    const goalDoc = makeGoalDoc({ progress: 2 });
    (Goal.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue(goalDoc),
    });

    const evidence = { solved: 3 };
    await goalSvc.updateGoalProgress(goalId, 3, userId, evidence);

    expect(goalDoc.lastCheckedAt).toBeInstanceOf(Date);
    expect(goalDoc.evidence).toEqual(evidence);
    expect(goalDoc.progress).toBe(3);
  });

  test('markComplete is idempotent for GoalCheckHistory', async () => {
    const goalDoc = makeGoalDoc({ status: 'completed', progress: 5 });
    (Goal.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue(goalDoc),
    });

    await goalSvc.markComplete(userId, goalId, new Date(), { via: 'manual' });
    expect(GoalCheckHistory.create).not.toHaveBeenCalled();
    expect(goalDoc.save).not.toHaveBeenCalled();
  });

  test('markComplete writes history only once', async () => {
    const goalDoc = makeGoalDoc({ status: 'active', progress: 4 });
    (Goal.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue(goalDoc),
    });

    await goalSvc.markComplete(userId, goalId, new Date(), { via: 'manual' }, { proof: true });
    expect(GoalCheckHistory.create).toHaveBeenCalledTimes(1);
    expect(goalDoc.status).toBe('completed');
  });

  test('updateGoalForUser rejects completed goal', async () => {
    const goalDoc = makeGoalDoc({ status: 'completed' });
    (Goal.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue(goalDoc),
    });

    await expect(
      goalSvc.updateGoalForUser(userId, goalId, { targetValue: 10 }),
    ).rejects.toBeInstanceOf(GoalServiceError);
  });

  test('updateGoalForUser throws when no valid fields', async () => {
    const goalDoc = makeGoalDoc();
    (Goal.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue(goalDoc),
    });

    await expect(goalSvc.updateGoalForUser(userId, goalId, {})).rejects.toMatchObject({
      code: 'NO_VALID_FIELDS',
    });
  });

  test('createGoal creates document', async () => {
    (Goal.create as jest.Mock).mockResolvedValue({
      toObject: () => ({
        _id: goalId,
        title: 'leetcode goal',
        platform: 'leetcode',
        targetValue: 3,
        progress: 0,
        status: 'active',
        unit: '',
        baselineValue: 0,
        checkIntervalMs: 3600000,
      }),
    });

    const result = await goalSvc.createGoal(userId, {
      platform: 'leetcode',
      targetValue: 3,
    });
    expect(result.platform).toBe('leetcode');
    expect(Goal.create).toHaveBeenCalled();
  });

  test('deleteGoalForUser returns false for invalid id', async () => {
    const deleted = await goalSvc.deleteGoalForUser(userId, 'bad-id');
    expect(deleted).toBe(false);
  });

  test('listGoalsForUser returns sorted goals', async () => {
    (Goal.find as jest.Mock).mockReturnValue({
      sort: jest.fn().mockReturnThis(),
      lean: jest.fn().mockReturnThis(),
      exec: jest.fn().mockResolvedValue([
        { _id: goalId, title: 'g', platform: 'leetcode', targetValue: 3, progress: 1, status: 'active', unit: '', baselineValue: 0, checkIntervalMs: 3600000 },
      ]),
    });

    const result = await goalSvc.listGoalsForUser(userId);
    expect(result).toHaveLength(1);
  });

  test('updateGoalForUser updates allowed fields', async () => {
    const goalDoc = makeGoalDoc();
    (Goal.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue(goalDoc),
    });
    (Goal.findOneAndUpdate as jest.Mock).mockReturnValue({
      lean: jest.fn().mockResolvedValue({
        _id: goalId,
        title: 'Updated',
        platform: 'leetcode',
        targetValue: 10,
        progress: 2,
        status: 'active',
        unit: 'problems',
        baselineValue: 0,
        checkIntervalMs: 3600000,
      }),
    });

    const result = await goalSvc.updateGoalForUser(userId, goalId, { targetValue: 10 });
    expect(result?.targetValue).toBe(10);
  });

  test('getGoalForUser returns dto when found', async () => {
    (Goal.findOne as jest.Mock).mockReturnValue({
      lean: jest.fn().mockReturnThis(),
      exec: jest.fn().mockResolvedValue({
        _id: goalId,
        title: 'g',
        platform: 'leetcode',
        targetValue: 3,
        progress: 1,
        status: 'active',
        unit: '',
        baselineValue: 0,
        checkIntervalMs: 3600000,
      }),
    });

    const result = await goalSvc.getGoalForUser(userId, goalId);
    expect(result?.id).toBe(goalId);
  });

  test('deleteGoalForUser deletes when found', async () => {
    (Goal.deleteOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue({ deletedCount: 1 }),
    });

    const deleted = await goalSvc.deleteGoalForUser(userId, goalId);
    expect(deleted).toBe(true);
  });

  test('markComplete updates evidence when already completed', async () => {
    const goalDoc = makeGoalDoc({ status: 'completed', progress: 5 });
    (Goal.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue(goalDoc),
    });

    await goalSvc.markComplete(userId, goalId, new Date(), { via: 'manual' }, { new: true });
    expect(goalDoc.evidence).toEqual({ new: true });
    expect(goalDoc.save).toHaveBeenCalled();
  });

  test('markComplete throws when goal not found', async () => {
    (Goal.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue(null),
    });

    await expect(
      goalSvc.markComplete(userId, goalId, new Date(), {}),
    ).rejects.toMatchObject({ code: 'NOT_FOUND' });
  });

  test('markComplete throws for invalid id', async () => {
    await expect(
      goalSvc.markComplete(userId, 'bad-id', new Date(), {}),
    ).rejects.toMatchObject({ code: 'INVALID_ID' });
  });

  test('markComplete continues when history write fails', async () => {
    const goalDoc = makeGoalDoc({ status: 'active' });
    (Goal.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue(goalDoc),
    });
    (GoalCheckHistory.create as jest.Mock).mockRejectedValue(new Error('history fail'));

    const result = await goalSvc.markComplete(userId, goalId, new Date(), { via: 'manual' });
    expect(result.status).toBe('completed');
  });
});

describe('goalDto helpers', () => {
  test('toBlockedAppDtos wraps package names', () => {
    expect(toBlockedAppDtos(['com.app'])).toEqual([{ packageName: 'com.app' }]);
  });
});
