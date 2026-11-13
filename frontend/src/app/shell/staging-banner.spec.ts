import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { Environment, EntryPointService, UNREACHABLE } from '../core/entry-point.service';
import { Links } from '../core/hal';
import { ShellComponent } from './shell.component';

@Component({ template: '' })
class BlankPage {}

describe('Staging banner in the shell', () => {
  async function render(entryPoint: { environment: Environment; available: boolean }) {
    TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([{ path: '**', component: BlankPage }]),
        {
          provide: EntryPointService,
          useValue: { load: () => of({ systemVersion: '0.3.0', links: new Links(), ...entryPoint }) },
        },
      ],
    });
    const fixture = TestBed.createComponent(ShellComponent);
    await TestBed.inject(Router).navigateByUrl('/');
    await fixture.whenStable();
    return (fixture.nativeElement as HTMLElement).querySelector('header [role="note"]');
  }

  for (const environment of ['staging', 'development'] as const) {
    it(`is shown in ${environment}`, async () => {
      const banner = await render({ environment, available: true });
      expect(banner?.textContent?.trim()).toBe('STAGING ENVIRONMENT');
    });
  }

  it('is hidden in production', async () => {
    expect(await render({ environment: 'production', available: true })).toBeNull();
  });

  it('is hidden when the entry point cannot be reached', async () => {
    expect(await render(UNREACHABLE)).toBeNull();
  });
});
