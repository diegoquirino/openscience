import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Links } from '../core/hal';
import { ConsentForm, NoConsentFormError, OnboardingService } from '../core/onboarding.service';
import { ConsentComponent } from './consent.component';

describe('ConsentComponent', () => {
  const consentForm = vi.fn();
  const recordConsent = vi.fn();

  function form(translation: boolean, version = 'v1'): ConsentForm {
    return {
      version,
      language: translation ? 'en-US' : 'pt-BR',
      referenceLanguage: 'pt-BR',
      translation,
      text: 'TEXT\n\nSecond paragraph',
      publishedAt: '2026-11-20T12:00:00Z',
      links: new Links(),
    };
  }

  function create() {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: OnboardingService, useValue: { consentForm, recordConsent } }],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(ConsentComponent);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    return { fixture, navigate, root };
  }

  beforeEach(() => vi.resetAllMocks());

  it('shows the translation note and the reference link only for a translation', () => {
    consentForm.mockReturnValue(of(form(true)));
    const translated = create().root;
    expect(translated.querySelector('.translation')).not.toBeNull();
    const link = translated.querySelector<HTMLAnchorElement>('a.reference')!;
    expect(link.getAttribute('target')).toBe('_blank');
    expect(link.getAttribute('rel')).toBe('noopener');
    expect(link.getAttribute('href')).toContain('/consent/reference/v1');
  });

  it('does not show the translation note in the reference language', () => {
    consentForm.mockReturnValue(of(form(false)));
    expect(create().root.querySelector('.translation')).toBeNull();
  });

  it('shows the text in a labelled scrollable region', () => {
    consentForm.mockReturnValue(of(form(false)));
    const region = create().root.querySelector<HTMLElement>('[role=region]')!;
    expect(region.getAttribute('aria-labelledby')).toBe('consent-heading');
    expect(region.textContent).toContain('Second paragraph');
  });

  it('records the acceptance of the version on screen and opens the resolver', () => {
    consentForm.mockReturnValue(of(form(true, 'v7')));
    recordConsent.mockReturnValue(of(undefined));
    const { navigate, root } = create();

    root.querySelector<HTMLButtonElement>('button.accept')!.click();

    expect(recordConsent).toHaveBeenCalledWith('accepted', 'v7');
    expect(navigate).toHaveBeenCalledWith('/welcome');
  });

  it('records the refusal and opens the closing screen', () => {
    consentForm.mockReturnValue(of(form(false)));
    recordConsent.mockReturnValue(of(undefined));
    const { navigate, root } = create();

    root.querySelector<HTMLButtonElement>('button.refuse')!.click();

    expect(recordConsent).toHaveBeenCalledWith('refused', 'v1');
    expect(navigate).toHaveBeenCalledWith('/consent/declined');
  });

  it('reloads the text and warns when the version is outdated', () => {
    consentForm.mockReturnValueOnce(of(form(false, 'v1'))).mockReturnValueOnce(of(form(false, 'v2')));
    recordConsent.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { type: 'urn:veredito:problem:consent-form-outdated', currentVersion: 'v2' },
          }),
      ),
    );
    const { fixture, root } = create();

    root.querySelector<HTMLButtonElement>('button.accept')!.click();
    fixture.detectChanges();

    expect(consentForm).toHaveBeenCalledTimes(2);
    expect(root.querySelector('.notice')).not.toBeNull();
  });

  it('goes to the not-open screen when there is no consent form', () => {
    consentForm.mockReturnValue(throwError(() => new NoConsentFormError()));
    const { navigate } = create();
    expect(navigate).toHaveBeenCalledWith('/not-open', { replaceUrl: true });
  });
});