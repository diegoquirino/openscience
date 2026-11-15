/**
 * Question and answer labels of the screening (Appendix B.3 of spec 001). The code is what the API
 * stores; only the text shown to people is translated.
 */
export const SCREENING_ANSWERS = ['yes_professional', 'yes_academic', 'no'] as const;
export type ScreeningAnswer = (typeof SCREENING_ANSWERS)[number];

const ANSWER_LABELS: Record<ScreeningAnswer, () => string> = {
  yes_professional: () => $localize`:screening answer yes professional@@screening.answer.yes_professional:Yes, in professional practice`,
  yes_academic: () => $localize`:screening answer yes academic@@screening.answer.yes_academic:Yes, in studies, courses or research`,
  no: () => $localize`:screening answer no@@screening.answer.no:No`,
};

export function isScreeningAnswer(value: unknown): value is ScreeningAnswer {
  return typeof value === 'string' && (SCREENING_ANSWERS as readonly string[]).includes(value);
}

/** Label in the language of the build; an unknown code is shown as is. */
export function screeningAnswerLabel(code: string): string {
  return isScreeningAnswer(code) ? ANSWER_LABELS[code]() : code;
}

export function screeningQuestion(): string {
  return $localize`:screening question@@screening.question:Have you ever written, executed or reviewed test cases based on use cases like this one, with a basic flow and alternative and exception flows?`;
}
