import { clearTranslations, loadTranslations } from '@angular/localize';
import { studyPhaseLabel } from './study-phase-labels';

describe('studyPhaseLabel', () => {
  afterEach(() => clearTranslations());

  it('returns the source labels without translations', () => {
    expect(studyPhaseLabel('pilot')).toBe('pilot');
    expect(studyPhaseLabel('collection')).toBe('collection');
  });

  it('returns the translated label and leaves the code unchanged', () => {
    loadTranslations({ 'studyPhase.pilot': 'piloto', 'studyPhase.collection': 'coleta' });
    const phase = { code: 'collection' };

    expect(studyPhaseLabel('pilot')).toBe('piloto');
    expect(studyPhaseLabel(phase.code)).toBe('coleta');
    expect(phase.code).toBe('collection');
  });

  it('shows an unknown code as is', () => {
    loadTranslations({ 'studyPhase.pilot': 'piloto' });
    expect(studyPhaseLabel('archived')).toBe('archived');
    expect(studyPhaseLabel('toString')).toBe('toString');
  });
});
