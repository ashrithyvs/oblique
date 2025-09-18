import { createRequest, createResponse } from 'node-mocks-http';
import { requireAuth } from '../../../src/middleware/auth'; // adjust path
import * as jwtUtils from '../../../src/utils/jwt';

jest.mock('../../../src/utils/jwt');
jest.mock('../../../src/models/user.model', () => ({
    __esModule: true,
    default: {
        findById: jest.fn().mockReturnValue({
            select: jest.fn().mockReturnThis(),
            lean: jest.fn().mockReturnThis(),
            exec: jest.fn().mockResolvedValue({ _id: 'u1', email: 'test@test.com', name: 'Test User' })
        })
    }
}));

describe('requireAuth', () => {
    it('attaches user to req when token valid', async () => {
        (jwtUtils.verifyJwt as jest.Mock).mockReturnValue({ sub: 'u1' });
        const req = createRequest({ headers: { authorization: 'Bearer abc' } });
        const res = createResponse();
        const next = jest.fn();

        await requireAuth(req as any, res as any, next);
        expect((req as any).user).toBeDefined();
        expect(next).toHaveBeenCalled();
    });

    it('returns 401 when token invalid', async () => {
        (jwtUtils.verifyJwt as jest.Mock).mockImplementation(() => { throw new Error('bad'); });
        const req = createRequest({ headers: { authorization: 'Bearer abc' } });
        const res = createResponse();
        const next = jest.fn();

        await requireAuth(req as any, res as any, next);
        expect(res.statusCode).toBe(401);
    });
});
