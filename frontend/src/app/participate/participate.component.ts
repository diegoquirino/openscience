import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Provisional page until sign-in by e-mail code (spec 004). */
@Component({
  selector: 'app-participate',
  imports: [RouterLink],
  template: `
    <h1 i18n="@@participate.title">Participate</h1>
    <p i18n="@@participate.soon">Sign-in with a code sent by e-mail is coming soon.</p>
    <a routerLink="/" i18n="@@common.backHome">Back to the home page</a>
  `,
})
export class ParticipateComponent {}
