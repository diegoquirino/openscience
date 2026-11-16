import { Component, computed, inject } from '@angular/core';
import { MatListModule } from '@angular/material/list';
import { RouterLink } from '@angular/router';
import { SessionService } from '../core/session.service';

/** The administrator's area; Import, Export, Dashboard and Participants are live. */
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
