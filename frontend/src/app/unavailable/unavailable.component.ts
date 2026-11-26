import { Component, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-unavailable',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <section role="alert" class="unavailable">
      <h1 class="page-title">
        <mat-icon class="page-title-icon" fontSet="material-symbols-outlined" aria-hidden="true">cloud_off</mat-icon>
        <span i18n="@@unavailable.title">System temporarily unavailable</span>
      </h1>
      <p i18n="@@unavailable.detail">Please try again in a few minutes.</p>
      <button mat-flat-button type="button" (click)="retry.emit()">
        <mat-icon fontSet="material-symbols-outlined" aria-hidden="true">refresh</mat-icon>
        <span i18n="@@unavailable.retry">Try again</span>
      </button>
    </section>
  `,
})
export class UnavailableComponent {
  readonly retry = output<void>();
}
