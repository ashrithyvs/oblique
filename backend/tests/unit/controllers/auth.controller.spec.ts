import { createRequest, createResponse } from 'node-mocks-http';
import * as authCtrl from '../../../src/controllers/auth.controller';
import * as userSvc from '../../../src/services/user.service';
import * as jwtUtils from '../../../src/utils/jwt';

jest.mock('../../../src/services/user.service');
jest.mock('../../../src/utils/jwt');

describe('auth.controller', () => {
  beforeEach(() => jest.clearAllMocks());

  test('register returns token for new user', async () => {
    (userSvc.findUserByEmail as jest.Mock).mockResolvedValue(null);
    (userSvc.createUser as jest.Mock).mockResolvedValue({
      _id: 'u1',
      email: 'a@b.com',
      name: 'Test',
    });
    (jwtUtils.signJwt as jest.Mock).mockReturnValue('token123');

    const req = createRequest({
      body: { email: 'a@b.com', password: 'password123', name: 'Test' },
    });
    const res = createResponse();

    await authCtrl.register(req as any, res as any);
    expect(res._getJSONData().token).toBe('token123');
  });

  test('register rejects duplicate email', async () => {
    (userSvc.findUserByEmail as jest.Mock).mockResolvedValue({ _id: 'u1' });
    const req = createRequest({
      body: { email: 'a@b.com', password: 'password123' },
    });
    const res = createResponse();

    await authCtrl.register(req as any, res as any);
    expect(res.statusCode).toBe(400);
  });

  test('loginByEmail returns token for valid credentials', async () => {
    (userSvc.verifyUserPasswordByEmail as jest.Mock).mockResolvedValue({
      _id: 'u1',
      email: 'a@b.com',
      name: 'Test',
    });
    (jwtUtils.signJwt as jest.Mock).mockReturnValue('token123');

    const req = createRequest({
      body: { email: 'a@b.com', password: 'password123' },
    });
    const res = createResponse();

    await authCtrl.loginByEmail(req as any, res as any);
    expect(res._getJSONData().token).toBe('token123');
  });

  test('loginByEmail returns 401 for invalid credentials', async () => {
    (userSvc.verifyUserPasswordByEmail as jest.Mock).mockResolvedValue(null);
    const req = createRequest({
      body: { email: 'a@b.com', password: 'wrongpass1' },
    });
    const res = createResponse();

    await authCtrl.loginByEmail(req as any, res as any);
    expect(res.statusCode).toBe(401);
  });
});
