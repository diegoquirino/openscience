import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { SessionService } from './session.service';

const CURIES = [{ name: 'vd', href: '/api/v1/rels/{rel}', templated: true }];

const VISITOR_ENTRY = {
  _links: { self: { href: '/api/v1' }, curies: CURIES, 'vd:request-code': { href: '/links/codes' } },
};

const PARTICIPANT_ENTRY = {
  _links: {
    self: { href: '/api/v1' },
    curies: CURIES,
    'vd:session': { href: '/links/session' },
    'vd:me': { href: '/links/me' },
    'vd:sign-out': { href: '/links/session' },
  },
};

const SESSION = {
  subject: 'participant',
  name: 'Ana Souza',
  pseudonym: 'R-0001',
  preferredLanguage: 'es-ES',
  expiresAt: '2026-11-14T17:03:00Z',
  _links: { self: { href: '/links/session' }, 'vd:sign-out': { href: '/links/session' } },
};

describe('SessionService', () => {
  let service: SessionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(SessionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('is a visitor when the entry point offers no vd:session', async () => {
    const result = firstValueFrom(service.refresh());
    http.expectOne('/api/v1').flush(VISITOR_ENTRY);

    expect(await result).toBeNull();
    expect(service.subject()).toBe('visitor');
  });

  it('follows vd:session from the entry point', async () => {
    const result = firstValueFrom(service.refresh());
    http.expectOne('/api/v1').flush(PARTICIPANT_ENTRY);
    http.expectOne('/links/session').flush(SESSION);

    const session = await result;
    expect(session?.pseudonym).toBe('R-0001');
    expect(service.subject()).toBe('participant');
    expect(service.session()?.preferredLanguage).toBe('es-ES');
  });

  it('is a visitor when the session cannot be read', async () => {
    const result = firstValueFrom(service.refresh());
    http.expectOne('/api/v1').flush(PARTICIPANT_ENTRY);
    http.expectOne('/links/session').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(await result).toBeNull();
    expect(service.subject()).toBe('visitor');
  });

  it('requests a code and signs in through the links of the answers', async () => {
    const requested = firstValueFrom(service.requestCode('Ana Souza', 'Ana@Example.com'));
    http.expectOne('/api/v1').flush(VISITOR_ENTRY);
    const codeRequest = http.expectOne('/links/codes');
    expect(codeRequest.request.method).toBe('POST');
    expect(codeRequest.request.body).toEqual({ name: 'Ana Souza', email: 'Ana@Example.com' });
    codeRequest.flush({
      email: 'ana@example.com',
      validityMinutes: 10,
      resendAfterSeconds: 60,
      _links: { 'vd:sign-in': { href: '/links/sessions' } },
    });
    expect(await requested).toEqual({ email: 'ana@example.com', validityMinutes: 10, resendAfterSeconds: 60 });

    const signedIn = firstValueFrom(service.signIn('ana@example.com', '123 456'));
    const signIn = http.expectOne('/links/sessions');
    expect(signIn.request.method).toBe('POST');
    expect(signIn.request.body).toEqual({ email: 'ana@example.com', code: '123 456' });
    signIn.flush(SESSION, { status: 201, statusText: 'Created' });

    expect((await signedIn).subject).toBe('participant');
    expect(service.subject()).toBe('participant');
  });

  it('refuses to sign in before a code was requested', async () => {
    await expect(firstValueFrom(service.signIn('ana@example.com', '123456'))).rejects.toThrow('vd:sign-in');
  });

  it('signs out through vd:sign-out of the session', async () => {
    const refreshed = firstValueFrom(service.refresh());
    http.expectOne('/api/v1').flush(PARTICIPANT_ENTRY);
    http.expectOne('/links/session').flush(SESSION);
    await refreshed;

    const signedOut = firstValueFrom(service.signOut());
    const request = http.expectOne('/links/session');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await signedOut;

    expect(service.subject()).toBe('visitor');
  });
});
