import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { OnboardingService, Step, STEP_ROUTES } from '../core/onboarding.service';
import { Links } from '../core/hal';
import { ToastService } from '../core/toast.service';
import { WelcomeComponent } from './welcome.component';

describe('WelcomeComponent (onboarding resolver)', () => {
  const state = vi.fn();

  function create() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: OnboardingService, useValue: { state } },
        {
          provide: ToastService,
          useValue: { error: vi.fn(), success: vi.fn(), warn: vi.fn(), info: vi.fn(), show: vi.fn(), action: vi.fn() },
        },
      ],
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
    const toast = TestBed.inject(ToastService) as unknown as { error: ReturnType<typeof vi.fn> };
    expect(navigate).not.toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalled();
    expect(root.querySelector('button')).not.toBeNull();
  });
});