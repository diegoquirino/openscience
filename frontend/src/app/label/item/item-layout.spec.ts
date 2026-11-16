import { buildItemLayout } from './item-layout';

const base = {
  system: 'minitest',
  uc: 'Login User',
  field: 'exc_flow_step',
  originVersion: 'v1.1',
  targetVersion: 'v1.2',
  origin: 'alerts that user does not exist',
  target: 'alerts: user does not exist',
};

describe('buildItemLayout', () => {
  it('lays out MOD as two compared blocks with the labels of the header', () => {
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
    expect(layout.columns[0].segments?.some((s) => s.kind === 'removed')).toBe(true);
    expect(layout.columns[0].segments?.some((s) => s.kind === 'added')).toBe(false);
    expect(layout.columns[1].segments?.some((s) => s.kind === 'added')).toBe(true);
    expect(layout.identical).toBe(false);
  });

  it('shows only the added text for ADD, with the first column holding just the button', () => {
    const layout = buildItemLayout({ ...base, kind: 'ADD', origin: '', target: 'exception 3 "New"\n  step 1' });

    expect(layout.columns[0]).toMatchObject({ side: 'origin', heading: null, segments: null, versionLabel: 'v1.1' });
    expect(layout.columns[1]).toMatchObject({ side: 'target', heading: 'added', marking: 'added', empty: false });
    expect(layout.columns[1].segments).toEqual([{ kind: 'added', text: 'exception 3 "New"\n  step 1' }]);
    expect(layout.identical).toBe(false);
  });

  it('shows only the removed text for DEL, with the second column holding just the button', () => {
    const layout = buildItemLayout({ ...base, kind: 'DEL', origin: 'exception 5 "Old"', target: '' });

    expect(layout.columns[0]).toMatchObject({ side: 'origin', heading: 'removed', marking: 'removed', empty: false });
    expect(layout.columns[0].segments).toEqual([{ kind: 'removed', text: 'exception 5 "Old"' }]);
    expect(layout.columns[1]).toMatchObject({ side: 'target', heading: null, segments: null, versionLabel: 'v1.2' });
  });

  it('shows the requirements link before and after for META, with an empty side flagged', () => {
    const layout = buildItemLayout({ ...base, kind: 'META', field: 'requirements', origin: '', target: 'REQ-07' });

    expect(layout.header.fieldLabel).toBe('Requirements link');
    expect(layout.header.kindLabel).toBe('Metadata');
    expect(layout.columns.map((column) => column.heading)).toEqual(['metaBefore', 'metaAfter']);
    expect(layout.columns[0]).toMatchObject({ empty: true, segments: [] });
    expect(layout.columns[1]).toMatchObject({ empty: false });
    expect(layout.columns[1].segments).toEqual([{ kind: 'added', text: 'REQ-07' }]);
  });

  it('flags identical texts of a compared layout', () => {
    expect(buildItemLayout({ ...base, kind: 'META', field: 'requirements', origin: 'REQ-07', target: 'REQ-07' }).identical).toBe(true);
    expect(buildItemLayout({ ...base, kind: 'MOD', origin: 'same', target: 'same' }).identical).toBe(true);
  });

  it('treats null texts as empty and an unknown kind like MOD with the original names', () => {
    const layout = buildItemLayout({
      ...base,
      kind: 'REWRITE',
      field: 'custom_field',
      origin: null,
      target: 'new',
      originVersion: null,
    });

    expect(layout.header.kindLabel).toBe('REWRITE');
    expect(layout.header.fieldLabel).toBe('custom_field');
    expect(layout.columns.map((column) => column.heading)).toEqual(['before', 'after']);
    expect(layout.columns[0]).toMatchObject({ empty: true, versionLabel: '-' });
  });
});
