import * as jwtUtils from '../../../src/utils/jwt';

describe('jwt utils', () => {
    const payload = { id: 'user1', email: 'a@b.com' };

    it('should sign and verify token', () => {
        const token = jwtUtils.signJwt(payload);
        expect(typeof token).toBe('string');
        const decoded = jwtUtils.verifyJwt(token);
        expect(decoded).toMatchObject({ id: 'user1', email: 'a@b.com' });
    });
});
