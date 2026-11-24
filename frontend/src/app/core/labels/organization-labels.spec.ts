import {
  filterOrganizationSuggestions,
  ORGANIZATION_SUGGESTION_MIN_CHARS,
} from './organization-labels';

describe('filterOrganizationSuggestions', () => {
  const all = [
    'UFPB - Universidade Federal da Paraíba',
    'IFPB - Instituto Federal da Paraíba',
    'Viceri Solution Ltda',
    'Ingenico do Brasil Ltda',
    'Acme Ltd',
  ];

  it('requires at least three characters before listing matches', () => {
    expect(ORGANIZATION_SUGGESTION_MIN_CHARS).toBe(3);
    expect(filterOrganizationSuggestions('', all)).toEqual([]);
    expect(filterOrganizationSuggestions('V', all)).toEqual([]);
    expect(filterOrganizationSuggestions('Vi', all)).toEqual([]);
  });

  it('filters case-insensitively once the query is long enough', () => {
    expect(filterOrganizationSuggestions('Vic', all)).toEqual(['Viceri Solution Ltda']);
    expect(filterOrganizationSuggestions('ufp', all)).toEqual([
      'UFPB - Universidade Federal da Paraíba',
    ]);
    expect(filterOrganizationSuggestions('Para', all)).toEqual([
      'UFPB - Universidade Federal da Paraíba',
      'IFPB - Instituto Federal da Paraíba',
    ]);
  });
});
