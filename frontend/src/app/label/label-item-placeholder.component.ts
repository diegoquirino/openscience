import { Component } from '@angular/core';

/** Placeholder until US2 ships the simple item screen. */
@Component({
  selector: 'app-label-item-placeholder',
  template: `
    <section>
      <h1 i18n="@@label.item.placeholderTitle">Block reserved</h1>
      <p i18n="@@label.item.placeholderBody">The labeling screen for this item will be available next.</p>
    </section>
  `,
})
export class LabelItemPlaceholderComponent {}
