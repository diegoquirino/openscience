import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { administratorOnly, areaOf, participantOnly, visitorOnly } from './guards';
import { Subject } from './session.service';

@Component({ template: '' })
class BlankPage {}

const CURIES = [{ name: 'vd', href: '/api/v1/rels/{rel}', templated: true }];

describe('guards', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'participate', canActivate: [visitorOnly], component: BlankPage },
          { path: 'welcome', canActivate: [participantOnly], component: BlankPage },
          { path: 'admin', canActivate: [administratorOnly], component: BlankPage },
        ]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  /** Opens `url` as `subject`, answering the entry point and `vd:session` the guard reads. */
  async function open(url: string, subject: Subject): Promise<string> {
    const navigation = router.navigateByUrl(url);
    await new Promise((resolve) => setTimeout(resolve));
    const entry = http.expectOne('/api/v1');
    if (subject === 'visitor') {
      entry.flush({ _links: { curies: CURIES, 'vd:request-code': { href: '/links/codes' } } });
    } else {
      entry.flush({ _links: { curies: CURIES, 'vd:session': { href: '/links/session' } } });
      http.expectOne('/links/session').flush({ subject, name: 'Someone', expiresAt: '2026-11-14T17:03:00Z' });
    }
    await navigation;
    return router.url;
  }

  it('maps each kind of person to an area', () => {
    expect(areaOf('visitor')).toBe('/participate');
    expect(areaOf('participant')).toBe('/welcome');
    expect(areaOf('administrator')).toBe('/admin');
  });

  it('sends a visitor from the areas to the sign-in form', async () => {
    expect(await open('/welcome', 'visitor')).toBe('/participate');
  });

  it('sends a visitor from the administration to the sign-in form', async () => {
    expect(await open('/admin', 'visitor')).toBe('/participate');
  });

  it('sends a participant from the administration to the welcome screen', async () => {
    expect(await open('/admin', 'participant')).toBe('/welcome');
  });

  it('sends the administrator from the participant area to the administration', async () => {
    expect(await open('/welcome', 'administrator')).toBe('/admin');
  });

  it('sends a signed-in person from the sign-in form to their own area', async () => {
    expect(await open('/participate', 'administrator')).toBe('/admin');
  });

  it('lets each person into their own area', async () => {
    expect(await open('/admin', 'administrator')).toBe('/admin');
    await router.navigateByUrl('/participate');
    expect(router.url).toBe('/admin');
  });
});
