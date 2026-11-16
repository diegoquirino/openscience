import { LiveAnnouncer } from '@angular/cdk/a11y';
import { DestroyRef, Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { formatCountdown, remainingSeconds } from '../countdown';
import { LabelService } from '../label.service';

/** The last stretch of the reservation that uses the alert color and is announced (FR-007). */
export const URGENT_SECONDS = 120;

/**
 * The reservation clock of the item screen (spec 007 countdown, spec 008 FR-007). The page and the
 * use case dialog show the same values, so there is one interval and one announcement.
 */
@Injectable()
export class ItemClock {
  private readonly labels = inject(LabelService);
  private readonly announcer = inject(LiveAnnouncer);
  private readonly now = signal(Date.now());
  private announced = false;

  /** Seconds left of the reservation, `null` when the task has no expiry. */
  readonly remaining = computed(() =>
    remainingSeconds(this.labels.task()?.reservationExpiresAt, this.labels.clockOffsetMs(), this.now()),
  );
  readonly countdown = computed(() => {
    const seconds = this.remaining();
    return seconds === null ? '' : formatCountdown(seconds);
  });
  readonly expired = computed(() => this.remaining() === 0);
  /** True while less than 2 minutes are left and the time has not run out. */
  readonly urgent = computed(() => {
    const seconds = this.remaining();
    return seconds !== null && seconds > 0 && seconds <= URGENT_SECONDS;
  });

  constructor() {
    const timer = setInterval(() => this.now.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
    effect(() => {
      const seconds = this.remaining();
      untracked(() => this.announceOnce(seconds));
    });
  }

  /**
   * Announces the last 2 minutes once to screen readers; a save that renews the reservation arms
   * the announcement again for the next time it runs low.
   */
  private announceOnce(seconds: number | null): void {
    if (seconds === null || seconds > URGENT_SECONDS) {
      this.announced = false;
    } else if (seconds > 0 && !this.announced) {
      this.announced = true;
      void this.announcer.announce(this.message(), 'polite');
    }
  }

  private message(): string {
    return $localize`:countdown announcement@@item.progress.lowTime:Less than 2 minutes left in this reservation.`;
  }
}
