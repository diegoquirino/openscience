import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { RouterLink } from '@angular/router';
import {
  AnonymizationResult,
  ParticipantCard,
  ParticipantsService,
} from './participants.service';

@Component({
  selector: 'app-participants',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatRadioModule, RouterLink],
  templateUrl: './participants.component.html',
  styleUrl: './participants.component.scss',
})
export class ParticipantsComponent {
  private readonly participants = inject(ParticipantsService);

  protected readonly email = signal('');
  protected readonly pseudonym = signal('');
  protected readonly card = signal<ParticipantCard | null>(null);
  protected readonly empty = signal(false);
  protected readonly error = signal('');
  protected readonly result = signal<AnonymizationResult | null>(null);
  protected readonly confirming = signal(false);
  protected readonly option = signal<'keep_responses' | 'discard_responses'>('keep_responses');
  protected readonly busy = signal(false);

  protected search(): void {
    this.error.set('');
    this.result.set(null);
    this.confirming.set(false);
    const email = this.email().trim();
    const pseudonym = this.pseudonym().trim();
    if ((email && pseudonym) || (!email && !pseudonym)) {
      this.error.set('exact-one');
      return;
    }
    this.busy.set(true);
    this.participants.search(email ? { email } : { pseudonym }).subscribe({
      next: (cards) => {
        this.busy.set(false);
        this.empty.set(cards.length === 0);
        this.card.set(cards[0] ?? null);
      },
      error: () => {
        this.busy.set(false);
        this.error.set('search-failed');
      },
    });
  }

  protected askConfirm(option: 'keep_responses' | 'discard_responses'): void {
    this.option.set(option);
    this.confirming.set(true);
  }

  protected confirm(): void {
    const current = this.card();
    if (!current) {
      return;
    }
    this.busy.set(true);
    this.participants.anonymize(current, this.option()).subscribe({
      next: (outcome) => {
        this.busy.set(false);
        this.confirming.set(false);
        this.result.set(outcome);
        this.search();
      },
      error: () => {
        this.busy.set(false);
        this.error.set('anonymize-failed');
      },
    });
  }

  protected cancelConfirm(): void {
    this.confirming.set(false);
  }
}
