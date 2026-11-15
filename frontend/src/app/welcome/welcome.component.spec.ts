import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { WelcomeComponent } from './welcome.component';

describe('WelcomeComponent', () => {
  it('thanks the participant and offers to start labeling', async () => {
    TestBed.configureTestingModule({
      imports: [WelcomeComponent],
      providers: [provideRouter([])],
    });
    const fixture = TestBed.createComponent(WelcomeComponent);
    await fixture.whenStable();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Thank you for identifying yourself');
    expect(text).toContain('Start labeling');
  });
});
