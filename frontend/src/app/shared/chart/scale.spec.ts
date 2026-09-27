import { linear, nearestIndex, niceTicks } from './scale';

describe('chart scale', () => {
  it('linear_shouldMapDomainToRange', () => {
    const y = linear([0, 100], [200, 0]); // SVG y raste naniže
    expect(y(0)).toBe(200);
    expect(y(50)).toBe(100);
    expect(y(100)).toBe(0);
  });

  it('linear_withEmptyDomain_shouldReturnMiddle', () => {
    expect(linear([5, 5], [0, 100])(5)).toBe(50);
  });

  it('niceTicks_shouldUseRoundStepsCoveringData', () => {
    expect(niceTicks(62.5, 88.67)).toEqual({ ticks: [60, 70, 80, 90], domain: [60, 90] });
    expect(niceTicks(0, 1550)).toEqual({ ticks: [0, 500, 1000, 1500, 2000], domain: [0, 2000] });
  });

  it('niceTicks_withSingleValue_shouldPadAroundIt', () => {
    const { domain } = niceTicks(80, 80);
    expect(domain[0]).toBeLessThan(80);
    expect(domain[1]).toBeGreaterThan(80);
  });

  it('niceTicks_shouldAvoidFloatingPointNoise', () => {
    expect(niceTicks(0.1, 0.7).ticks).toEqual([0, 0.2, 0.4, 0.6, 0.8]);
  });

  it('nearestIndex_shouldPickClosestX', () => {
    expect(nearestIndex([0, 10, 20, 30], 14)).toBe(1);
    expect(nearestIndex([0, 10, 20, 30], 26)).toBe(3);
  });
});
