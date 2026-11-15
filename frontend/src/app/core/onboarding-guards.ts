import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { OnboardingService, routeOfStep, Step } from './onboarding.service';

/**
 * Lets the participant in only while `step` is the real pending step; otherwise sends them to the
 * screen of the real step. The step is read on every navigation, so the Back button and typed
 * addresses are harmless (spec 006, contracts/web.md "Guards"). Use after `participantOnly`.
 * When the step cannot be read the navigation proceeds: the API enforces the gate anyway.
 */
export function onboardingStep(step: Step): CanActivateFn {
  return () => {
    const router = inject(Router);
    return inject(OnboardingService)
      .state()
      .pipe(
        map((state) => (state.step === step ? true : router.parseUrl(routeOfStep(state.step)))),
        catchError(() => of(true)),
      );
  };
}

export const participationOnly = onboardingStep('participation');
