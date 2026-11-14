import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { HomeComponent } from './home.component';

@Component({ template: '' })
class BlankPage {}

const CURIES = [{ name: 'vd', href: '/api/v1/rels/{rel}', templated: true }];

describe('HomeComponent', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'participate', component: BlankPage },
          { path: 'welcome', component: BlankPage },
        ]),
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function render(entryLinks: object, session?: object): Promise<ComponentFixture<HomeComponent>> {
    const fixture = TestBed.createComponent(HomeComponent);
    http.expectOne('/api/v1').flush({ _links: { curies: CURIES, ...entryLinks } });
    if (session) {
      http.expectOne('/links/session').flush(session);
    }
    await fixture.whenStable();
    return fixture;
  }

  function participateButton(fixture: ComponentFixture<HomeComponent>): HTMLAnchorElement {
    return (fixture.nativeElement as HTMLElement).querySelector<HTMLAnchorElement>('a[mat-flat-button]')!;
  }

  it('shows a sentence about the study', async () => {
    const fixture = await render({ 'vd:request-code': { href: '/links/codes' } });
    const intro = (fixture.nativeElement as HTMLElement).querySelector('.intro');
    expect(intro?.textContent).toContain('study');
  });

  it('leads a visitor to the participate page', async () => {
    const fixture = await render({ 'vd:request-code': { href: '/links/codes' } });
    const button = participateButton(fixture);
    expect(button.textContent?.trim()).toBe('Participate');
    button.click();
    await fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/participate');
  });

  it('leads a signed-in participant to the welcome screen', async () => {
    const fixture = await render(
      { 'vd:session': { href: '/links/session' } },
      { subject: 'participant', name: 'Ana Souza', expiresAt: '2026-11-14T17:03:00Z' },
    );
    participateButton(fixture).click();
    await fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/welcome');
  });
});
