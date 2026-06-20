import { toBlockedAppDtos, toGoalDto } from '../../../src/utils/goalDto';

describe('goalDto', () => {
  test('toGoalDto includes progress and deadline', () => {
    const dto = toGoalDto({
      _id: '1',
      title: 'Daily',
      platform: 'leetcode',
      unit: 'problems',
      baselineValue: 0,
      targetValue: 5,
      progress: 2,
      status: 'active',
      deadline: 999,
    });
    expect(dto.id).toBe('1');
    expect(dto.progress).toBe(2);
    expect(dto.deadline).toBe(999);
  });

  test('toBlockedAppDtos standardizes blocked app responses', () => {
    expect(toBlockedAppDtos(['a.b', 'c.d'])).toEqual([
      { packageName: 'a.b' },
      { packageName: 'c.d' },
    ]);
  });
});
