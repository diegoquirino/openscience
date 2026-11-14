import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { SessionService, Subject } from './session.service';

/** Where each kind of person belongs: the sign-in form, the participant area or the administration. */
export function areaOf(subject: Subject): string {
  switch (subject) {
    case 'participant':
      return '/welcome';
    case 'administrator':
      return '/admin';
    default:
      return '/participate';
  }
}

/** Lets `subject` in and sends anyone else to their own area (contracts/web.md, "Guards"). */
function only(subject: Subject): CanActivateFn {
  return () => {
    const session = inject(SessionService);
    const router = inject(Router);
    return session.current().pipe(
      map(() => (session.subject() === subject ? true : router.parseUrl(areaOf(session.subject())))),
    );
  };
}

export const visitorOnly = only('visitor');
export const participantOnly = only('participant');
export const administratorOnly = only('administrator');
