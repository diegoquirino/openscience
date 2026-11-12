import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [RouterLink, MatButtonModule],
  template: `
    <section class="home">
      <p class="intro" i18n="@@home.intro">
        Veredito is the data labeling platform of a doctoral study on software testing: volunteers
        label how edits to use cases affect their tests.
      </p>
      <a mat-flat-button routerLink="/participate" i18n="@@home.participate">Participate</a>
    </section>
  `,
  styles: `
    .home {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 24px;
    }

    .intro {
      margin: 0;
      font: var(--mat-sys-body-large);
    }
  `,
})
export class HomeComponent {}
