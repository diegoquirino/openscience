import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { onboardingStep, participationOnly } from './onboarding-guards';
import { routeOfStep, Step, STEP_ROUTES } from './onboarding.service';

@Component({ template: '' })
class BlankPage {}

const CURIES = [{ name: 'vd', href: '/api/v1/rels/{rel}', templated: true }];

describe('step to route map', () => {
  it('has one distinct route per step', () => {
    const steps: Step[] = ['consent', 'skill', 'screening', 'participation', 'ineligible', 'not-open'];
    expect(steps.map(routeOfStep)).toEqual([
      '/consent',
      '/skill',
      '/screening',
      '/label',
      '/screening/closed',
      '/not-open',
    ]);
    expect(new Set(Object.values(STEP_ROUTES)).size).toBe(steps.length);
  });
});

describe('onboarding guards', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'consent', canActivate: [onboardingStep('consent')], component: BlankPage },
          { path: 'skill', canActivate: [onboardingStep('skill')], component: BlankPage },
          { path: 'screening', canActivate: [onboardingStep('screening')], component: BlankPage },
          { path: 'screening/closed', canActivate: [onboardingStep('ineligible')], component: BlankPage },
          { path: 'not-open', canActivate: [onboardingStep('not-open')], component: BlankPage },
          { path: 'label', canActivate: [participationOnly], component: BlankPage },
        ]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  /**
   * Opens `url` while the server says the participant is at `step`. A redirect opens another route
   * whose guard reads the step again, so every request is answered until the navigation ends.
   */
  async function open(url: string, step: Step): Promise<string> {
    let done = false;
    const navigation = router.navigateByUrl(url).finally(() => (done = true));
    while (!done) {
      await new Promise((resolve) => setTimeout(resolve));
      for (const request of http.match('/api/v1')) {
        request.flush({
          _links: { curies: CURIES, 'vd:onboarding': { href: '/api/v1/participants/me/onboarding' } },
        });
      }
      for (const request of http.match('/api/v1/participants/me/onboarding')) {
        request.flush({
          step,
          eligible: step === 'participation',
          currentConsentFormVersion: 'v1',
          _links: { curies: CURIES },
        });
      }
    }
    await navigation;
    return router.url;
  }
  it.each([
    ['/consent', 'consent'],
    ['/skill', 'skill'],
    ['/screening', 'screening'],
    ['/screening/closed', 'ineligible'],
    ['/not-open', 'not-open'],
    ['/label', 'participation'],
  ] as const)('lets the participant into %s at step %s', async (url, step) => {
    expect(await open(url, step)).toBe(url);
  });

  it.each([
    ['/label', 'consent', '/consent'],
    ['/label', 'skill', '/skill'],
    ['/label', 'screening', '/screening'],
    ['/label', 'ineligible', '/screening/closed'],
    ['/label', 'not-open', '/not-open'],
    ['/consent', 'skill', '/skill'],
    ['/skill', 'participation', '/label'],
    ['/screening', 'ineligible', '/screening/closed'],
  ] as const)('sends the participant from %s at step %s to %s', async (url, step, expected) => {
    expect(await open(url, step)).toBe(expected);
  });

  it('lets the navigation proceed when the step cannot be read', async () => {
    const navigation = router.navigateByUrl('/skill');
    await new Promise((resolve) => setTimeout(resolve));
    http.expectOne('/api/v1').flush({ _links: { curies: CURIES } });
    await navigation;
    expect(router.url).toBe('/skill');
  });
});