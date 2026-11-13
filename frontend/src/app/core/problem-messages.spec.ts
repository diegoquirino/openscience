import { clearTranslations, loadTranslations } from '@angular/localize';
import { fieldMessage, problemMessage, problemSlug } from './problem-messages';

const type = (slug: string) => `urn:veredito:problem:${slug}`;

describe('problem messages', () => {
  afterEach(() => clearTranslations());

  it('reads the slug of a Veredito problem only', () => {
    expect(problemSlug({ type: type('invalid-code') })).toBe('invalid-code');
    expect(problemSlug({ type: 'about:blank' })).toBeUndefined();
    expect(problemSlug(null)).toBeUndefined();
  });

  it('fills the source texts with the extension values', () => {
    const now = new Date('2026-11-13T12:00:00Z');

    expect(problemMessage({ type: type('code-resend-too-soon'), retryAfterSeconds: 42 })).toBe(
      'Wait 42 s to request a new code',
    );
    expect(problemMessage({ type: type('code-hourly-limit'), retryAt: '2026-11-13T12:17:30Z' }, now)).toBe(
      'Too many codes for this e-mail; try again in about 18 minutes',
    );
    expect(problemMessage({ type: type('invalid-code'), attemptsLeft: 3 })).toBe('Wrong code; 3 attempts left');
    expect(problemMessage({ type: type('daily-email-limit') })).toBe(
      'We cannot send more codes today; please try again tomorrow',
    );
    expect(problemMessage({ type: type('code-not-sent') })).toBe('The code could not be sent right now; try again');
    expect(problemMessage({ type: type('code-expired') })).toBe('This code is no longer valid');
  });

  it('never rounds the waiting time down to zero', () => {
    const now = new Date('2026-11-13T12:00:00Z');
    expect(problemMessage({ type: type('code-hourly-limit'), retryAt: '2026-11-13T12:00:05Z' }, now)).toContain(
      'about 1 minutes',
    );
  });

  it('has no message for the types that lead to another screen, nor for unknown types', () => {
    expect(problemMessage({ type: type('forbidden') })).toBeUndefined();
    expect(problemMessage({ type: type('authentication-required') })).toBeUndefined();
    expect(problemMessage({ type: type('something-new') })).toBeUndefined();
  });

  it('uses the translations of the build, with the same numbers', () => {
    loadTranslations({
      'problem.codeResendTooSoon': 'Aguarde {$seconds} s para pedir um novo código',
      'problem.invalidCode': 'Código incorreto; restam {$attemptsLeft} tentativas',
    });

    expect(problemMessage({ type: type('code-resend-too-soon'), retryAfterSeconds: 9 })).toBe(
      'Aguarde 9 s para pedir um novo código',
    );
    expect(problemMessage({ type: type('invalid-code'), attemptsLeft: 2 })).toBe('Código incorreto; restam 2 tentativas');
  });

  it('maps the field errors of invalid-request', () => {
    expect(fieldMessage({ field: 'name', code: 'size' })).toBe('Enter your name (2 to 120 characters)');
    expect(fieldMessage({ field: 'email', code: 'email' })).toBe('Enter a valid e-mail');
    expect(fieldMessage({ field: 'code', code: 'required' })).toBe('Enter the code');
    expect(fieldMessage({ field: 'other', code: 'required' })).toBeUndefined();
  });
});
