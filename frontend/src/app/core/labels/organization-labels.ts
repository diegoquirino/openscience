/** Organization / employer codes (spec 025). English identifiers; UI labels translated. */
export const ORGANIZATIONS = ['tjpb', 'ufcg', 'other'] as const;
export type Organization = (typeof ORGANIZATIONS)[number];

export const ORGANIZATION_OTHER_MAX = 100;

const ORGANIZATION_LABELS: Record<Organization, () => string> = {
  tjpb: () =>
    $localize`:organization tjpb@@organization.tjpb:TJPB - Tribunal de Justiça do Estado da Paraíba`,
  ufcg: () =>
    $localize`:organization ufcg@@organization.ufcg:UFCG - Universidade Federal de Campina Grande`,
  other: () => $localize`:organization other@@organization.other:Other`,
};

export function isOrganization(value: unknown): value is Organization {
  return typeof value === 'string' && (ORGANIZATIONS as readonly string[]).includes(value);
}

export function organizationLabel(code: string): string {
  return isOrganization(code) ? ORGANIZATION_LABELS[code]() : code;
}

export function organizationAnswerValid(organization: string | null, other: string): boolean {
  if (!isOrganization(organization)) {
    return false;
  }
  if (organization !== 'other') {
    return true;
  }
  const trimmed = other.trim();
  return trimmed.length > 0 && Array.from(other).length <= ORGANIZATION_OTHER_MAX;
}
