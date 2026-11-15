/**
 * Display labels of the professional skill codes (Appendix B.4 of spec 001), in the order of the
 * appendix. The code is what the API stores; only the label shown to people is translated. The
 * `Record` type makes the compiler flag a code without label.
 */
export const SKILLS = [
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
] as const;
export type Skill = (typeof SKILLS)[number];

/** Longest description of "Other", counted in code points as the API does. */
export const SKILL_OTHER_MAX = 100;

const SKILL_LABELS: Record<Skill, () => string> = {
  developer: () => $localize`:professional skill developer@@skill.developer:Software developer`,
  software_engineer: () => $localize`:professional skill software engineer@@skill.software_engineer:Software engineer`,
  systems_analyst: () => $localize`:professional skill systems analyst@@skill.systems_analyst:Systems analyst`,
  requirements_analyst: () =>
    $localize`:professional skill requirements analyst@@skill.requirements_analyst:Requirements or business analyst`,
  test_analyst: () => $localize`:professional skill test analyst@@skill.test_analyst:Test analyst / QA`,
  test_engineer: () =>
    $localize`:professional skill test engineer@@skill.test_engineer:Test or test automation engineer`,
  software_architect: () => $localize`:professional skill software architect@@skill.software_architect:Software architect`,
  tech_lead: () => $localize`:professional skill tech lead@@skill.tech_lead:Tech lead`,
  product_project_manager: () =>
    $localize`:professional skill product or project manager@@skill.product_project_manager:Product owner, product manager or project manager`,
  professor_researcher: () =>
    $localize`:professional skill professor or researcher@@skill.professor_researcher:Computing professor or researcher`,
  graduate_student: () =>
    $localize`:professional skill graduate student@@skill.graduate_student:Graduate student in computing`,
  undergraduate_student: () =>
    $localize`:professional skill undergraduate student@@skill.undergraduate_student:Undergraduate student in computing`,
  other: () => $localize`:professional skill other@@skill.other:Other (describe)`,
};

export function isSkill(value: unknown): value is Skill {
  return typeof value === 'string' && (SKILLS as readonly string[]).includes(value);
}

/** Label in the language of the build; an unknown code is shown as is. */
export function skillLabel(code: string): string {
  return isSkill(code) ? SKILL_LABELS[code]() : code;
}

/** Number of characters as the API counts them (code points, not UTF-16 units). */
export function codePoints(text: string): number {
  return Array.from(text).length;
}

/** The skill form is valid with an option and, for "Other", a non-blank description of at most 100 characters. */
export function skillAnswerValid(skill: string | null, other: string): boolean {
  if (!isSkill(skill)) {
    return false;
  }
  return skill !== 'other' || (other.trim().length > 0 && codePoints(other) <= SKILL_OTHER_MAX);
}
