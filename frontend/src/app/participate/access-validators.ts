import { AbstractControl, ValidationErrors } from '@angular/forms';

/**
 * The field rules of the API (domain classes `PersonName`, `EmailAddress` and `AccessCodeFormat`),
 * checked before the request so a typo costs no round trip; the API checks them again.
 */

export const NAME_MIN_LENGTH = 2;
export const NAME_MAX_LENGTH = 120;
export const EMAIL_MAX_LENGTH = 254;

export function nameValidator(control: AbstractControl<string>): ValidationErrors | null {
  const name = (control.value ?? '').trim();
  if (name === '') {
    return { required: true };
  }
  return name.length < NAME_MIN_LENGTH || name.length > NAME_MAX_LENGTH ? { size: true } : null;
}

export function emailValidator(control: AbstractControl<string>): ValidationErrors | null {
  const email = (control.value ?? '').trim().toLowerCase();
  if (email === '') {
    return { required: true };
  }
  return isEmail(email) ? null : { email: true };
}

/** The 6 digits of a typed code, ignoring spaces and hyphens. */
export function codeValidator(control: AbstractControl<string>): ValidationErrors | null {
  const code = (control.value ?? '').replace(/[\s-]/g, '');
  if (code === '') {
    return { required: true };
  }
  return /^[0-9]{6}$/.test(code) ? null : { code: true };
}

function isEmail(email: string): boolean {
  if (email.length > EMAIL_MAX_LENGTH || /\s/.test(email)) {
    return false;
  }
  const at = email.indexOf('@');
  if (at <= 0 || at !== email.lastIndexOf('@')) {
    return false;
  }
  const domain = email.substring(at + 1);
  return domain.indexOf('.') > 0 && !domain.endsWith('.') && !domain.includes('..');
}
