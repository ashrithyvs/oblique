import { createRequest, createResponse } from 'node-mocks-http';
import * as userCtrl from '../../../src/controllers/user.controller';
import * as userSvc from '../../../src/services/user.service';
import Goal from '../../../src/models/goal.model';

jest.mock('../../../src/services/user.service');
jest.mock('../../../src/models/goal.model', () => ({
  __esModule: true,
  default: {
    find: jest.fn(),
  },
}));

describe('user.controller', () => {
  beforeEach(() => jest.clearAllMocks());

  test('getCurrentUser returns profile with goals', async () => {
    (userSvc.getUserDocumentById as jest.Mock).mockResolvedValue({
      _id: 'u1',
      email: 'a@b.com',
      name: 'Test',
      displayName: 'Tester',
      platformUsernames: { leetcode: 'user1' },
      hashedPin: 'hash',
      blockedApps: ['com.app'],
    });
    (Goal.find as jest.Mock).mockReturnValue({
      select: jest.fn().mockReturnThis(),
      lean: jest.fn().mockReturnThis(),
      exec: jest.fn().mockResolvedValue([
        { _id: 'g1', platform: 'leetcode', targetValue: 3, unit: 'problems', progress: 1 },
      ]),
    });

    const req = createRequest({ user: { _id: 'u1' } });
    const res = createResponse();

    await userCtrl.getCurrentUser(req as any, res as any);
    const body = res._getJSONData();
    expect(body.email).toBe('a@b.com');
    expect(body.goals).toHaveLength(1);
    expect(body.hasPin).toBe(true);
  });

  test('getCurrentUser returns 404 when user missing', async () => {
    (userSvc.getUserDocumentById as jest.Mock).mockResolvedValue(null);
    const req = createRequest({ user: { _id: 'u1' } });
    const res = createResponse();

    await userCtrl.getCurrentUser(req as any, res as any);
    expect(res.statusCode).toBe(404);
  });

  test('listBlockedApps returns package list', async () => {
    (userSvc.getBlockedApps as jest.Mock).mockResolvedValue(['com.app']);
    const req = createRequest({ user: { _id: 'u1' } });
    const res = createResponse();

    await userCtrl.listBlockedApps(req as any, res as any);
    expect(res._getJSONData()).toEqual([{ packageName: 'com.app' }]);
  });

  test('addBlockedApp rejects missing packageName', async () => {
    const req = createRequest({ user: { _id: 'u1' }, body: {} });
    const res = createResponse();

    await userCtrl.addBlockedApp(req as any, res as any);
    expect(res.statusCode).toBe(400);
  });

  test('addBlockedApp adds package', async () => {
    (userSvc.addBlockedApp as jest.Mock).mockResolvedValue(undefined);
    (userSvc.getBlockedApps as jest.Mock).mockResolvedValue(['com.app']);
    const req = createRequest({ user: { _id: 'u1' }, body: { packageName: 'com.app' } });
    const res = createResponse();

    await userCtrl.addBlockedApp(req as any, res as any);
    expect(res._getJSONData()).toEqual(['com.app']);
  });

  test('removeBlockedApp removes package', async () => {
    (userSvc.removeBlockedApp as jest.Mock).mockResolvedValue(undefined);
    (userSvc.getBlockedApps as jest.Mock).mockResolvedValue([]);
    const req = createRequest({ user: { _id: 'u1' }, params: { pkg: 'com.app' } });
    const res = createResponse();

    await userCtrl.removeBlockedApp(req as any, res as any);
    expect(res._getJSONData()).toEqual([]);
  });

  test('replaceBlockedApps updates list', async () => {
    (userSvc.updateBlockedApps as jest.Mock).mockResolvedValue(undefined);
    (userSvc.getBlockedApps as jest.Mock).mockResolvedValue(['com.app']);

    const req = createRequest({
      user: { _id: 'u1' },
      body: { blockedApps: ['com.app'] },
    });
    const res = createResponse();

    await userCtrl.replaceBlockedApps(req as any, res as any);
    expect(res._getJSONData()).toEqual([{ packageName: 'com.app' }]);
  });

  test('updatePreferences returns updated user', async () => {
    (userSvc.updatePreferences as jest.Mock).mockResolvedValue({
      _id: 'u1',
      displayName: 'New',
      platformUsernames: { leetcode: 'user1' },
    });

    const req = createRequest({
      user: { _id: 'u1' },
      body: { displayName: 'New', usernames: { leetcode: 'user1' } },
    });
    const res = createResponse();

    await userCtrl.updatePreferences(req as any, res as any);
    expect(res._getJSONData().user.displayName).toBe('New');
  });

  test('updatePreferences returns 401 without user', async () => {
    const req = createRequest({ body: { displayName: 'New' } });
    const res = createResponse();

    await userCtrl.updatePreferences(req as any, res as any);
    expect(res.statusCode).toBe(401);
  });
});
