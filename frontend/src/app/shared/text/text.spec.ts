import { normalize } from './normalize';
import { plural } from './plural';

describe('normalize', () => {
  it('shouldIgnoreCaseAndDiacritics', () => {
    expect(normalize('Čučanj')).toBe('cucanj');
    expect(normalize(' Leđa ')).toBe('ledja');
  });
});

describe('plural', () => {
  it.each([
    [1, 'vežba'],
    [3, 'vežbe'],
    [5, 'vežbi'],
    [11, 'vežbi'],
    [12, 'vežbi'],
    [21, 'vežba'],
    [23, 'vežbe'],
  ])('%i → %s', (count, expected) => {
    expect(plural(count, 'vežba', 'vežbe', 'vežbi')).toBe(expected);
  });
});
