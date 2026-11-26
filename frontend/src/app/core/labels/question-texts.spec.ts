import {
  OPERATION_PRECEDENCE,
  impactExplanation,
  impactStatement,
  operationExplanation,
  operationStatement,
  precedenceIntro,
} from './question-texts';
import { CHANGE_IMPACTS, MAINTENANCE_OPERATIONS } from './response-labels';

describe('question-texts', () => {
  it('explains every impact and every operation code', () => {
    for (const code of CHANGE_IMPACTS) {
      expect(impactExplanation(code).length).toBeGreaterThan(40);
    }
    for (const code of MAINTENANCE_OPERATIONS) {
      expect(operationExplanation(code).length).toBeGreaterThan(30);
    }
  });

  it('keeps the statements and the rule of Appendix B (source language of the test build)', () => {
    expect(impactStatement()).toBe('Does this edit change the behavior that the use case specifies?');
    expect(operationStatement()).toBe('What should be done with the manual test cases that exercise this part?');
    expect(precedenceIntro()).toBe('If more than one applies, choose the one with the greatest consequence:');
    expect(impactExplanation('HIGH')).toContain('so vague that the tester cannot decide');
    expect(operationExplanation('MERGE')).toBe('Separate behaviors were merged into a single one.');
  });

  it('lists each operation once in the order FLAG > REMOVE > MERGE > SPLIT > CREATE > UPDATE > REASSIGN > KEEP', () => {
    expect(OPERATION_PRECEDENCE.join(' > ')).toBe('FLAG > REMOVE > MERGE > SPLIT > CREATE > UPDATE > REASSIGN > KEEP');
    expect([...OPERATION_PRECEDENCE].sort()).toEqual([...MAINTENANCE_OPERATIONS].sort());
  });
});
