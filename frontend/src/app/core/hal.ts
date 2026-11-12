/** Minimal reader of HAL / HAL-FORMS `_links`, with expansion of CURIEs (`vd:request-code`). */

export interface HalLink {
  href: string;
  templated?: boolean;
  name?: string;
  title?: string;
}

export type HalLinks = Record<string, HalLink | HalLink[] | undefined>;

export interface HalResource {
  _links?: HalLinks;
}

export const DEFAULT_CURIE = 'vd';

export class Links {
  constructor(private readonly raw: HalLinks = {}) {}

  static of(resource: HalResource | null | undefined): Links {
    return new Links(resource?._links ?? {});
  }

  /** A relation by name; a name without a prefix is read as `vd:<name>`. */
  get(rel: string): HalLink | undefined {
    const value = this.raw[this.qualify(rel)];
    return Array.isArray(value) ? value[0] : value;
  }

  has(rel: string): boolean {
    return this.get(rel) !== undefined;
  }

  href(rel: string): string | undefined {
    return this.get(rel)?.href;
  }

  /** Relations present, without `curies`. */
  rels(): string[] {
    return Object.keys(this.raw).filter((rel) => rel !== 'curies');
  }

  /** Documentation address of a curied relation (`/api/v1/rels/request-code`). */
  documentation(rel: string): string | undefined {
    const qualified = this.qualify(rel);
    const separator = qualified.indexOf(':');
    if (separator < 0) {
      return undefined;
    }
    const prefix = qualified.substring(0, separator);
    const name = qualified.substring(separator + 1);
    const curies = this.raw['curies'];
    const curie = (Array.isArray(curies) ? curies : curies ? [curies] : []).find((c) => c.name === prefix);
    return curie?.href.replace('{rel}', encodeURIComponent(name));
  }

  private qualify(rel: string): string {
    return rel === 'self' || rel === 'curies' || rel.includes(':') ? rel : `${DEFAULT_CURIE}:${rel}`;
  }
}
