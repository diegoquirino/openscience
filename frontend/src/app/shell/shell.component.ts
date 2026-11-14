import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { BUILD_VERSION } from '../build-info';
import { EntryPoint, EntryPointService } from '../core/entry-point.service';
import { LanguageService, Locale, LOCALES } from '../core/language.service';
import { SessionService } from '../core/session.service';
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
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  protected readonly version = BUILD_VERSION;
  protected readonly signedIn = this.session.session;

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

  /** Follows `vd:sign-out`; the session is gone locally even if the call fails (US3 scenario 3). */
  protected signOut(): void {
    this.session.signOut().subscribe(() => void this.router.navigateByUrl('/'));
  }

  /** Header language switch (FR-013): the same screen under the new prefix, with a page load. */
  protected switchTo(locale: Locale): void {
    if (locale !== this.currentLocale) {
      this.language.switchTo(locale).subscribe();
    }
  }
}
