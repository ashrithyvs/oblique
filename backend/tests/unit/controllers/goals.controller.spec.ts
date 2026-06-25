import { createRequest, createResponse } from 'node-mocks-http';
import * as goalCtrl from '../../../src/controllers/goals.controller';
import { GoalServiceError } from '../../../src/services/goals.service';

jest.mock('../../../src/services/goals.service', () => {
  const actual = jest.requireActual('../../../src/services/goals.service');
  return {
    ...actual,
    listGoalsForUser: jest.fn(),
    createGoal: jest.fn(),
    deleteGoalForUser: jest.fn(),
    markComplete: jest.fn(),
    getGoalForUser: jest.fn(),
    updateGoalProgress: jest.fn(),
    updateGoalForUser: jest.fn(),
  };
});

import * as goalSvc from '../../../src/services/goals.service';

describe('goals.controller', () => {
  beforeEach(() => jest.clearAllMocks());

  test('listGoals returns goals', async () => {
    (goalSvc.listGoalsForUser as jest.Mock).mockResolvedValue([{ id: 'g1' }]);
    const req = createRequest({ user: { _id: 'u1' } });
    const res = createResponse();

    await goalCtrl.listGoals(req as any, res as any);
    expect(res._getJSONData()).toEqual([{ id: 'g1' }]);
  });

  test('createGoal returns 201', async () => {
    (goalSvc.createGoal as jest.Mock).mockResolvedValue({ id: 'g1' });
    const req = createRequest({ user: { _id: 'u1' }, body: { targetValue: 3 } });
    const res = createResponse();

    await goalCtrl.createGoal(req as any, res as any);
    expect(res.statusCode).toBe(201);
  });

  test('getGoal returns 404 when missing', async () => {
    (goalSvc.getGoalForUser as jest.Mock).mockResolvedValue(null);
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: '507f1f77bcf86cd799439011' },
    });
    const res = createResponse();

    await goalCtrl.getGoal(req as any, res as any);
    expect(res.statusCode).toBe(404);
  });

  test('completeGoal validates body with schema', async () => {
    (goalSvc.markComplete as jest.Mock).mockResolvedValue({ id: 'g1', status: 'completed' });
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: '507f1f77bcf86cd799439011' },
      body: { completedAt: Date.now(), evidence: { solved: 3 } },
    });
    const res = createResponse();

    await goalCtrl.completeGoal(req as any, res as any);
    expect(goalSvc.markComplete).toHaveBeenCalled();
    expect(res._getJSONData().status).toBe('completed');
  });

  test('completeGoal returns 400 for invalid id', async () => {
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: 'bad-id' },
      body: {},
    });
    const res = createResponse();

    await goalCtrl.completeGoal(req as any, res as any);
    expect(res.statusCode).toBe(400);
  });

  test('updateGoalProgress accepts evidence', async () => {
    (goalSvc.updateGoalProgress as jest.Mock).mockResolvedValue({ id: 'g1', progress: 3 });
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: '507f1f77bcf86cd799439011' },
      body: { progress: 3, evidence: { source: 'leetcode' } },
    });
    const res = createResponse();

    await goalCtrl.updateGoalProgress(req as any, res as any);
    expect(goalSvc.updateGoalProgress).toHaveBeenCalledWith(
      '507f1f77bcf86cd799439011',
      3,
      'u1',
      { source: 'leetcode' },
    );
  });

  test('updateGoalProgress maps regression error to 400', async () => {
    (goalSvc.updateGoalProgress as jest.Mock).mockRejectedValue(
      new GoalServiceError('Progress regression not allowed', 'PROGRESS_REGRESSION'),
    );
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: '507f1f77bcf86cd799439011' },
      body: { progress: 1 },
    });
    const res = createResponse();

    await goalCtrl.updateGoalProgress(req as any, res as any);
    expect(res.statusCode).toBe(400);
    expect(res._getJSONData().code).toBe('PROGRESS_REGRESSION');
  });

  test('updateGoalProgress returns 404 when goal missing', async () => {
    (goalSvc.updateGoalProgress as jest.Mock).mockResolvedValue(null);
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: '507f1f77bcf86cd799439011' },
      body: { progress: 3 },
    });
    const res = createResponse();

    await goalCtrl.updateGoalProgress(req as any, res as any);
    expect(res.statusCode).toBe(404);
  });

  test('deleteGoal returns 404 when not found', async () => {
    (goalSvc.deleteGoalForUser as jest.Mock).mockResolvedValue(false);
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: '507f1f77bcf86cd799439011' },
    });
    const res = createResponse();

    await goalCtrl.deleteGoal(req as any, res as any);
    expect(res.statusCode).toBe(404);
  });

  test('updateGoal maps completed goal error', async () => {
    (goalSvc.updateGoalForUser as jest.Mock).mockRejectedValue(
      new GoalServiceError('Cannot update completed goal', 'GOAL_COMPLETED'),
    );
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: '507f1f77bcf86cd799439011' },
      body: { targetValue: 10 },
    });
    const res = createResponse();

    await goalCtrl.updateGoal(req as any, res as any);
    expect(res.statusCode).toBe(400);
  });

  test('updateGoal returns updated goal', async () => {
    (goalSvc.updateGoalForUser as jest.Mock).mockResolvedValue({ id: 'g1', targetValue: 10 });
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: '507f1f77bcf86cd799439011' },
      body: { targetValue: 10 },
    });
    const res = createResponse();

    await goalCtrl.updateGoal(req as any, res as any);
    expect(res._getJSONData().targetValue).toBe(10);
  });

  test('listGoals handles service error', async () => {
    (goalSvc.listGoalsForUser as jest.Mock).mockRejectedValue(new Error('db'));
    const req = createRequest({ user: { _id: 'u1' } });
    const res = createResponse();

    await goalCtrl.listGoals(req as any, res as any);
    expect(res.statusCode).toBe(500);
  });

  test('createGoal returns 400 on validation error', async () => {
    (goalSvc.createGoal as jest.Mock).mockRejectedValue(new Error('invalid'));
    const req = createRequest({ user: { _id: 'u1' }, body: {} });
    const res = createResponse();

    await goalCtrl.createGoal(req as any, res as any);
    expect(res.statusCode).toBe(400);
  });

  test('deleteGoal returns 400 for invalid id', async () => {
    const req = createRequest({ user: { _id: 'u1' }, params: { id: 'bad' } });
    const res = createResponse();

    await goalCtrl.deleteGoal(req as any, res as any);
    expect(res.statusCode).toBe(400);
  });

  test('completeGoal returns 404 when goal not found', async () => {
    (goalSvc.markComplete as jest.Mock).mockRejectedValue(
      new GoalServiceError('Goal not found', 'NOT_FOUND'),
    );
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: '507f1f77bcf86cd799439011' },
      body: {},
    });
    const res = createResponse();

    await goalCtrl.completeGoal(req as any, res as any);
    expect(res.statusCode).toBe(404);
  });

  test('updateGoalProgress returns 400 for invalid body', async () => {
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: '507f1f77bcf86cd799439011' },
      body: {},
    });
    const res = createResponse();

    await goalCtrl.updateGoalProgress(req as any, res as any);
    expect(res.statusCode).toBe(400);
  });

  test('updateGoal returns 400 for invalid id', async () => {
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: 'bad' },
      body: { targetValue: 10 },
    });
    const res = createResponse();

    await goalCtrl.updateGoal(req as any, res as any);
    expect(res.statusCode).toBe(400);
  });

  test('getGoal returns goal when found', async () => {
    (goalSvc.getGoalForUser as jest.Mock).mockResolvedValue({ id: 'g1' });
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: '507f1f77bcf86cd799439011' },
    });
    const res = createResponse();

    await goalCtrl.getGoal(req as any, res as any);
    expect(res._getJSONData().id).toBe('g1');
  });

  test('completeGoal uses default completedAt when omitted', async () => {
    (goalSvc.markComplete as jest.Mock).mockResolvedValue({ id: 'g1', status: 'completed' });
    const req = createRequest({
      user: { _id: 'u1' },
      params: { id: '507f1f77bcf86cd799439011' },
      body: {},
    });
    const res = createResponse();

    await goalCtrl.completeGoal(req as any, res as any);
    expect(goalSvc.markComplete).toHaveBeenCalled();
    expect(res.statusCode).toBe(200);
  });
});
