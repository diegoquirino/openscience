/**
 * The code requested in this tab, kept in `sessionStorage` so the code screen survives a reload and
 * "Correct e-mail" can fill the form again.
 */

export const PENDING_CODE_KEY = 'veredito-pending-code';

export interface PendingCode {
  name: string;
  /** The e-mail as typed, for the form. */
  typedEmail: string;
  /** The e-mail as the API normalized it, shown on the code screen and sent with the code. */
  email: string;
  validityMinutes: number;
  /** When "Resend" becomes available, in milliseconds since the epoch. */
  resendAt: number;
  signInHref: string;
}

export function readPendingCode(): PendingCode | null {
  try {
    const value: unknown = JSON.parse(sessionStorage.getItem(PENDING_CODE_KEY) ?? 'null');
    return isPendingCode(value) ? value : null;
  } catch {
    return null;
  }
}

export function savePendingCode(pending: PendingCode): void {
  sessionStorage.setItem(PENDING_CODE_KEY, JSON.stringify(pending));
}

export function clearPendingCode(): void {
  sessionStorage.removeItem(PENDING_CODE_KEY);
}

function isPendingCode(value: unknown): value is PendingCode {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const entry = value as Record<string, unknown>;
  return (
    ['name', 'typedEmail', 'email', 'signInHref'].every((key) => typeof entry[key] === 'string') &&
    ['validityMinutes', 'resendAt'].every((key) => typeof entry[key] === 'number')
  );
}
