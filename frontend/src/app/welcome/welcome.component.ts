import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

/** Participant landing; starts the labeling loop of spec 007. */
@Component({
  selector: 'app-welcome',
  imports: [MatButtonModule, RouterLink],
  template: `
    <section class="welcome">
      <h1 i18n="@@welcome.title">Welcome</h1>
      <p i18n="@@welcome.next">Thank you for identifying yourself. You can request a block to label.</p>
      <a mat-flat-button color="primary" routerLink="/label" i18n="@@welcome.start">Start labeling</a>
    </section>
  `,
  styles: `
    .welcome {
      display: grid;
      gap: 1rem;
      justify-items: start;
      padding: 1.5rem 0;
    }
  `,
})
export class WelcomeComponent {}
