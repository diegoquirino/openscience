/**
 * Word-level difference between the `origin` and the `target` of an edit (spec 008, FR-002).
 * Computed at display time only; nothing computed here is stored or sent.
 */
export type DiffKind = 'same' | 'removed' | 'added';

export interface DiffSegment {
  kind: DiffKind;
  text: string;
}

/**
 * Above this many token pairs the longest common subsequence is skipped and the whole origin is
 * reported as removed and the whole target as added, so the screen never freezes.
 */
export const DIFF_CELL_LIMIT = 1_000_000;

const TOKEN = /\s+|[\p{L}\p{N}_]+|[^\s\p{L}\p{N}_]/gu;

/** Words, single punctuation characters and runs of whitespace (a line break is whitespace). */
export function tokenize(text: string): string[] {
  return text.match(TOKEN) ?? [];
}

export function diffWords(origin: string, target: string): DiffSegment[] {
  const before = tokenize(origin);
  const after = tokenize(target);

  let start = 0;
  while (start < before.length && start < after.length && before[start] === after[start]) {
    start++;
  }
  let endBefore = before.length;
  let endAfter = after.length;
  while (endBefore > start && endAfter > start && before[endBefore - 1] === after[endAfter - 1]) {
    endBefore--;
    endAfter--;
  }

  const builder = new SegmentBuilder();
  builder.add('same', before.slice(0, start));
  const middleBefore = before.slice(start, endBefore);
  const middleAfter = after.slice(start, endAfter);

  if (middleBefore.length * middleAfter.length > DIFF_CELL_LIMIT) {
    builder.add('removed', middleBefore);
    builder.add('added', middleAfter);
  } else {
    diffMiddle(middleBefore, middleAfter, builder);
  }

  builder.add('same', before.slice(endBefore));
  return builder.segments;
}

/** The segments of the "Before" side: what is on both sides plus what was removed. */
export function originSegments(segments: DiffSegment[]): DiffSegment[] {
  return segments.filter((segment) => segment.kind !== 'added');
}

/** The segments of the "After" side: what is on both sides plus what was added. */
export function targetSegments(segments: DiffSegment[]): DiffSegment[] {
  return segments.filter((segment) => segment.kind !== 'removed');
}

/** The whole text as one segment of `kind` (ADD and DEL show the whole text as the change). */
export function wholeText(text: string, kind: DiffKind): DiffSegment[] {
  return text === '' ? [] : [{ kind, text }];
}

function diffMiddle(before: string[], after: string[], builder: SegmentBuilder): void {
  const rows = before.length;
  const columns = after.length;
  if (rows === 0 || columns === 0) {
    builder.add('removed', before);
    builder.add('added', after);
    return;
  }

  // common[i * width + j] = length of the longest common subsequence of before[i..] and after[j..]
  const width = columns + 1;
  const common = new Uint32Array((rows + 1) * width);
  for (let i = rows - 1; i >= 0; i--) {
    for (let j = columns - 1; j >= 0; j--) {
      common[i * width + j] =
        before[i] === after[j]
          ? common[(i + 1) * width + j + 1] + 1
          : Math.max(common[(i + 1) * width + j], common[i * width + j + 1]);
    }
  }

  let i = 0;
  let j = 0;
  while (i < rows && j < columns) {
    if (before[i] === after[j]) {
      builder.add('same', [before[i]]);
      i++;
      j++;
    } else if (common[(i + 1) * width + j] >= common[i * width + j + 1]) {
      builder.add('removed', [before[i]]);
      i++;
    } else {
      builder.add('added', [after[j]]);
      j++;
    }
  }
  builder.add('removed', before.slice(i));
  builder.add('added', after.slice(j));
}

/** Collects tokens into segments, merging neighbours of the same kind. */
class SegmentBuilder {
  readonly segments: DiffSegment[] = [];

  add(kind: DiffKind, tokens: string[]): void {
    if (tokens.length === 0) {
      return;
    }
    const text = tokens.join('');
    const last = this.segments[this.segments.length - 1];
    if (last && last.kind === kind) {
      last.text += text;
    } else {
      this.segments.push({ kind, text });
    }
  }
}
