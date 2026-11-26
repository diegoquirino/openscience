/**
 * Display model of a full use case text (spec 008, FR-003 and FR-010). The stored text is never
 * changed: the `<- edited` marker is only left out of what is shown and the pipeline text of a
 * missing version is only replaced when the screen renders it. CLARET keywords vs quoted content
 * are tokenized for IDE-style highlighting (spec 036).
 */
export const MISSING_USECASE_TEXT = '(the use case does not exist in this version)';
export const EDITED_MARKER = '<- edited';

/** Thesis `lstdefinestyle{CLARET}` keywords (case-sensitive). */
export const CLARET_KEYWORDS = [
  'systemName',
  'usecase',
  'actor',
  'preCondition',
  'postCondition',
  'basic',
  'alternative',
  'exception',
  'step',
  'af',
  'ef',
  'bs',
] as const;

export type ClaretTokenKind = 'keyword' | 'string' | 'text';

export interface ClaretToken {
  kind: ClaretTokenKind;
  text: string;
}

export interface UseCaseLine {
  /** Leading whitespace columns (a tab counts as 4), shown as a hanging indent. */
  indent: number;
  /** The line without its indentation and without the trailing marker. */
  content: string;
  edited: boolean;
  /** IDE-style CLARET tokens for display (spec 036). */
  tokens: ClaretToken[];
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

/** Longest-first so `systemName` wins over a shorter prefix if one were added later. */
const KEYWORD_PATTERN = [...CLARET_KEYWORDS].sort((a, b) => b.length - a.length).join('|');
const TOKEN_RE = new RegExp(`"(?:[^"]*)"?|\\b(?:${KEYWORD_PATTERN})\\b`, 'g');

/**
 * Split a visible CLARET line into keywords, quoted strings and plain text. Keywords inside
 * quotes are not matched.
 */
export function tokenizeClaret(content: string): ClaretToken[] {
  if (content === '') {
    return [];
  }
  const tokens: ClaretToken[] = [];
  let last = 0;
  for (const match of content.matchAll(TOKEN_RE)) {
    const index = match.index ?? 0;
    if (index > last) {
      tokens.push({ kind: 'text', text: content.slice(last, index) });
    }
    const piece = match[0];
    if (piece.startsWith('"')) {
      tokens.push({ kind: 'string', text: piece });
    } else {
      tokens.push({ kind: 'keyword', text: piece });
    }
    last = index + piece.length;
  }
  if (last < content.length) {
    tokens.push({ kind: 'text', text: content.slice(last) });
  }
  return tokens;
}

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
  const content = visible.slice(leading.length);
  return { indent, content, edited, tokens: tokenizeClaret(content) };
}
