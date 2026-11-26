import { ChangeImpact, MaintenanceOperation } from './response-labels';

/**
 * Statements and explanations of the two questions (Appendices B.1 and B.2 of spec 001). The
 * texts are fixed in the application, in the three locales; the option labels are in
 * `response-labels.ts` and the codes that are stored never change with the language.
 */

/** Order of precedence when more than one operation applies, the greatest consequence first. */
export const OPERATION_PRECEDENCE: readonly MaintenanceOperation[] = [
  'FLAG',
  'REMOVE',
  'MERGE',
  'SPLIT',
  'CREATE',
  'UPDATE',
  'REASSIGN',
  'KEEP',
];

const IMPACT_EXPLANATIONS: Record<ChangeImpact, () => string> = {
  LOW: () =>
    $localize`:impact explanation LOW@@item.impact.LOW:The edit does not change the observable behavior. Examples: rewording, added detail, synonym, typo fix, punctuation, formatting, shortening, reordering, renaming, or a change only in the requirements link. A step that was only moved, or merged with another or split from it, without losing behavior, is also low.`,
  HIGH: () =>
    $localize`:impact explanation HIGH@@item.impact.HIGH:The edit changes behavior, inputs, outputs, conditions or the flow. Examples: adding or removing a requirement, feature, entity, step, alternative or exception flow. It is also high when the text becomes so vague that the tester cannot decide whether the test passed or failed.`,
};

const OPERATION_EXPLANATIONS: Record<MaintenanceOperation, () => string> = {
  KEEP: () =>
    $localize`:operation explanation KEEP@@item.operation.KEEP:The edit does not affect exercised behavior, or it only rewords the text; verification remains possible as it is.`,
  UPDATE: () =>
    $localize`:operation explanation UPDATE@@item.operation.UPDATE:The behavior still exists, but a step, data item, condition or expected result changed, or the expected result needs to mention a new detail.`,
  REMOVE: () =>
    $localize`:operation explanation REMOVE@@item.operation.REMOVE:The verified behavior no longer exists (it was deleted, not moved or merged with another).`,
  CREATE: () =>
    $localize`:operation explanation CREATE@@item.operation.CREATE:There is new behavior that nothing existing covers.`,
  MERGE: () =>
    $localize`:operation explanation MERGE@@item.operation.MERGE:Separate behaviors were merged into a single one.`,
  SPLIT: () =>
    $localize`:operation explanation SPLIT@@item.operation.SPLIT:What was a single behavior became several independently verifiable ones.`,
  REASSIGN: () =>
    $localize`:operation explanation REASSIGN@@item.operation.REASSIGN:The behavior is the same, but the requirements link changed.`,
  FLAG: () =>
    $localize`:operation explanation FLAG@@item.operation.FLAG:The edit is ambiguous or leaves a step vague; there is no safe decision and human analysis is needed.`,
};

/** Statement of the impact question (Appendix B.1). */
export function impactStatement(): string {
  return $localize`:impact question@@item.impact.question:Does this edit change the behavior that the use case specifies?`;
}

/** Statement of the operation question (Appendix B.2). */
export function operationStatement(): string {
  return $localize`:operation question@@item.operation.question:What should be done with the manual test cases that exercise this part?`;
}

/** Sentence before the precedence order of the operations (Appendix B.2). */
export function precedenceIntro(): string {
  return $localize`:operation precedence rule@@item.operation.precedence:If more than one applies, choose the one with the greatest consequence:`;
}

/** Definition and examples of an impact option (Appendix B.1). */
export function impactExplanation(code: ChangeImpact): string {
  return IMPACT_EXPLANATIONS[code]();
}

/** "When to choose" text of an operation option (Appendix B.2). */
export function operationExplanation(code: MaintenanceOperation): string {
  return OPERATION_EXPLANATIONS[code]();
}
