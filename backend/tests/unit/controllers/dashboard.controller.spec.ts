import { createRequest, createResponse } from 'node-mocks-http';
import * as dashboardCtrl from '../../../src/controllers/dashboard.controller';
import * as goalSvc from '../../../src/services/goals.service';
import * as userSvc from '../../../src/services/user.service';

jest.mock('../../../src/services/goals.service');
jest.mock('../../../src/services/user.service');

describe('dashboard.controller', () => {
  beforeEach(() => jest.clearAllMocks());

  test('getDashboard aggregates goals and blocked apps', async () => {
    (goalSvc.listGoalsForUser as jest.Mock).mockResolvedValue([{ id: 'g1' }]);
    (userSvc.getBlockedApps as jest.Mock).mockResolvedValue(['com.app']);

    const req = createRequest({ user: { _id: 'u1' } });
    const res = createResponse();

    await dashboardCtrl.getDashboard(req as any, res as any);
    expect(res._getJSONData()).toEqual({
      goals: [{ id: 'g1' }],
      blockedApps: [{ packageName: 'com.app' }],
    });
  });

  test('getDashboard returns 500 on service error', async () => {
    (goalSvc.listGoalsForUser as jest.Mock).mockRejectedValue(new Error('db down'));
    const req = createRequest({ user: { _id: 'u1' } });
    const res = createResponse();

    await dashboardCtrl.getDashboard(req as any, res as any);
    expect(res.statusCode).toBe(500);
  });
});
