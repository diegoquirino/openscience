import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterOutlet } from '@angular/router';
import { BUILD_VERSION } from '../build-info';
import { EntryPoint, EntryPointService } from '../core/entry-point.service';
import { LanguageService, Locale, LOCALES } from '../core/language.service';
import { UnavailableComponent } from '../unavailable/unavailable.component';
import { StagingBannerComponent } from './staging-banner.component';

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    MatToolbarModule,
    MatButtonModule,
    MatMenuModule,
    UnavailableComponent,
    StagingBannerComponent,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly entryPoints = inject(EntryPointService);
  private readonly language = inject(LanguageService);
  protected readonly version = BUILD_VERSION;

  private readonly entryPoint = signal<EntryPoint | null>(null);
  protected readonly unavailable = computed(() => this.entryPoint()?.available === false);
  protected readonly environment = computed(() => this.entryPoint()?.environment ?? null);

  protected readonly locales = LOCALES;
  protected readonly currentLocale = this.language.current;
  protected readonly currentLabel = LOCALES.find((option) => option.code === this.language.current)?.label ?? '';
  protected readonly switchLabel = $localize`:@@shell.language:Language: ${this.currentLabel}:language:`;

  constructor() {
    this.load();
  }

  protected load(): void {
    this.entryPoints.load().subscribe((entryPoint) => this.entryPoint.set(entryPoint));
  }

  /** Header language switch (FR-013): the same screen under the new prefix, with a page load. */
  protected switchTo(locale: Locale): void {
    if (locale !== this.currentLocale) {
      this.language.switchTo(locale).subscribe();
    }
  }
}
