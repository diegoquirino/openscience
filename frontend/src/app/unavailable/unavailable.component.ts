import { Component, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-unavailable',
  imports: [MatButtonModule],
  template: `
    <section role="alert" class="unavailable">
      <h1 i18n="@@unavailable.title">System temporarily unavailable</h1>
      <p i18n="@@unavailable.detail">Please try again in a few minutes.</p>
      <button mat-flat-button type="button" (click)="retry.emit()" i18n="@@unavailable.retry">Try again</button>
    </section>
  `,
})
export class UnavailableComponent {
  readonly retry = output<void>();
}
