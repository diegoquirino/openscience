import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { HomeComponent } from './home.component';

@Component({ template: '' })
class BlankPage {}

describe('HomeComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [provideRouter([{ path: 'participate', component: BlankPage }])],
    }).compileComponents();
  });

  it('shows a sentence about the study', async () => {
    const fixture = TestBed.createComponent(HomeComponent);
    await fixture.whenStable();
    const intro = (fixture.nativeElement as HTMLElement).querySelector('.intro');
    expect(intro?.textContent).toContain('study');
  });

  it('leads to the participate page', async () => {
    const fixture = TestBed.createComponent(HomeComponent);
    await fixture.whenStable();
    const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLAnchorElement>('a[mat-flat-button]');
    expect(button?.textContent?.trim()).toBe('Participate');
    button?.click();
    await fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/participate');
  });
});
