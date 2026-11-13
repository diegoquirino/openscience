import { TestBed } from '@angular/core/testing';
import { WelcomeComponent } from './welcome.component';

describe('WelcomeComponent', () => {
  it('thanks the participant and announces the next step', async () => {
    const fixture = TestBed.createComponent(WelcomeComponent);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Thank you for identifying yourself; the next step will be available soon.',
    );
  });
});
