import { DIFF_CELL_LIMIT, diffWords, originSegments, targetSegments, tokenize, wholeText } from './word-diff';

function sides(origin: string, target: string) {
  const segments = diffWords(origin, target);
  return {
    segments,
    before: originSegments(segments)
      .map((s) => s.text)
      .join(''),
    after: targetSegments(segments)
      .map((s) => s.text)
      .join(''),
  };
}

describe('word-diff', () => {
  it('tokenizes words, punctuation and whitespace runs, keeping everything', () => {
    const text = 'alerts: user  does\nnot exist!';
    expect(tokenize(text)).toEqual(['alerts', ':', ' ', 'user', '  ', 'does', '\n', 'not', ' ', 'exist', '!']);
    expect(tokenize(text).join('')).toBe(text);
    expect(tokenize('')).toEqual([]);
  });

  it('marks a replaced word as removed and added and keeps the rest', () => {
    const { segments, before, after } = sides('launches the login screen', 'displays the login screen');
    expect(segments).toEqual([
      { kind: 'removed', text: 'launches' },
      { kind: 'added', text: 'displays' },
      { kind: 'same', text: ' the login screen' },
    ]);
    expect(before).toBe('launches the login screen');
    expect(after).toBe('displays the login screen');
  });

  it('reconstructs both sides exactly, line breaks included', () => {
    const origin = 'step 1\nstep 2 system "a"\n  step 3';
    const target = 'step 1\nstep 2 system "b c"\n  step 3\nstep 4';
    const { before, after, segments } = sides(origin, target);
    expect(before).toBe(origin);
    expect(after).toBe(target);
    for (let i = 1; i < segments.length; i++) {
      expect(segments[i].kind).not.toBe(segments[i - 1].kind);
    }
  });

  it('shows a change of only punctuation', () => {
    const { segments } = sides('alerts user does not exist', 'alerts: user does not exist');
    expect(segments.filter((s) => s.kind === 'added')).toEqual([{ kind: 'added', text: ':' }]);
    expect(segments.filter((s) => s.kind === 'removed')).toEqual([]);
  });

  it('shows a change of only spaces', () => {
    const { segments, before, after } = sides('a b', 'a  b');
    expect(segments.some((s) => s.kind === 'removed')).toBe(true);
    expect(segments.some((s) => s.kind === 'added')).toBe(true);
    expect(before).toBe('a b');
    expect(after).toBe('a  b');
  });

  it('reports identical texts as one unchanged segment', () => {
    expect(diffWords('same text', 'same text')).toEqual([{ kind: 'same', text: 'same text' }]);
    expect(diffWords('', '')).toEqual([]);
  });

  it('handles an empty side', () => {
    expect(diffWords('', 'REQ-07')).toEqual([{ kind: 'added', text: 'REQ-07' }]);
    expect(diffWords('REQ-07', '')).toEqual([{ kind: 'removed', text: 'REQ-07' }]);
  });

  it('shows added requirements after the common prefix', () => {
    const { segments } = sides('REQ-07', 'REQ-07, REQ-08, REQ-09');
    expect(segments[0]).toEqual({ kind: 'same', text: 'REQ-07' });
    expect(segments[1]).toEqual({ kind: 'added', text: ', REQ-08, REQ-09' });
  });

  it('falls back to the whole text changed above the size limit', () => {
    const half = Math.ceil(Math.sqrt(DIFF_CELL_LIMIT)) + 5;
    const origin = Array.from({ length: half }, (_, i) => `a${i}`).join(' ');
    const target = Array.from({ length: half }, (_, i) => `b${i}`).join(' ');
    const segments = diffWords(origin, target);
    expect(segments).toEqual([
      { kind: 'removed', text: origin },
      { kind: 'added', text: target },
    ]);
  });

  it('builds the whole-text segment of ADD and DEL', () => {
    expect(wholeText('new flow', 'added')).toEqual([{ kind: 'added', text: 'new flow' }]);
    expect(wholeText('', 'removed')).toEqual([]);
  });
});
