import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { LanguageService, LOAD_PAGE } from '../core/language.service';
import { PendingCode, readPendingCode, savePendingCode } from './pending-code';
import { SignInComponent } from './sign-in.component';

@Component({ template: '' })
class BlankPage {}

const VISITOR_ENTRY = {
  _links: {
    self: { href: '/api/v1' },
    curies: [{ name: 'vd', href: '/api/v1/rels/{rel}', templated: true }],
    'vd:request-code': { href: '/links/codes' },
  },
};

const problem = (slug: string, extensions: object = {}) => ({ type: `urn:veredito:problem:${slug}`, ...extensions });

describe('SignInComponent', () => {
  let http: HttpTestingController;
  let load: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    sessionStorage.clear();
    load = vi.fn();
    vi.spyOn(document, 'cookie', 'set').mockImplementation(() => undefined);
    await TestBed.configureTestingModule({
      imports: [SignInComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'participate/code', component: BlankPage },
          { path: 'participate', component: BlankPage },
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

  async function render(): Promise<ComponentFixture<SignInComponent>> {
    const fixture = TestBed.createComponent(SignInComponent);
    await fixture.whenStable();
    return fixture;
  }

  function element(fixture: ComponentFixture<SignInComponent>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function type(fixture: ComponentFixture<SignInComponent>, control: string, value: string): void {
    const input = element(fixture).querySelector<HTMLInputElement>(`input[formcontrolname="${control}"]`)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function submit(fixture: ComponentFixture<SignInComponent>): Promise<void> {
    element(fixture).querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  function errors(fixture: ComponentFixture<SignInComponent>): string[] {
    return Array.from(element(fixture).querySelectorAll('mat-error')).map((error) => error.textContent!.trim());
  }

  async function answerTheCodeRequest(body: object, status = 200, statusText = 'OK'): Promise<void> {
    http.expectOne('/api/v1').flush(VISITOR_ENTRY);
    http.expectOne('/links/codes').flush(body, { status, statusText });
  }

  it('marks the fields with a problem and sends nothing', async () => {
    const fixture = await render();
    type(fixture, 'name', ' A ');
    type(fixture, 'email', 'ana@');

    await submit(fixture);

    expect(errors(fixture)).toEqual(['Enter your name (2 to 120 characters)', 'Enter a valid e-mail']);
    http.expectNone('/api/v1');
  });

  it('sends the code and opens the code screen with the pending code saved', async () => {
    const fixture = await render();
    type(fixture, 'name', 'Ana Souza');
    type(fixture, 'email', 'Ana@Example.com');

    await submit(fixture);
    await answerTheCodeRequest({
      email: 'ana@example.com',
      validityMinutes: 10,
      resendAfterSeconds: 60,
      _links: { 'vd:sign-in': { href: '/links/sessions' } },
    });
    await fixture.whenStable();

    expect(TestBed.inject(Router).url).toBe('/participate/code');
    expect(readPendingCode()).toMatchObject({
      name: 'Ana Souza',
      typedEmail: 'Ana@Example.com',
      email: 'ana@example.com',
      validityMinutes: 10,
      signInHref: '/links/sessions',
    });
  });

  it('shows the message of a refusal in the language of the build', async () => {
    const fixture = await render();
    type(fixture, 'name', 'Ana Souza');
    type(fixture, 'email', 'ana@example.com');

    await submit(fixture);
    await answerTheCodeRequest(problem('code-resend-too-soon', { retryAfterSeconds: 42 }), 429, 'Too Many Requests');
    await fixture.whenStable();

    expect(element(fixture).querySelector('[role="alert"]')?.textContent?.trim()).toBe('Wait 42 s to request a new code');
    expect(TestBed.inject(Router).url).toBe('/');
  });

  it('marks the field that the API refused', async () => {
    const fixture = await render();
    type(fixture, 'name', 'Ana Souza');
    type(fixture, 'email', 'ana@example.com');

    await submit(fixture);
    await answerTheCodeRequest(
      problem('invalid-request', { errors: [{ field: 'email', code: 'email' }] }),
      400,
      'Bad Request',
    );
    await fixture.whenStable();

    expect(errors(fixture)).toEqual(['Enter a valid e-mail']);
  });

  it('fills the form again from the pending code ("Correct e-mail")', async () => {
    const pending: PendingCode = {
      name: 'Ana Souza',
      typedEmail: 'ana@exmple.com',
      email: 'ana@exmple.com',
      validityMinutes: 10,
      resendAt: Date.now(),
      signInHref: '/links/sessions',
    };
    savePendingCode(pending);

    const fixture = await render();

    const value = (control: string) =>
      element(fixture).querySelector<HTMLInputElement>(`input[formcontrolname="${control}"]`)!.value;
    expect(value('name')).toBe('Ana Souza');
    expect(value('email')).toBe('ana@exmple.com');
  });

  it('keeps the typed values across a language switch, once', async () => {
    const fixture = await render();
    type(fixture, 'name', 'Ana Souza');
    type(fixture, 'email', 'ana@example.com');
    await TestBed.inject(Router).navigateByUrl('/participate');
    TestBed.inject(LanguageService).switchTo('es-ES').subscribe();
    expect(load).toHaveBeenCalledWith('/es-ES/participate');
    fixture.destroy();

    const reloaded = await render();
    const value = (control: string) =>
      element(reloaded).querySelector<HTMLInputElement>(`input[formcontrolname="${control}"]`)!.value;
    expect(value('name')).toBe('Ana Souza');
    expect(value('email')).toBe('ana@example.com');
    expect(TestBed.inject(LanguageService).takeUnsaved('/participate')).toBeUndefined();
  });
});
