import {
  ITEM_FIELDS,
  ITEM_KINDS,
  fieldLabel,
  isItemField,
  isItemKind,
  kindLabel,
  missingUsecaseLabel,
  noneLabel,
} from './item-labels';

describe('item-labels', () => {
  it('labels every kind and field of Appendix A (source language of the test build)', () => {
    expect(ITEM_KINDS.map(kindLabel)).toEqual(['Modification', 'Addition', 'Removal', 'Metadata']);
    expect(fieldLabel('exc_flow_step')).toBe('Exception flow step');
    expect(fieldLabel('requirements')).toBe('Requirements link');
    for (const field of ITEM_FIELDS) {
      expect(fieldLabel(field)).not.toBe(field);
    }
  });

  it('shows an unknown kind or field with its original name', () => {
    expect(kindLabel('OTHER')).toBe('OTHER');
    expect(fieldLabel('custom_field')).toBe('custom_field');
    expect(fieldLabel('constructor')).toBe('constructor');
    expect(isItemKind('constructor')).toBe(false);
    expect(isItemField('toString')).toBe(false);
  });

  it('has the stand-ins for the empty side and the missing version', () => {
    expect(noneLabel()).toBe('(none)');
    expect(missingUsecaseLabel()).toBe('(this use case does not exist in this version)');
  });
});
