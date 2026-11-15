import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { OnboardingService, Step, STEP_ROUTES } from '../core/onboarding.service';
import { Links } from '../core/hal';
import { WelcomeComponent } from './welcome.component';

describe('WelcomeComponent (onboarding resolver)', () => {
  const state = vi.fn();

  function create() {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: OnboardingService, useValue: { state } }],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(WelcomeComponent);
    fixture.detectChanges();
    return { navigate, root: fixture.nativeElement as HTMLElement };
  }

  beforeEach(() => vi.resetAllMocks());

  it.each(Object.keys(STEP_ROUTES) as Step[])('opens the screen of step "%s"', (step) => {
    state.mockReturnValue(of({ step, eligible: step === 'participation', currentConsentFormVersion: 'v1', links: new Links() }));
    const { navigate } = create();
    expect(navigate).toHaveBeenCalledWith(STEP_ROUTES[step], { replaceUrl: true });
  });

  it('shows an error and a retry button when the step cannot be read', () => {
    state.mockReturnValue(throwError(() => new Error('down')));
    const { navigate, root } = create();
    expect(navigate).not.toHaveBeenCalled();
    expect(root.querySelector('[role=alert]')).not.toBeNull();
    expect(root.querySelector('button')).not.toBeNull();
  });
});