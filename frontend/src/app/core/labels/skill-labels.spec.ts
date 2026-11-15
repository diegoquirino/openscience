import { clearTranslations, loadTranslations } from '@angular/localize';
import { codePoints, isSkill, SKILL_OTHER_MAX, skillAnswerValid, skillLabel, SKILLS } from './skill-labels';

describe('skill labels', () => {
  afterEach(() => clearTranslations());

  it('lists the 13 codes of Appendix B.4 in order', () => {
    expect([...SKILLS]).toEqual([
      'developer',
      'software_engineer',
      'systems_analyst',
      'requirements_analyst',
      'test_analyst',
      'test_engineer',
      'software_architect',
      'tech_lead',
      'product_project_manager',
      'professor_researcher',
      'graduate_student',
      'undergraduate_student',
      'other',
    ]);
  });

  it('gives every code a label different from the code', () => {
    for (const code of SKILLS) {
      expect(skillLabel(code)).not.toBe('');
      expect(skillLabel(code)).not.toBe(code);
    }
  });

  it('translates the label and leaves the code unchanged', () => {
    loadTranslations({ 'skill.tech_lead': 'Lider tecnico(a)' });
    const code = 'tech_lead';
    expect(skillLabel(code)).toBe('Lider tecnico(a)');
    expect(isSkill(code)).toBe(true);
  });

  it('shows an unknown code as is', () => {
    expect(skillLabel('astronaut')).toBe('astronaut');
    expect(skillLabel('toString')).toBe('toString');
  });
});

describe('skillAnswerValid', () => {
  it('needs an option', () => {
    expect(skillAnswerValid(null, '')).toBe(false);
    expect(skillAnswerValid('astronaut', '')).toBe(false);
    expect(skillAnswerValid('developer', '')).toBe(true);
  });

  it('needs a non-blank description for "other"', () => {
    expect(skillAnswerValid('other', '')).toBe(false);
    expect(skillAnswerValid('other', '   ')).toBe(false);
    expect(skillAnswerValid('other', 'Technical writer')).toBe(true);
  });

  it('accepts 100 characters and refuses 101', () => {
    expect(skillAnswerValid('other', 'a'.repeat(SKILL_OTHER_MAX))).toBe(true);
    expect(skillAnswerValid('other', 'a'.repeat(SKILL_OTHER_MAX + 1))).toBe(false);
  });

  it('counts code points, not UTF-16 units', () => {
    const emoji = '\u{1F600}'.repeat(100);
    expect(emoji.length).toBe(200);
    expect(codePoints(emoji)).toBe(100);
    expect(skillAnswerValid('other', emoji)).toBe(true);
    expect(skillAnswerValid('other', emoji + 'a')).toBe(false);
  });
});