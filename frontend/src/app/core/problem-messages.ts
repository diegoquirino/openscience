/**
 * Texts of the build for the Problem Details of the API, chosen by `type` and filled with the
 * extension values (spec 004, contracts/web.md "Messages on screen"). `title` and `detail` of the
 * server are never shown.
 */

import { HttpErrorResponse } from '@angular/common/http';

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
  column?: string;
  systems?: string[];
  errors?: FieldError[];
  /** `block-incomplete`: 1-based position of the first item without a draft. */
  firstPendingPosition?: number;
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
    case 'csv-not-utf8':
      return $localize`:uploaded file is not UTF-8@@problem.csvNotUtf8:The file is not valid UTF-8 text`;
    case 'csv-invalid':
      return $localize`:uploaded file is not a readable CSV@@problem.csvInvalid:The file could not be read as a CSV`;
    case 'csv-missing-column': {
      const column = problem?.column ?? '';
      return $localize`:CSV missing a required column@@problem.csvMissingColumn:The CSV is missing the required column ${column}:column:`;
    }
    case 'minitest-only': {
      const systems = (problem?.systems ?? []).join(', ');
      return $localize`:non-minitest systems while pilot/staging@@problem.minitestOnly:Only Minitest items are accepted in this phase. Other systems: ${systems}:systems:`;
    }
    case 'preview-not-found':
      return $localize`:import preview expired or unknown@@problem.previewNotFound:This import preview does not exist or has expired; upload the file again`;
    case 'phase-switch-not-allowed':
      return $localize`:cannot switch study phase@@problem.phaseSwitchNotAllowed:The study phase cannot switch to collection now (database not empty, already in collection, or staging)`;
    case 'reservation-expired':
      return $localize`:reservation expired and taken by someone else@@problem.reservationExpired:Your time for this block ran out and it was given to someone else`;
    case 'task-already-submitted':
      return $localize`:save after the block was submitted@@problem.taskAlreadySubmitted:This block was already submitted and can no longer be changed`;
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
      return $localize`:code field of the sign-in form@@field.code.required:Enter the 6-digit code`;
    default:
      return undefined;
  }
}

/** The Problem Details body of a failed call, if the API sent one. */
export function problemOf(error: unknown): Problem | undefined {
  const body: unknown = error instanceof HttpErrorResponse ? error.error : undefined;
  return typeof body === 'object' && body !== null && problemSlug(body as Problem) !== undefined
    ? (body as Problem)
    : undefined;
}

/** For a failure that has no message of its own (network down, unexpected status). */
export function unexpectedErrorMessage(): string {
  return $localize`:failure without a specific message@@problem.unexpected:Something went wrong; please try again`;
}

function minutesUntil(retryAt: string | undefined, now: Date): number {
  const at = retryAt ? Date.parse(retryAt) : NaN;
  if (Number.isNaN(at)) {
    return 60;
  }
  return Math.max(1, Math.ceil((at - now.getTime()) / 60_000));
}
