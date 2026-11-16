import { fieldLabel, kindLabel } from '../../core/labels/item-labels';
import { ItemBody } from '../label.service';
import { DiffSegment, diffWords, originSegments, targetSegments, wholeText } from './word-diff';

/** Which title a column shows; the template maps each one to its localized text. */
export type ColumnHeading = 'before' | 'after' | 'added' | 'removed' | 'metaBefore' | 'metaAfter';

/** How the text of a column is marked: word diff, or the whole text as added or as removed. */
export type ColumnMarking = 'diff' | 'added' | 'removed';

export interface ItemColumn {
  /** `origin` is the previous version (left), `target` the modified one (right). */
  side: 'origin' | 'target';
  /** `null` when the column holds only the use case button (ADD on the left, DEL on the right). */
  heading: ColumnHeading | null;
  /** `null` together with `heading`; otherwise the text with its highlighted words. */
  segments: DiffSegment[] | null;
  marking: ColumnMarking;
  /** The text is empty: the screen shows the localized "(none)". */
  empty: boolean;
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
  /** `origin` equals `target` where both are compared: the screen warns that nothing differs. */
  identical: boolean;
}

type LayoutInput = Pick<
  ItemBody,
  'system' | 'uc' | 'kind' | 'field' | 'origin' | 'target' | 'originVersion' | 'targetVersion'
>;

const NO_VERSION = '-';

/** What the item screen shows for the edit of an item, by kind (spec 008, User Story 1). */
export function buildItemLayout(item: LayoutInput): ItemLayout {
  const origin = item.origin ?? '';
  const target = item.target ?? '';
  const originVersion = item.originVersion ?? NO_VERSION;
  const targetVersion = item.targetVersion ?? NO_VERSION;
  const header: ItemHeader = {
    system: item.system,
    uc: item.uc,
    fieldLabel: fieldLabel(item.field),
    kindLabel: kindLabel(item.kind),
  };

  if (item.kind === 'ADD') {
    return {
      header,
      identical: false,
      columns: [
        { side: 'origin', heading: null, segments: null, marking: 'diff', empty: false, versionLabel: originVersion },
        {
          side: 'target',
          heading: 'added',
          segments: wholeText(target, 'added'),
          marking: 'added',
          empty: target === '',
          versionLabel: targetVersion,
        },
      ],
    };
  }

  if (item.kind === 'DEL') {
    return {
      header,
      identical: false,
      columns: [
        {
          side: 'origin',
          heading: 'removed',
          segments: wholeText(origin, 'removed'),
          marking: 'removed',
          empty: origin === '',
          versionLabel: originVersion,
        },
        { side: 'target', heading: null, segments: null, marking: 'diff', empty: false, versionLabel: targetVersion },
      ],
    };
  }

  const segments = diffWords(origin, target);
  const meta = item.kind === 'META';
  return {
    header,
    identical: origin === target,
    columns: [
      {
        side: 'origin',
        heading: meta ? 'metaBefore' : 'before',
        segments: originSegments(segments),
        marking: 'diff',
        empty: origin === '',
        versionLabel: originVersion,
      },
      {
        side: 'target',
        heading: meta ? 'metaAfter' : 'after',
        segments: targetSegments(segments),
        marking: 'diff',
        empty: target === '',
        versionLabel: targetVersion,
      },
    ],
  };
}
