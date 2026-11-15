/**
 * Display labels of the stored codes of a response (Appendices B.1 and B.2 of spec 001). The codes
 * are what the API stores; only the label shown to people is translated.
 */
export const CHANGE_IMPACTS = ['LOW', 'HIGH'] as const;
export type ChangeImpact = (typeof CHANGE_IMPACTS)[number];

export const MAINTENANCE_OPERATIONS = [
  'KEEP',
  'UPDATE',
  'REMOVE',
  'CREATE',
  'MERGE',
  'SPLIT',
  'REASSIGN',
  'FLAG',
] as const;
export type MaintenanceOperation = (typeof MAINTENANCE_OPERATIONS)[number];

const IMPACT_LABELS: Record<ChangeImpact, () => string> = {
  LOW: () => $localize`:change impact code LOW@@changeImpact.LOW:Low`,
  HIGH: () => $localize`:change impact code HIGH@@changeImpact.HIGH:High`,
};

const OPERATION_LABELS: Record<MaintenanceOperation, () => string> = {
  KEEP: () => $localize`:maintenance operation code KEEP@@maintenanceOperation.KEEP:Keep`,
  UPDATE: () => $localize`:maintenance operation code UPDATE@@maintenanceOperation.UPDATE:Update`,
  REMOVE: () => $localize`:maintenance operation code REMOVE@@maintenanceOperation.REMOVE:Remove`,
  CREATE: () => $localize`:maintenance operation code CREATE@@maintenanceOperation.CREATE:Create`,
  MERGE: () => $localize`:maintenance operation code MERGE@@maintenanceOperation.MERGE:Merge`,
  SPLIT: () => $localize`:maintenance operation code SPLIT@@maintenanceOperation.SPLIT:Split`,
  REASSIGN: () => $localize`:maintenance operation code REASSIGN@@maintenanceOperation.REASSIGN:Reassign`,
  FLAG: () => $localize`:maintenance operation code FLAG@@maintenanceOperation.FLAG:Flag`,
};

export function isChangeImpact(value: unknown): value is ChangeImpact {
  return typeof value === 'string' && (CHANGE_IMPACTS as readonly string[]).includes(value);
}

export function isMaintenanceOperation(value: unknown): value is MaintenanceOperation {
  return typeof value === 'string' && (MAINTENANCE_OPERATIONS as readonly string[]).includes(value);
}

/** Label in the language of the build; an unknown code is shown as is. */
export function changeImpactLabel(code: string): string {
  return isChangeImpact(code) ? IMPACT_LABELS[code]() : code;
}

/** Label in the language of the build; an unknown code is shown as is. */
export function maintenanceOperationLabel(code: string): string {
  return isMaintenanceOperation(code) ? OPERATION_LABELS[code]() : code;
}
