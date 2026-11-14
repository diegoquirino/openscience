import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { LOAD_PAGE } from '../core/language.service';
import { SessionService } from '../core/session.service';
import { CodeComponent } from './code.component';
import { PendingCode, readPendingCode, savePendingCode } from './pending-code';

@Component({ template: '' })
class BlankPage {}

const CURIES = [{ name: 'vd', href: '/api/v1/rels/{rel}', templated: true }];

const SESSION = {
  subject: 'participant',
  name: 'Ana Souza',
  pseudonym: 'R-0001',
  preferredLanguage: 'en-US',
  expiresAt: '2026-11-14T17:03:00Z',
  _links: { self: { href: '/links/session' }, 'vd:sign-out': { href: '/links/session' } },
};

const problem = (slug: string, extensions: object = {}) => ({ type: `urn:veredito:problem:${slug}`, ...extensions });

function pendingCode(resendInSeconds: number): PendingCode {
  return {
    name: 'Ana Souza',
    typedEmail: 'Ana@Example.com',
    email: 'ana@example.com',
    validityMinutes: 10,
    resendAt: Date.now() + resendInSeconds * 1000,
    signInHref: '/links/sessions',
  };
}

describe('CodeComponent', () => {
  let http: HttpTestingController;
  let load: ReturnType<typeof vi.fn>;
  let cookies: string[];

  beforeEach(async () => {
    sessionStorage.clear();
    load = vi.fn();
    cookies = [];
    vi.spyOn(document, 'cookie', 'set').mockImplementation((value: string) => {
      cookies.push(value);
    });
    await TestBed.configureTestingModule({
      imports: [CodeComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'participate', component: BlankPage },
          { path: 'welcome', component: BlankPage },
          { path: 'admin', component: BlankPage },
        ]),
        { provide: LOAD_PAGE, useValue: load },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
  });

  async function render(): Promise<ComponentFixture<CodeComponent>> {
    const fixture = TestBed.createComponent(CodeComponent);
    await fixture.whenStable();
    return fixture;
  }

  function element(fixture: ComponentFixture<CodeComponent>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function buttons(fixture: ComponentFixture<CodeComponent>): HTMLButtonElement[] {
    return Array.from(element(fixture).querySelectorAll<HTMLButtonElement>('button'));
  }

  async function signInWith(fixture: ComponentFixture<CodeComponent>, code: string): Promise<void> {
    const input = element(fixture).querySelector<HTMLInputElement>('input')!;
    input.value = code;
    input.dispatchEvent(new Event('input'));
    element(fixture).querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('sends a visitor without a pending code back to the form', async () => {
    await render();
    expect(TestBed.inject(Router).url).toBe('/participate');
  });

  it('shows the e-mail, the validity and the spam hint, with "Resend" counting down', async () => {
    savePendingCode(pendingCode(60));

    const fixture = await render();

    const text = element(fixture).textContent!;
    expect(text).toContain('ana@example.com');
    expect(text).toContain('valid for 10 minutes');
    expect(text).toContain('spam');
    const resend = buttons(fixture)[1];
    expect(resend.disabled).toBe(true);
    expect(resend.textContent!.trim()).toMatch(/^Resend in (59|60) s$/);
  });

  it('signs in through vd:sign-in and opens the welcome screen', async () => {
    savePendingCode(pendingCode(60));
    const fixture = await render();

    await signInWith(fixture, '123 456');
    const signIn = http.expectOne('/links/sessions');
    expect(signIn.request.method).toBe('POST');
    expect(signIn.request.body).toEqual({ email: 'ana@example.com', code: '123 456' });
    signIn.flush(SESSION, { status: 201, statusText: 'Created' });
    await fixture.whenStable();

    expect(TestBed.inject(Router).url).toBe('/welcome');
    expect(TestBed.inject(SessionService).subject()).toBe('participant');
    expect(readPendingCode()).toBeNull();
  });

  it('opens the administration for the administrator', async () => {
    savePendingCode(pendingCode(60));
    const fixture = await render();

    await signInWith(fixture, '123 456');
    http
      .expectOne('/links/sessions')
      .flush({ ...SESSION, subject: 'administrator', pseudonym: undefined }, { status: 201, statusText: 'Created' });
    await fixture.whenStable();

    expect(TestBed.inject(Router).url).toBe('/admin');
  });

  it('opens the area in the language of the profile when it differs from the build', async () => {
    savePendingCode(pendingCode(60));
    const fixture = await render();

    await signInWith(fixture, '123 456');
    http
      .expectOne('/links/sessions')
      .flush({ ...SESSION, preferredLanguage: 'es-ES' }, { status: 201, statusText: 'Created' });
    await fixture.whenStable();

    expect(cookies).toEqual([expect.stringContaining('veredito-lang=es-ES')]);
    expect(load).toHaveBeenCalledWith('/es-ES/welcome');
    expect(TestBed.inject(Router).url).toBe('/');
  });

  it('stays in the build language when the profile has none', async () => {
    savePendingCode(pendingCode(60));
    const fixture = await render();

    await signInWith(fixture, '123 456');
    http
      .expectOne('/links/sessions')
      .flush({ ...SESSION, preferredLanguage: null }, { status: 201, statusText: 'Created' });
    await fixture.whenStable();

    expect(load).not.toHaveBeenCalled();
    expect(TestBed.inject(Router).url).toBe('/welcome');
  });

  it('does not send a code of the wrong shape', async () => {
    savePendingCode(pendingCode(60));
    const fixture = await render();

    await signInWith(fixture, '12345');

    expect(element(fixture).querySelector('mat-error')?.textContent?.trim()).toBe('Enter the 6-digit code');
    http.expectNone('/links/sessions');
  });

  it('shows the attempts left after a wrong code', async () => {
    savePendingCode(pendingCode(60));
    const fixture = await render();

    await signInWith(fixture, '654321');
    http.expectOne('/links/sessions').flush(problem('invalid-code', { attemptsLeft: 3 }), {
      status: 422,
      statusText: 'Unprocessable Content',
    });
    await fixture.whenStable();

    expect(element(fixture).querySelector('[role="alert"]')?.textContent?.trim()).toBe('Wrong code; 3 attempts left');
  });

  it('offers a new code when the code is no longer valid', async () => {
    savePendingCode(pendingCode(-1));
    const fixture = await render();

    await signInWith(fixture, '654321');
    http.expectOne('/links/sessions').flush(problem('code-expired'), {
      status: 422,
      statusText: 'Unprocessable Content',
    });
    await fixture.whenStable();

    expect(element(fixture).querySelector('[role="alert"]')?.textContent?.trim()).toBe('This code is no longer valid');
    const sendNew = buttons(fixture)[1];
    expect(sendNew.textContent!.trim()).toBe('Send a new code');
    expect(sendNew.disabled).toBe(false);
  });

  it('resends the code with the typed name and e-mail and restarts the countdown', async () => {
    savePendingCode(pendingCode(-1));
    const fixture = await render();

    buttons(fixture)[1].click();
    http.expectOne('/api/v1').flush({
      _links: { self: { href: '/api/v1' }, curies: CURIES, 'vd:request-code': { href: '/links/codes' } },
    });
    const request = http.expectOne('/links/codes');
    expect(request.request.body).toEqual({ name: 'Ana Souza', email: 'Ana@Example.com' });
    request.flush({
      email: 'ana@example.com',
      validityMinutes: 10,
      resendAfterSeconds: 60,
      _links: { 'vd:sign-in': { href: '/links/sessions-2' } },
    });
    await fixture.whenStable();

    expect(element(fixture).querySelector('[role="status"]')).not.toBeNull();
    expect(buttons(fixture)[1].disabled).toBe(true);
    expect(readPendingCode()?.signInHref).toBe('/links/sessions-2');
  });

  it('goes back to the form to correct the e-mail', async () => {
    savePendingCode(pendingCode(60));
    const fixture = await render();

    buttons(fixture)[2].click();
    await fixture.whenStable();

    expect(TestBed.inject(Router).url).toBe('/participate');
    expect(readPendingCode()?.typedEmail).toBe('Ana@Example.com');
  });
});
