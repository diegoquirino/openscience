/**
 * Display labels of stored codes. The code (`pilot`, `collection`) is what the API and the
 * database keep; only the label shown to people is translated. Later code lists follow this
 * pattern: `change_impact`, `maintenance_operation`, `professional_skill` and screening codes.
 */
export type StudyPhase = 'pilot' | 'collection';

const LABELS: Record<StudyPhase, () => string> = {
  pilot: () => $localize`:study phase code pilot@@studyPhase.pilot:pilot`,
  collection: () => $localize`:study phase code collection@@studyPhase.collection:collection`,
};

/** Label in the language of the build; an unknown code is shown as is. */
export function studyPhaseLabel(code: string): string {
  return Object.hasOwn(LABELS, code) ? LABELS[code as StudyPhase]() : code;
}
