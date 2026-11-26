/**
 * Display labels of `kind` and `field` (Appendix A of spec 001) and the localized stand-ins of
 * spec 008 (FR-009, FR-010). Only labels are translated: a value without a label is returned as
 * it is, in every locale, and the data itself is never touched.
 */
export const ITEM_KINDS = ['MOD', 'ADD', 'DEL', 'META'] as const;
export type ItemKind = (typeof ITEM_KINDS)[number];

export const ITEM_FIELDS = [
  'step',
  'steps_block',
  'alt_flow_step',
  'alt_flow_steps_block',
  'alt_flow_title',
  'exc_flow_step',
  'exc_flow_steps_block',
  'exc_flow_title',
  'pre',
  'post',
  'requirements',
] as const;
export type ItemField = (typeof ITEM_FIELDS)[number];

const KIND_LABELS: Record<ItemKind, () => string> = {
  MOD: () => $localize`:edit kind MOD@@item.kind.MOD:Modification`,
  ADD: () => $localize`:edit kind ADD@@item.kind.ADD:Addition`,
  DEL: () => $localize`:edit kind DEL@@item.kind.DEL:Removal`,
  META: () => $localize`:edit kind META@@item.kind.META:Metadata`,
};

const FIELD_LABELS: Record<ItemField, () => string> = {
  step: () => $localize`:edit field step@@item.field.step:Basic flow step`,
  steps_block: () => $localize`:edit field steps_block@@item.field.steps_block:Block of basic flow steps`,
  alt_flow_step: () => $localize`:edit field alt_flow_step@@item.field.alt_flow_step:Alternative flow step`,
  alt_flow_steps_block: () =>
    $localize`:edit field alt_flow_steps_block@@item.field.alt_flow_steps_block:Entire alternative flow`,
  alt_flow_title: () => $localize`:edit field alt_flow_title@@item.field.alt_flow_title:Alternative flow title`,
  exc_flow_step: () => $localize`:edit field exc_flow_step@@item.field.exc_flow_step:Exception flow step`,
  exc_flow_steps_block: () =>
    $localize`:edit field exc_flow_steps_block@@item.field.exc_flow_steps_block:Entire exception flow`,
  exc_flow_title: () => $localize`:edit field exc_flow_title@@item.field.exc_flow_title:Exception flow title`,
  pre: () => $localize`:edit field pre@@item.field.pre:Precondition`,
  post: () => $localize`:edit field post@@item.field.post:Postcondition`,
  requirements: () => $localize`:edit field requirements@@item.field.requirements:Requirements link`,
};

export function isItemKind(value: unknown): value is ItemKind {
  return typeof value === 'string' && (ITEM_KINDS as readonly string[]).includes(value);
}

export function isItemField(value: unknown): value is ItemField {
  return typeof value === 'string' && (ITEM_FIELDS as readonly string[]).includes(value);
}

/** Label of the kind in the language of the build; an unknown kind is shown as is. */
export function kindLabel(code: string): string {
  return isItemKind(code) ? KIND_LABELS[code]() : code;
}

/** Label of the field in the language of the build; a field without a label is shown as is. */
export function fieldLabel(code: string): string {
  return isItemField(code) ? FIELD_LABELS[code]() : code;
}

/** The empty side of an edit (FR-010). */
export function noneLabel(): string {
  return $localize`:empty side of an edit@@item.none:(none)`;
}

/** Shown instead of the pipeline text of a use case that does not exist in a version (FR-010). */
export function missingUsecaseLabel(): string {
  return $localize`:use case missing in a version@@item.missingUsecase:(this use case does not exist in this version)`;
}

export function missingTestsLabel(): string {
  return $localize`:test suite missing in a version@@item.missingTests:(this test suite does not exist in this version)`;
}
