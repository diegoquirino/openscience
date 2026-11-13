import { Component, computed, input } from '@angular/core';
import { Environment } from '../core/entry-point.service';

/** Shown outside production, so nobody mistakes local staging for the real study. */
@Component({
  selector: 'app-staging-banner',
  template: `
    @if (visible()) {
      <div class="staging-banner" role="note" i18n="@@staging.banner">STAGING ENVIRONMENT</div>
    }
  `,
  styles: `
    .staging-banner {
      padding: 4px 16px;
      background-color: var(--mat-sys-tertiary-container);
      color: var(--mat-sys-on-tertiary-container);
      font: var(--mat-sys-label-large);
      letter-spacing: 0.08em;
      text-align: center;
    }
  `,
})
export class StagingBannerComponent {
  readonly environment = input<Environment | null>(null);
  protected readonly visible = computed(() => {
    const environment = this.environment();
    return environment !== null && environment !== 'production';
  });
}
