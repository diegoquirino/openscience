import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { BUILD_VERSION } from '../build-info';
import { EntryPointService } from '../core/entry-point.service';
import { Links } from '../core/hal';
import { ShellComponent } from './shell.component';

@Component({ template: '' })
class BlankPage {}

describe('ShellComponent', () => {
  beforeEach(async () => {
    const available = { systemVersion: '0.3.0', environment: 'production', available: true, links: new Links() };
    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([{ path: '**', component: BlankPage }]),
        { provide: EntryPointService, useValue: { load: () => of(available) } },
      ],
    }).compileComponents();
  });

  async function render(url = '/') {
    const fixture = TestBed.createComponent(ShellComponent);
    await TestBed.inject(Router).navigateByUrl(url);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows Veredito in the header', async () => {
    const element = await render();
    expect(element.querySelector('header')?.textContent).toContain('Veredito');
  });

  it('shows the build version in the footer', async () => {
    const element = await render();
    expect(element.querySelector('footer')?.textContent).toContain(BUILD_VERSION);
  });

  it('links the current route under the other two locales', async () => {
    const element = await render('/participate');
    const links = Array.from(element.querySelectorAll<HTMLAnchorElement>('footer nav a')).map((a) =>
      a.getAttribute('href'),
    );
    expect(links).toEqual(['/pt-BR/participate', '/es-ES/participate']);
  });
});
