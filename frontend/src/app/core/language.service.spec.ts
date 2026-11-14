import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, inject } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Links } from './hal';
import { LANGUAGE_COOKIE, LanguageService, LOAD_PAGE } from './language.service';
import { Session, SessionService } from './session.service';

@Component({ template: '' })
class BlankPage {}

@Component({ selector: 'app-form-page', template: 'form' })
class FormPage {
  constructor() {
    inject(LanguageService).keepOnSwitch('/participate', () => ({ name: 'Ana', email: 'ana@example.com' }));
  }
}

describe('LanguageService', () => {
  let load: ReturnType<typeof vi.fn>;
  let http: HttpTestingController;
  let cookies: string[];

  beforeEach(() => {
    load = vi.fn();
    cookies = [];
    vi.spyOn(document, 'cookie', 'set').mockImplementation((value: string) => {
      cookies.push(value);
    });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'participate', component: FormPage },
          { path: '**', component: BlankPage },
        ]),
        { provide: LOAD_PAGE, useValue: load },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    sessionStorage.clear();
  });

  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
  });

  function signedIn(): void {
    const session: Session = {
      subject: 'participant',
      name: 'Ana',
      pseudonym: 'P-1',
      preferredLanguage: 'en-US',
      expiresAt: '2026-11-13T12:00:00Z',
      links: new Links({ 'vd:me': { href: '/api/v1/participants/me' } }),
    };
    TestBed.inject(SessionService).session.set(session);
  }

  it('writes the language cookie for one year on the whole site', () => {
    TestBed.inject(LanguageService).setCookie('es-ES');
    expect(cookies).toEqual([`${LANGUAGE_COOKIE}=es-ES; Path=/; Max-Age=31536000; SameSite=Lax`]);
  });

  it('opens the same route under the new prefix for a visitor, without calling the API', async () => {
    await TestBed.inject(Router).navigateByUrl('/welcome?x=1');
    TestBed.inject(LanguageService).switchTo('es-ES').subscribe();
    expect(cookies).toEqual([expect.stringContaining('veredito-lang=es-ES')]);
    expect(load).toHaveBeenCalledWith('/es-ES/welcome?x=1');
  });

  it('saves the profile language before switching when signed in', async () => {
    signedIn();
    await TestBed.inject(Router).navigateByUrl('/welcome');
    TestBed.inject(LanguageService).switchTo('pt-BR').subscribe();
    const request = http.expectOne('/api/v1/participants/me');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ preferredLanguage: 'pt-BR' });
    expect(load).not.toHaveBeenCalled();
    request.flush({});
    expect(load).toHaveBeenCalledWith('/pt-BR/welcome');
  });

  it('still switches when the profile update fails', async () => {
    signedIn();
    await TestBed.inject(Router).navigateByUrl('/welcome');
    TestBed.inject(LanguageService).switchTo('pt-BR').subscribe();
    http.expectOne('/api/v1/participants/me').flush(null, { status: 500, statusText: 'Server Error' });
    expect(load).toHaveBeenCalledWith('/pt-BR/welcome');
  });

  it('keeps the values of the open form across the switch, once', async () => {
    const harness = await RouterTestingHarness.create();
    const language = TestBed.inject(LanguageService);
    await harness.navigateByUrl('/participate');
    language.switchTo('es-ES').subscribe();
    expect(language.takeUnsaved('/participate')).toEqual({ name: 'Ana', email: 'ana@example.com' });
    expect(language.takeUnsaved('/participate')).toBeUndefined();
  });

  it('forgets the form once it is destroyed', async () => {
    const harness = await RouterTestingHarness.create();
    const language = TestBed.inject(LanguageService);
    await harness.navigateByUrl('/participate');
    await harness.navigateByUrl('/welcome');
    language.switchTo('es-ES').subscribe();
    expect(language.takeUnsaved('/participate')).toBeUndefined();
  });

  it('ignores unreadable saved values', () => {
    sessionStorage.setItem('veredito-unsaved:/participate', '{not json');
    expect(TestBed.inject(LanguageService).takeUnsaved('/participate')).toBeUndefined();
  });
});
