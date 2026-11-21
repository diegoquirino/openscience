import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { SessionService } from '../core/session.service';
import { LabelDoneComponent } from './label-done.component';

describe('LabelDoneComponent', () => {
  const signOut = vi.fn();

  function create(outcome?: string) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: SessionService, useValue: { signOut } },
        {
          provide: ActivatedRoute,
          useValue: { queryParamMap: of(convertToParamMap(outcome ? { outcome } : {})) },
        },
      ],
    });
    const fixture = TestBed.createComponent(LabelDoneComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => vi.resetAllMocks());

  it('offers another block only when more are available', () => {
    expect(create('more-available').querySelector('a.another')).not.toBeNull();
  });

  it.each(['database-complete', 'none-available-for-you', 'no-items-yet', ''])(
    'does not offer another block for "%s"',
    (outcome) => {
      expect(create(outcome).querySelector('a.another')).toBeNull();
    },
  );

  it('shows icon cards after a cancelled block', () => {
    const root = create('cancelled');
    expect(root.querySelector('[data-testid="done-another-block"]')).not.toBeNull();
    expect(root.querySelector('[data-testid="done-leave"]')).not.toBeNull();
    expect(root.querySelector('a.another')).toBeNull();
  });

  it('signs out and goes to the start', () => {
    signOut.mockReturnValue(of(undefined));
    const root = create('database-complete');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    root.querySelector<HTMLButtonElement>('button.sign-out')!.click();

    expect(signOut).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith('/');
  });
});
