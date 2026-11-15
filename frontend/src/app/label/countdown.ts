/**
 * Reservation countdown (FR-013, SC-005). The remaining time is computed against the server clock:
 * the offset is taken from the `serverNow` of the task (or the `Date` header when absent) at the
 * moment the response was received, so a wrong client clock does not shorten or stretch the time.
 */

/** Server time in epoch milliseconds from `serverNow` (ISO-8601) or, failing that, the `Date` header. */
export function serverTimeMs(serverNow: string | null | undefined, dateHeader: string | null | undefined): number | undefined {
  for (const value of [serverNow, dateHeader]) {
    const parsed = value ? Date.parse(value) : NaN;
    if (!Number.isNaN(parsed)) {
      return parsed;
    }
  }
  return undefined;
}

/** `server - client` at the instant the response arrived; 0 when the server time is unknown. */
export function clockOffsetMs(
  serverNow: string | null | undefined,
  dateHeader: string | null | undefined,
  receivedAtMs: number,
): number {
  const server = serverTimeMs(serverNow, dateHeader);
  return server === undefined ? 0 : server - receivedAtMs;
}

/** Whole seconds left until `expiresAt` by the server clock, never negative; `null` when unknown. */
export function remainingSeconds(expiresAt: string | null | undefined, offsetMs: number, nowMs: number): number | null {
  const expires = expiresAt ? Date.parse(expiresAt) : NaN;
  if (Number.isNaN(expires)) {
    return null;
  }
  return Math.max(0, Math.ceil((expires - (nowMs + offsetMs)) / 1000));
}

/** `mm:ss`; minutes are not capped at 59. */
export function formatCountdown(totalSeconds: number): string {
  const seconds = Math.max(0, Math.floor(totalSeconds));
  const minutes = Math.floor(seconds / 60);
  return `${String(minutes).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
}
