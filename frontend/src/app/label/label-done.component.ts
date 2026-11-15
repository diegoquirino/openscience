import { Component, inject, computed } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

@Component({
  selector: 'app-label-done',
  imports: [MatButtonModule, RouterLink],
  template: `
    <section class="done">
      <h1 i18n="@@label.done.title">Thank you</h1>
      <p>{{ message() }}</p>
      @if (outcome() === 'more-available') {
        <a mat-flat-button routerLink="/label" i18n="@@label.another">Label another block</a>
      }
    </section>
  `,
  styles: `
    .done {
      display: grid;
      gap: 1rem;
      padding: 1.5rem 0;
      justify-items: start;
    }
  `,
})
export class LabelDoneComponent {
  private readonly route = inject(ActivatedRoute);
  readonly outcome = toSignal(this.route.queryParamMap.pipe(map((p) => p.get('outcome') ?? '')), {
    initialValue: '',
  });

  readonly message = computed(() => {
    switch (this.outcome()) {
      case 'no-items-yet':
        return $localize`:@@label.end.noItemsYet:No items to label yet.`;
      case 'database-complete':
        return $localize`:@@label.end.databaseComplete:The database has been completely answered.`;
      case 'none-available-for-you':
        return $localize`:@@label.end.noneForYou:There are no more blocks for you at the moment.`;
      case 'more-available':
        return $localize`:@@label.end.moreAvailable:You can label another block.`;
      default:
        return $localize`:@@label.end.thanks:Thank you for labeling.`;
    }
  });
}
