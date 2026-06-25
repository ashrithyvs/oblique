import {
  registerSchema,
  loginSchema,
  createGoalSchema,
  completeGoalSchema,
  updateGoalProgressSchema,
} from '../../../src/utils/validators';

describe('validators', () => {
  test('registerSchema accepts valid payload', () => {
    const parsed = registerSchema.parse({
      email: 'a@b.com',
      password: 'password123',
      name: 'Test',
    });
    expect(parsed.email).toBe('a@b.com');
  });

  test('loginSchema requires email', () => {
    expect(() => loginSchema.parse({ password: 'password123' })).toThrow();
  });

  test('createGoalSchema requires positive targetValue', () => {
    expect(() => createGoalSchema.parse({ targetValue: 0 })).toThrow();
    const parsed = createGoalSchema.parse({ targetValue: 3, platform: 'leetcode' });
    expect(parsed.targetValue).toBe(3);
  });

  test('completeGoalSchema accepts optional evidence', () => {
    const parsed = completeGoalSchema.parse({
      completedAt: Date.now(),
      evidence: { solved: 2 },
    });
    expect(parsed.evidence).toEqual({ solved: 2 });
  });

  test('updateGoalProgressSchema requires progress number', () => {
    expect(() => updateGoalProgressSchema.parse({})).toThrow();
    const parsed = updateGoalProgressSchema.parse({ progress: 5, evidence: { ok: true } });
    expect(parsed.progress).toBe(5);
    expect(parsed.evidence).toEqual({ ok: true });
  });
});
