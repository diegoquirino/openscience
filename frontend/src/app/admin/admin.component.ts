import { Component, computed, inject } from '@angular/core';
import { MatListModule } from '@angular/material/list';
import { SessionService } from '../core/session.service';

/** The administrator's area; its functions arrive with specs 005 and 009 to 011 (spec 004, US2 scenario 1). */
@Component({
  selector: 'app-admin',
  imports: [MatListModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent {
  private readonly session = inject(SessionService);
  protected readonly name = computed(() => this.session.session()?.name ?? '');
}
