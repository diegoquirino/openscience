/**
 * Display model of a full use case text (spec 008, FR-003 and FR-010). The stored text is never
 * changed: the `<- edited` marker is only left out of what is shown and the pipeline text of a
 * missing version is only replaced when the screen renders it.
 */
export const MISSING_USECASE_TEXT = '(the use case does not exist in this version)';
export const EDITED_MARKER = '<- edited';

export interface UseCaseLine {
  /** Leading whitespace columns (a tab counts as 4), shown as a hanging indent. */
  indent: number;
  /** The line without its indentation and without the trailing marker. */
  content: string;
  edited: boolean;
}

export interface UseCaseView {
  /** The whole text is the pipeline text for a use case that does not exist in this version. */
  missing: boolean;
  lines: UseCaseLine[];
  /** Index in `lines` of the first edited line, `-1` when none. */
  firstEditedIndex: number;
}

const TAB_COLUMNS = 4;
const MARKER = /[ \t]*<- edited[ \t]*$/;

export function parseUseCase(text: string | null | undefined): UseCaseView {
  if (text === null || text === undefined || text.trim() === '') {
    return { missing: false, lines: [], firstEditedIndex: -1 };
  }
  if (text.trim() === MISSING_USECASE_TEXT) {
    return { missing: true, lines: [], firstEditedIndex: -1 };
  }

  const rows = text.split(/\r\n|\r|\n/);
  if (rows.length > 1 && rows[rows.length - 1] === '') {
    rows.pop();
  }
  const lines = rows.map(parseLine);
  return { missing: false, lines, firstEditedIndex: lines.findIndex((line) => line.edited) };
}

function parseLine(row: string): UseCaseLine {
  const edited = MARKER.test(row);
  const visible = edited ? row.replace(MARKER, '') : row;
  const leading = /^[ \t]*/.exec(visible)?.[0] ?? '';
  let indent = 0;
  for (const character of leading) {
    indent += character === '\t' ? TAB_COLUMNS : 1;
  }
  return { indent, content: visible.slice(leading.length), edited };
}
