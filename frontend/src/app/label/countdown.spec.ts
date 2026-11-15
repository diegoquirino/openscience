import { clockOffsetMs, formatCountdown, remainingSeconds, serverTimeMs } from './countdown';

describe('countdown helper', () => {
  const server = '2026-11-15T18:00:00Z';
  const expires = '2026-11-15T18:20:00Z';

  it('prefers serverNow over the Date header', () => {
    expect(serverTimeMs(server, 'Sun, 15 Nov 2026 19:00:00 GMT')).toBe(Date.parse(server));
    expect(serverTimeMs(undefined, 'Sun, 15 Nov 2026 18:00:00 GMT')).toBe(Date.parse(server));
    expect(serverTimeMs(null, null)).toBeUndefined();
    expect(serverTimeMs('garbage', 'Sun, 15 Nov 2026 18:00:00 GMT')).toBe(Date.parse(server));
  });

  it('offsets a client clock that is ahead of the server', () => {
    const clientReceived = Date.parse(server) + 5 * 60_000;
    const offset = clockOffsetMs(server, null, clientReceived);

    expect(offset).toBe(-5 * 60_000);
    expect(remainingSeconds(expires, offset, clientReceived)).toBe(20 * 60);
    expect(remainingSeconds(expires, offset, clientReceived + 90_000)).toBe(20 * 60 - 90);
  });

  it('offsets a client clock that is behind the server and uses the Date header as a fallback', () => {
    const clientReceived = Date.parse(server) - 2 * 60_000;
    const offset = clockOffsetMs(undefined, 'Sun, 15 Nov 2026 18:00:00 GMT', clientReceived);

    expect(offset).toBe(2 * 60_000);
    expect(remainingSeconds(expires, offset, clientReceived)).toBe(20 * 60);
  });

  it('has no offset without server time', () => {
    expect(clockOffsetMs(undefined, null, 123)).toBe(0);
  });

  it('rounds up, stops at zero and handles a missing expiry', () => {
    const now = Date.parse(expires) - 1500;

    expect(remainingSeconds(expires, 0, now)).toBe(2);
    expect(remainingSeconds(expires, 0, Date.parse(expires))).toBe(0);
    expect(remainingSeconds(expires, 0, Date.parse(expires) + 60_000)).toBe(0);
    expect(remainingSeconds(undefined, 0, now)).toBeNull();
    expect(remainingSeconds('garbage', 0, now)).toBeNull();
  });

  it('formats mm:ss', () => {
    expect(formatCountdown(0)).toBe('00:00');
    expect(formatCountdown(59)).toBe('00:59');
    expect(formatCountdown(65)).toBe('01:05');
    expect(formatCountdown(20 * 60)).toBe('20:00');
    expect(formatCountdown(100 * 60 + 1)).toBe('100:01');
    expect(formatCountdown(-5)).toBe('00:00');
  });
});
