import { describe, expect, it } from 'vitest';
import { formatINR } from './format';

describe('formatINR', () => {
  it('formats Indian rupees', () => {
    expect(formatINR(1234.5)).toContain('1,234.50');
    expect(formatINR(1234.5)).toContain('₹');
  });
});
