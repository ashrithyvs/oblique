import * as userSvc from '../../../src/services/user.service';
import User from '../../../src/models/user.model';
import * as hash from '../../../src/utils/hash';

jest.mock('../../../src/models/user.model', () => ({
  __esModule: true,
  default: {
    create: jest.fn(),
    findOne: jest.fn(),
    findById: jest.fn(),
    findByIdAndUpdate: jest.fn(),
  },
}));

jest.mock('../../../src/utils/hash', () => ({
  hashPin: jest.fn().mockResolvedValue('hashed-pin'),
  verifyPin: jest.fn(),
  hashPassword: jest.fn().mockResolvedValue('hashed-password'),
  verifyPassword: jest.fn(),
}));

describe('user.service', () => {
  beforeEach(() => jest.clearAllMocks());

  test('createUser hashes pin and password', async () => {
    (User.create as jest.Mock).mockResolvedValue({ _id: 'u1', email: 'a@b.com' });

    await userSvc.createUser({
      email: 'a@b.com',
      pin: '1234',
      password: 'password123',
    });

    expect(hash.hashPin).toHaveBeenCalledWith('1234');
    expect(hash.hashPassword).toHaveBeenCalledWith('password123');
    expect(User.create).toHaveBeenCalled();
  });

  test('findUserByEmail returns null for empty email', async () => {
    const result = await userSvc.findUserByEmail(null);
    expect(result).toBeNull();
  });

  test('findUserByEmail queries database', async () => {
    (User.findOne as jest.Mock).mockReturnValue({
      exec: jest.fn().mockResolvedValue({ _id: 'u1' }),
    });
    const result = await userSvc.findUserByEmail('a@b.com');
    expect(result).toEqual({ _id: 'u1' });
  });

  test('getUserById returns null for invalid id', async () => {
    const result = await userSvc.getUserById('bad-id');
    expect(result).toBeNull();
  });

  test('getUserById returns lean user', async () => {
    (User.findById as jest.Mock).mockReturnValue({
      select: jest.fn().mockReturnThis(),
      lean: jest.fn().mockReturnThis(),
      exec: jest.fn().mockResolvedValue({ _id: 'u1', email: 'a@b.com' }),
    });

    const result = await userSvc.getUserById('507f1f77bcf86cd799439011');
    expect(result?.email).toBe('a@b.com');
  });

  test('verifyUserPasswordByEmail returns user when password valid', async () => {
    const user = { _id: 'u1', email: 'a@b.com', name: 'Test' };
    (User.findOne as jest.Mock).mockReturnValue({
      select: jest.fn().mockReturnThis(),
      exec: jest.fn().mockResolvedValue({ ...user, hashedPassword: 'hash' }),
    });
    (hash.verifyPassword as jest.Mock).mockResolvedValue(true);

    const result = await userSvc.verifyUserPasswordByEmail('a@b.com', 'password123');
    expect(result?.email).toBe('a@b.com');
  });

  test('verifyUserPin returns false when no pin', async () => {
    (User.findById as jest.Mock).mockReturnValue({
      select: jest.fn().mockReturnThis(),
      exec: jest.fn().mockResolvedValue({ hashedPin: null }),
    });

    const ok = await userSvc.verifyUserPin('507f1f77bcf86cd799439011', '1234');
    expect(ok).toBe(false);
  });

  test('setUserPin hashes and updates', async () => {
    (User.findByIdAndUpdate as jest.Mock).mockReturnValue({ exec: jest.fn().mockResolvedValue({}) });
    await userSvc.setUserPin('507f1f77bcf86cd799439011', '1234');
    expect(hash.hashPin).toHaveBeenCalled();
  });

  test('getBlockedApps returns empty when user missing', async () => {
    (User.findById as jest.Mock).mockReturnValue({
      select: jest.fn().mockReturnThis(),
      lean: jest.fn().mockReturnThis(),
      exec: jest.fn().mockResolvedValue(null),
    });

    const apps = await userSvc.getBlockedApps('507f1f77bcf86cd799439011');
    expect(apps).toEqual([]);
  });

  test('addBlockedApp no-ops for invalid package', async () => {
    await userSvc.addBlockedApp('u1', '');
    expect(User.findByIdAndUpdate).not.toHaveBeenCalled();
  });

  test('addBlockedApp updates user', async () => {
    (User.findByIdAndUpdate as jest.Mock).mockReturnValue({ exec: jest.fn().mockResolvedValue({}) });
    await userSvc.addBlockedApp('507f1f77bcf86cd799439011', 'com.app');
    expect(User.findByIdAndUpdate).toHaveBeenCalled();
  });

  test('removeBlockedApp updates user', async () => {
    (User.findByIdAndUpdate as jest.Mock).mockReturnValue({ exec: jest.fn().mockResolvedValue({}) });
    await userSvc.removeBlockedApp('507f1f77bcf86cd799439011', 'com.app');
    expect(User.findByIdAndUpdate).toHaveBeenCalled();
  });

  test('updateBlockedApps cleans list', async () => {
    (User.findByIdAndUpdate as jest.Mock).mockReturnValue({ exec: jest.fn().mockResolvedValue({}) });
    await userSvc.updateBlockedApps('507f1f77bcf86cd799439011', ['com.app', '', '  ']);
    expect(User.findByIdAndUpdate).toHaveBeenCalledWith(
      '507f1f77bcf86cd799439011',
      { blockedApps: ['com.app'] },
    );
  });

  test('updatePreferences merges usernames', async () => {
    const save = jest.fn().mockResolvedValue(undefined);
    const user = {
      displayName: 'Old',
      platformUsernames: { leetcode: 'olduser' },
      save,
    };
    (User.findById as jest.Mock).mockResolvedValue(user);

    const updated = await userSvc.updatePreferences('u1', 'New Name', { duolingo: 'duo1' });
    expect(updated.displayName).toBe('New Name');
    expect(updated.platformUsernames).toEqual({ leetcode: 'olduser', duolingo: 'duo1' });
    expect(save).toHaveBeenCalled();
  });

  test('updatePreferences removes empty username keys', async () => {
    const save = jest.fn().mockResolvedValue(undefined);
    const user = {
      displayName: 'Old',
      platformUsernames: { leetcode: 'user', duolingo: '' },
      save,
    };
    (User.findById as jest.Mock).mockResolvedValue(user);

    await userSvc.updatePreferences('u1', undefined, { duolingo: '' });
    expect(user.platformUsernames).toEqual({ leetcode: 'user' });
  });
});
