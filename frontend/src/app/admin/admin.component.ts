import { Component, computed, inject } from '@angular/core';
import { MatListModule } from '@angular/material/list';
import { RouterLink } from '@angular/router';
import { SessionService } from '../core/session.service';

/** The administrator's area; Import is live in 005 and Export in 009; Dashboard/Participants arrive later. */
@Component({
  selector: 'app-admin',
  imports: [MatListModule, RouterLink],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent {
  private readonly session = inject(SessionService);
  protected readonly name = computed(() => this.session.session()?.name ?? '');
}
