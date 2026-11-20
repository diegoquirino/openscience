import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable, switchMap, throwError } from 'rxjs';
import { EntryPointService } from './entry-point.service';
import { HalResource, Links } from './hal';

/** The step of a participant (spec 006, contracts/onboarding.md); what the resolver and the guards share. */
export type Step = 'consent' | 'profile' | 'screening' | 'participation' | 'ineligible' | 'not-open';

/** Route of the screen of each step (contracts/web.md, "Step to route map"). */
export const STEP_ROUTES: Record<Step, string> = {
  consent: '/consent',
  profile: '/skill',
  screening: '/screening',
  participation: '/label',
  ineligible: '/screening/closed',
  'not-open': '/not-open',
};

export function routeOfStep(step: Step): string {
  return STEP_ROUTES[step];
}

export interface OnboardingState {
  step: Step;
  eligible: boolean;
  currentConsentFormVersion: string | null;
  links: Links;
}

interface OnboardingBody extends HalResource {
  step: Step;
  eligible: boolean;
  currentConsentFormVersion: string | null;
}

export interface ConsentForm {
  version: string;
  language: string;
  referenceLanguage: string;
  translation: boolean;
  text: string;
  publishedAt: string;
  links: Links;
}

interface ConsentFormBody extends HalResource, Omit<ConsentForm, 'links'> {}

export type ConsentChoice = 'accepted' | 'refused';

export interface ScreeningExample {
  useCase: {
    id: string;
    title: string;
    basicFlow: string[];
    exceptionFlow: { id: string; condition: string; steps: string[] };
  };
  testCase: {
    id: string;
    title: string;
    derivedFrom: string[];
    steps: string[];
    expected: string;
  };
}

/** Raised when the API offers no current consent form (no `vd:consent-form` link). */
export class NoConsentFormError extends Error {
  constructor() {
    super('The study has no current consent form');
  }
}

/**
 * Reads and writes the three onboarding steps by following links from the entry point and the
 * `vd:onboarding` resource; the guards and the resolver read the step on every navigation.
 */
@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private readonly http = inject(HttpClient);
  private readonly entry = inject(EntryPointService);

  /** The step as the server sees it now (never cached: a new consent form takes effect at once). */
  state(): Observable<OnboardingState> {
    return this.entry.load().pipe(
      switchMap((entry) => {
        const href = entry.links.href('onboarding');
        return href ? this.http.get<OnboardingBody>(href) : missing('vd:onboarding');
      }),
      map((body) => ({
        step: body.step,
        eligible: body.eligible,
        currentConsentFormVersion: body.currentConsentFormVersion,
        links: Links.of(body),
      })),
    );
  }

  /** The current consent form in the language of the request. */
  consentForm(): Observable<ConsentForm> {
    return this.state().pipe(
      switchMap((state) => {
        const href = state.links.href('consent-form');
        return href ? this.http.get<ConsentFormBody>(href) : throwError(() => new NoConsentFormError());
      }),
      map(toConsentForm),
    );
  }

  /** The pt-BR reference text of the version of `form`, by its `vd:consent-text` link. */
  referenceText(form: ConsentForm): Observable<ConsentForm> {
    const href = form.links.href('consent-text');
    return href ? this.http.get<ConsentFormBody>(href).pipe(map(toConsentForm)) : missing('vd:consent-text');
  }

  recordConsent(decision: ConsentChoice, consentFormVersion: string): Observable<void> {
    return this.put('consent', { decision, consentFormVersion });
  }

  recordSkill(name: string, skill: string, skillOther: string | null): Observable<void> {
    return this.put(
      'skill',
      skillOther === null ? { name, skill } : { name, skill, skillOther },
    );
  }

  /** Current name and skill (profile menu and resume). */
  loadSkill(): Observable<{ name: string | null; skill: string | null; skillOther: string | null }> {
    return this.state().pipe(
      switchMap((state) => {
        const href = state.links.href('skill');
        return href
          ? this.http.get<{ name?: string | null; skill?: string | null; skillOther?: string | null }>(href)
          : missing('vd:skill');
      }),
      map((body) => ({
        name: body.name ?? null,
        skill: body.skill ?? null,
        skillOther: body.skillOther ?? null,
      })),
    );
  }

  recordScreening(answer: string): Observable<void> {
    return this.put('screening', { answer });
  }

  screeningExample(): Observable<ScreeningExample> {
    return this.state().pipe(
      switchMap((state) => {
        const href = state.links.href('screening-example');
        return href ? this.http.get<ScreeningExample>(href) : missing('vd:screening-example');
      }),
    );
  }

  private put(rel: 'consent' | 'skill' | 'screening', body: object): Observable<void> {
    return this.state().pipe(
      switchMap((state) => {
        const href = state.links.href(rel);
        return href ? this.http.put<unknown>(href, body) : missing(`vd:${rel}`);
      }),
      map(() => undefined),
    );
  }
}

function toConsentForm(body: ConsentFormBody): ConsentForm {
  return {
    version: body.version,
    language: body.language,
    referenceLanguage: body.referenceLanguage,
    translation: body.translation,
    text: body.text,
    publishedAt: body.publishedAt,
    links: Links.of(body),
  };
}

function missing(rel: string): Observable<never> {
  return throwError(() => new Error(`The API did not offer ${rel}`));
}
