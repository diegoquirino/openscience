import { MISSING_USECASE_TEXT } from './usecase-text';
import { buildItemLayout } from './item-layout';

const base = {
  system: 'minitest',
  uc: 'Login User',
  field: 'exc_flow_step',
  originVersion: 'v1.1',
  targetVersion: 'v1.2',
  origin: 'alerts that user does not exist',
  target: 'alerts: user does not exist',
  originUsecase: 'usecase "Login User" {\n  step 1 "A" <- edited\n}',
  targetUsecase: 'usecase "Login User" {\n  step 1 "B" <- edited\n}',
  originTests: 'Test Case ID: tc-1\nSummary: A   <- edited',
  targetTests: 'Test Case ID: tc-1\nSummary: B   <- edited',
};

describe('buildItemLayout', () => {
  it('lays out MOD with full use cases and edited highlights on both sides', () => {
    const layout = buildItemLayout({ ...base, kind: 'MOD' });

    expect(layout.header).toEqual({
      system: 'minitest',
      uc: 'Login User',
      fieldLabel: 'Exception flow step',
      kindLabel: 'Modification',
    });
    expect(layout.columns.map((column) => [column.side, column.heading, column.versionLabel])).toEqual([
      ['origin', 'before', 'v1.1'],
      ['target', 'after', 'v1.2'],
    ]);
    expect(layout.columns[0].usecase.lines.some((line) => line.edited && line.content.includes('"A"'))).toBe(true);
    expect(layout.columns[1].usecase.lines.some((line) => line.edited && line.content.includes('"B"'))).toBe(true);
    expect(layout.identical).toBe(false);
  });

  it('shows full use cases for ADD and DEL on both columns', () => {
    const add = buildItemLayout({
      ...base,
      kind: 'ADD',
      origin: '',
      target: 'exception 3 "New"',
      originUsecase: MISSING_USECASE_TEXT,
      targetUsecase: 'usecase "X" {\n  exception 3 "New" <- edited\n}',
    });
    expect(add.columns[0]).toMatchObject({ side: 'origin', heading: 'before', versionLabel: 'v1.1' });
    expect(add.columns[0].usecase.missing).toBe(true);
    expect(add.columns[1].heading).toBe('added');
    expect(add.columns[1].usecase.lines.some((line) => line.edited)).toBe(true);

    const del = buildItemLayout({
      ...base,
      kind: 'DEL',
      origin: 'exception 5 "Old"',
      target: '',
      originUsecase: 'usecase "X" {\n  exception 5 "Old" <- edited\n}',
      targetUsecase: MISSING_USECASE_TEXT,
    });
    expect(del.columns[0].heading).toBe('removed');
    expect(del.columns[1].heading).toBe('after');
    expect(del.columns[1].usecase.missing).toBe(true);
  });

  it('shows META headings with full use cases of each version', () => {
    const layout = buildItemLayout({
      ...base,
      kind: 'META',
      field: 'requirements',
      origin: '',
      target: 'REQ-07',
    });
    expect(layout.columns.map((column) => column.heading)).toEqual(['metaBefore', 'metaAfter']);
    expect(layout.columns[0].usecase.lines.length).toBeGreaterThan(0);
    expect(layout.columns[1].usecase.lines.length).toBeGreaterThan(0);
  });

  it('flags identical full use case texts', () => {
    expect(
      buildItemLayout({
        ...base,
        kind: 'MOD',
        originUsecase: 'same uc',
        targetUsecase: 'same uc',
      }).identical,
    ).toBe(true);
  });

  it('uses "-" when a version label is null', () => {
    const layout = buildItemLayout({
      ...base,
      kind: 'MOD',
      originVersion: null,
      originUsecase: null,
      targetUsecase: 'new',
    });
    expect(layout.columns[0].versionLabel).toBe('-');
    expect(layout.columns[0].usecase.lines).toEqual([]);
  });
});
