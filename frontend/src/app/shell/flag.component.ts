import { Component, input } from '@angular/core';
import { Locale } from '../core/language.service';

/**
 * Simplified flag of the country of a locale, drawn inline: Windows shows flag emoji as two letters,
 * and an image file would be one more request. Decorative; the button or menu item names the language.
 */
@Component({
  selector: 'app-flag',
  template: `
    <svg aria-hidden="true" focusable="false" preserveAspectRatio="none" [attr.viewBox]="viewBox[locale()]">
      @switch (locale()) {
        @case ('pt-BR') {
          <rect width="20" height="14" fill="#009c3b" />
          <polygon points="10,1.7 18.3,7 10,12.3 1.7,7" fill="#ffdf00" />
          <circle cx="10" cy="7" r="3.5" fill="#002776" />
          <path d="M6.7 6.1 Q10 5.4 13.4 7.6" stroke="#fff" stroke-width="0.7" fill="none" />
        }
        @case ('en-US') {
          <rect width="38" height="20" fill="#b22234" />
          @for (y of usWhiteStripes; track y) {
            <rect [attr.y]="y" width="38" height="1.54" fill="#fff" />
          }
          <rect width="15.2" height="10.77" fill="#3c3b6e" />
        }
        @case ('es-ES') {
          <rect width="3" height="2" fill="#aa151b" />
          <rect y="0.5" width="3" height="1" fill="#f1bf00" />
        }
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex: none;
    }

    svg {
      width: 1.5em;
      height: 1em;
      border-radius: 2px;
      box-shadow: 0 0 0 1px rgb(0 0 0 / 0.2);
    }
  `,
})
export class FlagComponent {
  readonly locale = input.required<Locale>();

  protected readonly viewBox: Record<Locale, string> = {
    'pt-BR': '0 0 20 14',
    'en-US': '0 0 38 20',
    'es-ES': '0 0 3 2',
  };

  /** The six white stripes among the thirteen of the United States flag. */
  protected readonly usWhiteStripes = [1, 3, 5, 7, 9, 11].map((stripe) => +((stripe * 20) / 13).toFixed(2));
}
