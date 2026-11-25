import { fieldLabel, kindLabel } from '../../core/labels/item-labels';
import { ItemBody } from '../label.service';
import { parseUseCase, UseCaseView } from './usecase-text';

/** Which title a column shows; the template maps each one to its localized text. */
export type ColumnHeading = 'before' | 'after' | 'added' | 'removed' | 'metaBefore' | 'metaAfter';

export interface ItemColumn {
  /** `origin` is the previous version (left), `target` the modified one (right). */
  side: 'origin' | 'target';
  heading: ColumnHeading;
  /** Full use case of this version, with `<- edited` highlights (display-only, spec 034). */
  usecase: UseCaseView;
  /** Version label of the use case this column's button opens, as stored. */
  versionLabel: string;
}

export interface ItemHeader {
  system: string;
  uc: string;
  fieldLabel: string;
  kindLabel: string;
}

export interface ItemLayout {
  header: ItemHeader;
  columns: [ItemColumn, ItemColumn];
  /** Both full use case texts are equal (after storage as-is). */
  identical: boolean;
}

type LayoutInput = Pick<
  ItemBody,
  | 'system'
  | 'uc'
  | 'kind'
  | 'field'
  | 'origin'
  | 'target'
  | 'originVersion'
  | 'targetVersion'
  | 'originUsecase'
  | 'targetUsecase'
>;

const NO_VERSION = '-';

/**
 * What the item screen shows for the edit of an item (specs 008 + 034): each side is the full use
 * case of that version, with pipeline highlights — not only the field fragment.
 */
export function buildItemLayout(item: LayoutInput): ItemLayout {
  const originVersion = item.originVersion ?? NO_VERSION;
  const targetVersion = item.targetVersion ?? NO_VERSION;
  const originUsecase = item.originUsecase ?? '';
  const targetUsecase = item.targetUsecase ?? '';
  const header: ItemHeader = {
    system: item.system,
    uc: item.uc,
    fieldLabel: fieldLabel(item.field),
    kindLabel: kindLabel(item.kind),
  };

  const originHeading = headingFor('origin', item.kind);
  const targetHeading = headingFor('target', item.kind);

  return {
    header,
    identical: originUsecase === targetUsecase,
    columns: [
      {
        side: 'origin',
        heading: originHeading,
        usecase: parseUseCase(originUsecase),
        versionLabel: originVersion,
      },
      {
        side: 'target',
        heading: targetHeading,
        usecase: parseUseCase(targetUsecase),
        versionLabel: targetVersion,
      },
    ],
  };
}

function headingFor(side: 'origin' | 'target', kind: string): ColumnHeading {
  if (kind === 'ADD') {
    return side === 'target' ? 'added' : 'before';
  }
  if (kind === 'DEL') {
    return side === 'origin' ? 'removed' : 'after';
  }
  if (kind === 'META') {
    return side === 'origin' ? 'metaBefore' : 'metaAfter';
  }
  return side === 'origin' ? 'before' : 'after';
}
