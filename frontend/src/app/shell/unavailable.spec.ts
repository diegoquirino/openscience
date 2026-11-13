import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { EntryPoint, EntryPointService, UNREACHABLE } from '../core/entry-point.service';
import { Links } from '../core/hal';
import { ShellComponent } from './shell.component';

@Component({ template: '<p class="page-content">Page content</p>' })
class ContentPage {}

const AVAILABLE: EntryPoint = { systemVersion: '0.3.0', environment: 'production', available: true, links: new Links() };

describe('Shell when the system is unavailable', () => {
  let responses: EntryPoint[];

  beforeEach(async () => {
    responses = [UNREACHABLE];
    const service = { load: (): Observable<EntryPoint> => of(responses.shift() ?? UNREACHABLE) };
    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([{ path: '**', component: ContentPage }]),
        { provide: EntryPointService, useValue: service },
      ],
    }).compileComponents();
  });

  async function render() {
    const fixture = TestBed.createComponent(ShellComponent);
    await TestBed.inject(Router).navigateByUrl('/');
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  it('shows the unavailable message instead of the page content', async () => {
    const { element } = await render();
    const alert = element.querySelector('main [role="alert"]');
    expect(alert?.textContent).toContain('System temporarily unavailable');
    expect(element.querySelector('.page-content')).toBeNull();
    expect(element.querySelector('header')?.textContent).toContain('Veredito');
  });

  it('shows the page again when a retry finds the system available', async () => {
    const { fixture, element } = await render();
    responses.push(AVAILABLE);

    element.querySelector<HTMLButtonElement>('main [role="alert"] button')?.click();
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')).toBeNull();
    expect(element.querySelector('.page-content')?.textContent).toContain('Page content');
  });
});
