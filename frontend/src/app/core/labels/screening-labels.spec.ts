import { clearTranslations, loadTranslations } from '@angular/localize';
import { isScreeningAnswer, SCREENING_ANSWERS, screeningAnswerLabel, screeningQuestion } from './screening-labels';

describe('screening labels', () => {
  afterEach(() => clearTranslations());

  it('has the three answers of Appendix B.3', () => {
    expect([...SCREENING_ANSWERS]).toEqual(['yes_professional', 'yes_academic', 'no']);
    expect(isScreeningAnswer('yes_academic')).toBe(true);
    expect(isScreeningAnswer('maybe')).toBe(false);
  });

  it('labels each answer and the question', () => {
    expect(screeningAnswerLabel('yes_professional')).toBe('Yes, in professional practice');
    expect(screeningAnswerLabel('yes_academic')).toBe('Yes, in studies, courses or research');
    expect(screeningAnswerLabel('no')).toBe('No');
    expect(screeningQuestion()).toContain('test cases');
  });

  it('translates the label and leaves the code unchanged', () => {
    loadTranslations({ 'screening.answer.no': 'Nao' });
    const code = 'no';
    expect(screeningAnswerLabel(code)).toBe('Nao');
    expect(code).toBe('no');
  });

  it('shows an unknown code as is', () => {
    expect(screeningAnswerLabel('maybe')).toBe('maybe');
  });
});