import { Component } from '@angular/core';

/** Provisional participant screen until consent and screening (spec 006); spec 004, US2 scenario 2. */
@Component({
  selector: 'app-welcome',
  template: `
    <section class="welcome">
      <h1 i18n="@@welcome.title">Welcome</h1>
      <p i18n="@@welcome.next">Thank you for identifying yourself; the next step will be available soon.</p>
    </section>
  `,
})
export class WelcomeComponent {}
