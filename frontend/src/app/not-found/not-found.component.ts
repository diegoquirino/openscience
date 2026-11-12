import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  template: `
    <h1 i18n="@@notFound.title">Page not found</h1>
    <p i18n="@@notFound.detail">The address you opened does not exist.</p>
    <a routerLink="/" i18n="@@common.backHome">Back to the home page</a>
  `,
})
export class NotFoundComponent {}
