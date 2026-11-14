import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Links } from './hal';
import { SessionService } from './session.service';
import { unauthorizedInterceptor } from './unauthorized.interceptor';

@Component({ template: '' })
class BlankPage {}

describe('unauthorizedInterceptor', () => {
  let http: HttpTestingController;
  let client: HttpClient;
  let session: SessionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([unauthorizedInterceptor])),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'participate', component: BlankPage },
          { path: 'welcome', component: BlankPage },
        ]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
    session = TestBed.inject(SessionService);
    session.session.set({
      subject: 'participant',
      name: 'Ana Souza',
      pseudonym: 'R-0001',
      preferredLanguage: 'pt-BR',
      expiresAt: '2026-11-14T17:03:00Z',
      links: Links.of({}),
    });
  });

  afterEach(() => http.verify());

  it('forgets the session and opens the sign-in form on a 401 from the API', async () => {
    await TestBed.inject(Router).navigateByUrl('/welcome');

    const call = firstValueFrom(client.get('/api/v1/participants/me'));
    http.expectOne('/api/v1/participants/me').flush({}, { status: 401, statusText: 'Unauthorized' });

    await expect(call).rejects.toMatchObject({ status: 401 });
    await new Promise((resolve) => setTimeout(resolve));
    expect(TestBed.inject(Router).url).toBe('/participate');
    expect(session.subject()).toBe('visitor');
  });

  it('leaves other errors to the caller', async () => {
    await TestBed.inject(Router).navigateByUrl('/welcome');

    const call = firstValueFrom(client.get('/api/v1/participants/me'));
    http.expectOne('/api/v1/participants/me').flush({}, { status: 403, statusText: 'Forbidden' });

    await expect(call).rejects.toMatchObject({ status: 403 });
    expect(TestBed.inject(Router).url).toBe('/welcome');
    expect(session.subject()).toBe('participant');
  });
});
