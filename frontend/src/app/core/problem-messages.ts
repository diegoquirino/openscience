/**
 * Texts of the build for the Problem Details of the API, chosen by `type` and filled with the
 * extension values (spec 004, contracts/web.md "Messages on screen"). `title` and `detail` of the
 * server are never shown.
 */

export const PROBLEM_TYPE_PREFIX = 'urn:veredito:problem:';

export interface FieldError {
  field: string;
  code: string;
}

export interface Problem {
  type?: string;
  status?: number;
  retryAfterSeconds?: number;
  retryAt?: string;
  attemptsLeft?: number;
  errors?: FieldError[];
}

/** The slug of the type (`invalid-code`), or `undefined` for a body that is not a Veredito problem. */
export function problemSlug(problem: Problem | null | undefined): string | undefined {
  const type = problem?.type;
  return type?.startsWith(PROBLEM_TYPE_PREFIX) ? type.substring(PROBLEM_TYPE_PREFIX.length) : undefined;
}

/**
 * The message for the person; `undefined` when the type has no message (`forbidden` and
 * `authentication-required` lead to another screen) or is unknown.
 */
export function problemMessage(problem: Problem | null | undefined, now: Date = new Date()): string | undefined {
  switch (problemSlug(problem)) {
    case 'code-resend-too-soon': {
      const seconds = Math.max(1, Math.ceil(problem?.retryAfterSeconds ?? 60));
      return $localize`:wait before a new code, seconds left@@problem.codeResendTooSoon:Wait ${seconds}:seconds: s to request a new code`;
    }
    case 'code-hourly-limit': {
      const minutes = minutesUntil(problem?.retryAt, now);
      return $localize`:hourly code limit, minutes until the next request@@problem.codeHourlyLimit:Too many codes for this e-mail; try again in about ${minutes}:minutes: minutes`;
    }
    case 'daily-email-limit':
      return $localize`:daily e-mail limit of the whole system@@problem.dailyEmailLimit:We cannot send more codes today; please try again tomorrow`;
    case 'code-not-sent':
      return $localize`:SMTP failure when sending the code@@problem.codeNotSent:The code could not be sent right now; try again`;
    case 'invalid-code': {
      const attemptsLeft = problem?.attemptsLeft ?? 0;
      return $localize`:wrong access code, attempts left@@problem.invalidCode:Wrong code; ${attemptsLeft}:attemptsLeft: attempts left`;
    }
    case 'code-expired':
      return $localize`:expired, used or replaced access code@@problem.codeExpired:This code is no longer valid`;
    default:
      return undefined;
  }
}

/** The message of a field error of `invalid-request`; `undefined` for an unknown field. */
export function fieldMessage(error: FieldError): string | undefined {
  switch (error.field) {
    case 'name':
      return $localize`:name field of the sign-in form@@field.name.invalid:Enter your name (2 to 120 characters)`;
    case 'email':
      return $localize`:e-mail field of the sign-in form@@field.email.invalid:Enter a valid e-mail`;
    case 'code':
      return $localize`:code field of the sign-in form@@field.code.required:Enter the code`;
    default:
      return undefined;
  }
}

function minutesUntil(retryAt: string | undefined, now: Date): number {
  const at = retryAt ? Date.parse(retryAt) : NaN;
  if (Number.isNaN(at)) {
    return 60;
  }
  return Math.max(1, Math.ceil((at - now.getTime()) / 60_000));
}
