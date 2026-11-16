import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ItemClock } from './item-clock';

/**
 * "x of n" and the reservation countdown (spec 007 FR-017/FR-013, spec 008 FR-007). The item page
 * and the use case dialog both render it from the same clock, so it is always visible.
 */
@Component({
  selector: 'app-item-progress',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p class="counter" i18n="@@label.item.counter">{{ position() }} of {{ size() }}</p>
    @if (clock().countdown()) {
      <p
        class="countdown"
        role="timer"
        [class.urgent]="clock().urgent()"
        [class.expired]="clock().expired()"
      >
        <span i18n="@@label.item.timeLeft">Time left</span>
        <strong>{{ clock().countdown() }}</strong>
      </p>
    }
  `,
  styles: `
    :host {
      display: flex;
      flex-wrap: wrap;
      align-items: baseline;
      justify-content: space-between;
      gap: 0.25rem 1rem;
    }

    p {
      margin: 0;
    }

    .counter {
      font: var(--mat-sys-title-medium);
    }

    .countdown {
      display: flex;
      gap: 0.5rem;
      align-items: baseline;
      color: var(--mat-sys-on-surface-variant);

      strong {
        font: var(--mat-sys-title-medium);
        font-variant-numeric: tabular-nums;
      }

      &.urgent,
      &.expired {
        color: var(--mat-sys-error);
      }
    }
  `,
})
export class ItemProgressComponent {
  readonly position = input.required<number>();
  readonly size = input.required<number>();
  readonly clock = input.required<ItemClock>();
}
