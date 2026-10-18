import { describe, expect, it } from 'vitest';
import { looksLikeWalletId, parseCollectPayload } from './collectPayload';

describe('parseCollectPayload', () => {
  it('parses drippay scheme', () => {
    expect(parseCollectPayload('drippay://collect?handle=demo@drippay')).toBe('demo@drippay');
  });

  it('parses web pay URL', () => {
    expect(parseCollectPayload('http://localhost:5173/collect/pay?handle=friend@drippay')).toBe('friend@drippay');
  });

  it('parses bare handle', () => {
    expect(parseCollectPayload('friend@drippay')).toBe('friend@drippay');
  });

  it('returns null for wallet id', () => {
    expect(parseCollectPayload('DRIPABCDEF12')).toBeNull();
    expect(looksLikeWalletId('DRIPABCDEF12')).toBe(true);
  });
});
