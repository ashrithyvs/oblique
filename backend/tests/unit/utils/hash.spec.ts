import { hashPin, verifyPin } from '../../../src/utils/hash.ts';

describe('hash utils', () => {
    it('hash and verify should work', async () => {
        const pin = '123456';
        const hashed = await hashPin(pin);
        expect(typeof hashed).toBe('string');
        const ok = await verifyPin(pin, hashed);
        expect(ok).toBe(true);
        const wrong = await verifyPin('000000', hashed);
        expect(wrong).toBe(false);
    });
});
