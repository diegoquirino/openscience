import { MISSING_USECASE_TEXT, parseUseCase, tokenizeClaret } from './usecase-text';

describe('usecase-text', () => {
  const text = [
    'usecase "Login User" {',
    '  basic {',
    '    step 1 emailUser "launches the login screen"   <- edited',
    '    step 2 system "presents a form"',
    '  }',
    '\tstep 9 "tab"   <- edited',
    '}',
  ].join('\n');

  it('removes the edited marker, flags the line and keeps the indentation', () => {
    const view = parseUseCase(text);

    expect(view.missing).toBe(false);
    expect(view.lines).toHaveLength(7);
    expect(view.lines[0]).toMatchObject({ indent: 0, content: 'usecase "Login User" {', edited: false });
    expect(view.lines[2]).toMatchObject({
      indent: 4,
      content: 'step 1 emailUser "launches the login screen"',
      edited: true,
    });
    expect(view.lines[5]).toMatchObject({ indent: 4, content: 'step 9 "tab"', edited: true });
    expect(view.lines.some((line) => line.content.includes('<- edited'))).toBe(false);
    expect(view.firstEditedIndex).toBe(2);
  });

  it('has no first edited line when nothing is marked', () => {
    const view = parseUseCase('usecase "A" {\n  step 1 "x"\n}');
    expect(view.firstEditedIndex).toBe(-1);
    expect(view.lines.every((line) => !line.edited)).toBe(true);
  });

  it('keeps blank lines and handles CRLF and a trailing newline', () => {
    const view = parseUseCase('a\r\n\r\n  b\r\n');
    expect(view.lines.map((line) => [line.indent, line.content])).toEqual([
      [0, 'a'],
      [0, ''],
      [2, 'b'],
    ]);
  });

  it('flags the pipeline text only when it is the whole text', () => {
    expect(parseUseCase(MISSING_USECASE_TEXT)).toEqual({ missing: true, lines: [], firstEditedIndex: -1 });
    expect(parseUseCase(`\n  ${MISSING_USECASE_TEXT}  \n`).missing).toBe(true);

    const inside = parseUseCase(`usecase "A" {\n  ${MISSING_USECASE_TEXT}\n}`);
    expect(inside.missing).toBe(false);
    expect(inside.lines[1].content).toBe(MISSING_USECASE_TEXT);
  });

  it('shows nothing for an absent text', () => {
    expect(parseUseCase(null)).toEqual({ missing: false, lines: [], firstEditedIndex: -1 });
    expect(parseUseCase('')).toEqual({ missing: false, lines: [], firstEditedIndex: -1 });
  });
});

describe('tokenizeClaret', () => {
  it('marks CLARET keywords and quoted strings separately from actor ids', () => {
    expect(tokenizeClaret('step 1 emailUser "launches the login screen"')).toEqual([
      { kind: 'keyword', text: 'step' },
      { kind: 'text', text: ' 1 emailUser ' },
      { kind: 'string', text: '"launches the login screen"' },
    ]);
  });

  it('does not treat keywords inside quotes as keywords', () => {
    expect(tokenizeClaret('actor emailUser "step basic usecase"')).toEqual([
      { kind: 'keyword', text: 'actor' },
      { kind: 'text', text: ' emailUser ' },
      { kind: 'string', text: '"step basic usecase"' },
    ]);
  });

  it('highlights flow refs af ef bs and braces stay as text', () => {
    expect(tokenizeClaret('step 4 system "ok" ef[1,2]')).toEqual([
      { kind: 'keyword', text: 'step' },
      { kind: 'text', text: ' 4 system ' },
      { kind: 'string', text: '"ok"' },
      { kind: 'text', text: ' ' },
      { kind: 'keyword', text: 'ef' },
      { kind: 'text', text: '[1,2]' },
    ]);
  });

  it('is case-sensitive for keywords', () => {
    expect(tokenizeClaret('Step 1 "x"')).toEqual([
      { kind: 'text', text: 'Step 1 ' },
      { kind: 'string', text: '"x"' },
    ]);
  });
});
