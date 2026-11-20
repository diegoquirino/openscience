import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import {
  AnonymizationResult,
  ParticipantCard,
  ParticipantsService,
} from './participants.service';

@Component({
  selector: 'app-participants',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, RouterLink],
  templateUrl: './participants.component.html',
  styleUrl: './participants.component.scss',
})
export class ParticipantsComponent implements OnInit {
  private readonly participants = inject(ParticipantsService);

  protected readonly cards = signal<ParticipantCard[]>([]);
  protected readonly card = signal<ParticipantCard | null>(null);
  protected readonly error = signal('');
  protected readonly result = signal<AnonymizationResult | null>(null);
  protected readonly confirming = signal(false);
  protected readonly option = signal<'keep_responses' | 'discard_responses'>('keep_responses');
  protected readonly chosenPseudonym = signal('');
  protected readonly busy = signal(false);

  /** People who still have an e-mail (identifiable). */
  protected readonly withEmail = computed(() => this.cards().filter((c) => !!c.email));

  /** Full list keyed by pseudonym (includes anonymized). */
  protected readonly byPseudonym = computed(() =>
    [...this.cards()].sort((a, b) => a.pseudonym.localeCompare(b.pseudonym)),
  );

  ngOnInit(): void {
    this.reload();
  }

  protected select(person: ParticipantCard): void {
    this.error.set('');
    this.result.set(null);
    this.confirming.set(false);
    this.chosenPseudonym.set(person.pseudonym);
    this.card.set(person);
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
    this.error.set('');
    this.participants.anonymize(current, this.option(), this.chosenPseudonym()).subscribe({
      next: (outcome) => {
        this.busy.set(false);
        this.confirming.set(false);
        this.result.set(outcome);
        this.reload(outcome.pseudonym);
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

  private reload(selectPseudonym?: string): void {
    this.busy.set(true);
    this.participants.list().subscribe({
      next: (cards) => {
        this.busy.set(false);
        this.cards.set(cards);
        if (selectPseudonym) {
          this.card.set(cards.find((c) => c.pseudonym === selectPseudonym) ?? null);
        }
      },
      error: () => {
        this.busy.set(false);
        this.error.set('search-failed');
      },
    });
  }
}
