import { Component, computed, inject, LOCALE_ID, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatToolbarModule } from '@angular/material/toolbar';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { BUILD_VERSION } from '../build-info';
import { EntryPoint, EntryPointService } from '../core/entry-point.service';
import { UnavailableComponent } from '../unavailable/unavailable.component';
import { StagingBannerComponent } from './staging-banner.component';

/** Supported locales, each labeled in its own language. */
export const LOCALES = [
  { code: 'pt-BR', label: 'Português (Brasil)' },
  { code: 'en-US', label: 'English (US)' },
  { code: 'es-ES', label: 'Español (España)' },
] as const;

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, MatToolbarModule, UnavailableComponent, StagingBannerComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly router = inject(Router);
  private readonly locale = inject(LOCALE_ID);
  private readonly entryPoints = inject(EntryPointService);
  protected readonly version = BUILD_VERSION;

  private readonly entryPoint = signal<EntryPoint | null>(null);
  protected readonly unavailable = computed(() => this.entryPoint()?.available === false);
  protected readonly environment = computed(() => this.entryPoint()?.environment ?? null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.entryPoints.load().subscribe((entryPoint) => this.entryPoint.set(entryPoint));
  }

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  /** Provisional language switch: a full page load of the same route under another prefix. */
  protected readonly otherLocales = computed(() =>
    LOCALES.filter((option) => option.code !== this.locale).map((option) => ({
      ...option,
      href: `/${option.code}${this.url()}`,
    })),
  );
}
