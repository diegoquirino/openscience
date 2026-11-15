import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

/** Stub of the review screen; User Story 3 of spec 007 replaces it. */
@Component({
  selector: 'app-label-review',
  imports: [MatButtonModule, RouterLink],
  template: `
    <section class="review">
      <h1 i18n="@@label.review.title">Review</h1>
      <p i18n="@@label.review.coming">The review of your answers is coming soon.</p>
      <a mat-stroked-button [routerLink]="['/label/items', 1]" i18n="@@label.review.backToItems">Back to the items</a>
    </section>
  `,
  styles: `
    .review {
      display: grid;
      gap: 1rem;
      justify-items: start;
      padding: 1.5rem 0;
    }
  `,
})
export class LabelReviewComponent {}
