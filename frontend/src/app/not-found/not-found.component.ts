import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [MatIconModule, RouterLink],
  template: `
    <h1 class="page-title">
      <mat-icon class="page-title-icon" fontSet="material-symbols-outlined" aria-hidden="true">error</mat-icon>
      <span i18n="@@notFound.title">Page not found</span>
    </h1>
    <p i18n="@@notFound.detail">The address you opened does not exist.</p>
    <a routerLink="/" i18n="@@common.backHome">Back to the home page</a>
  `,
})
export class NotFoundComponent {}
