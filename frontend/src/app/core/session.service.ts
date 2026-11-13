import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of, switchMap, tap, throwError } from 'rxjs';
import { ENTRY_POINT_URL } from './entry-point.service';
import { HalResource, Links } from './hal';

export type Subject = 'visitor' | 'participant' | 'administrator';

export interface Session {
  subject: Exclude<Subject, 'visitor'>;
  name: string | null;
  pseudonym: string | null;
  preferredLanguage: string | null;
  expiresAt: string;
  links: Links;
}

export interface CodeRequested {
  email: string;
  validityMinutes: number;
  resendAfterSeconds: number;
}

interface SessionBody extends HalResource {
  subject: Session['subject'];
  name?: string | null;
  pseudonym?: string | null;
  preferredLanguage?: string | null;
  expiresAt: string;
}

interface CodeRequestedBody extends HalResource, CodeRequested {}

/**
 * Who is signed in, read by following `vd:session` from the entry point. Every address comes from
 * a link; the only address the client knows is the entry point.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly http = inject(HttpClient);

  readonly session = signal<Session | null>(null);
  readonly subject = computed<Subject>(() => this.session()?.subject ?? 'visitor');

  private signInHref: string | undefined;

  /** Reads the entry point and, when it offers `vd:session`, the current session. */
  refresh(): Observable<Session | null> {
    return this.entryLinks().pipe(
      switchMap((links) => {
        const href = links.href('session');
        return href ? this.http.get<SessionBody>(href).pipe(map(toSession)) : of(null);
      }),
      catchError(() => of(null)),
      tap((session) => this.session.set(session)),
    );
  }

  /** Follows `vd:request-code`; keeps the `vd:sign-in` link of the answer for {@link signIn}. */
  requestCode(name: string, email: string): Observable<CodeRequested> {
    return this.entryLinks().pipe(
      switchMap((links) => {
        const href = links.href('request-code');
        return href ? this.http.post<CodeRequestedBody>(href, { name, email }) : missing('vd:request-code');
      }),
      tap((body) => (this.signInHref = Links.of(body).href('sign-in'))),
      map(({ email: normalized, validityMinutes, resendAfterSeconds }) => ({
        email: normalized,
        validityMinutes,
        resendAfterSeconds,
      })),
    );
  }

  /** Follows `vd:sign-in` with the typed code; the server sets the session cookie. */
  signIn(email: string, code: string): Observable<Session> {
    if (!this.signInHref) {
      return missing('vd:sign-in');
    }
    return this.http.post<SessionBody>(this.signInHref, { email, code }).pipe(
      map(toSession),
      tap((session) => this.session.set(session)),
    );
  }

  /** Follows `vd:sign-out` of the session; the session is gone locally even if the call fails. */
  signOut(): Observable<void> {
    const href = this.session()?.links.href('sign-out');
    this.session.set(null);
    if (!href) {
      return of(undefined);
    }
    return this.http.delete<void>(href).pipe(
      map(() => undefined),
      catchError(() => of(undefined)),
    );
  }

  private entryLinks(): Observable<Links> {
    return this.http.get<HalResource>(ENTRY_POINT_URL).pipe(map((body) => Links.of(body)));
  }
}

function toSession(body: SessionBody): Session {
  return {
    subject: body.subject,
    name: body.name ?? null,
    pseudonym: body.pseudonym ?? null,
    preferredLanguage: body.preferredLanguage ?? null,
    expiresAt: body.expiresAt,
    links: Links.of(body),
  };
}

function missing(rel: string): Observable<never> {
  return throwError(() => new Error(`The API did not offer ${rel}`));
}
