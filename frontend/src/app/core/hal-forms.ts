/** Minimal reader of HAL-FORMS `_templates` (method, properties, inline options). */

export interface HalFormsProperty {
  name: string;
  required: boolean;
  options: string[];
}

export interface HalFormsTemplate {
  method: string;
  properties: HalFormsProperty[];
}

interface RawProperty {
  name: string;
  required?: boolean;
  options?: { inline?: (string | { value: string })[] };
}

interface RawTemplate {
  method?: string;
  properties?: RawProperty[];
}

export interface HalFormsResource {
  _templates?: Record<string, RawTemplate | undefined>;
}

/** The `default` template, or `undefined` when the resource offers no change. */
export function defaultTemplate(resource: HalFormsResource | null | undefined): HalFormsTemplate | undefined {
  const raw = resource?._templates?.['default'];
  if (!raw) {
    return undefined;
  }
  return {
    method: (raw.method ?? 'POST').toUpperCase(),
    properties: (raw.properties ?? []).map((property) => ({
      name: property.name,
      required: property.required ?? false,
      options: (property.options?.inline ?? []).map((option) => (typeof option === 'string' ? option : option.value)),
    })),
  };
}

/** A property of the template by name. */
export function templateProperty(template: HalFormsTemplate | undefined, name: string): HalFormsProperty | undefined {
  return template?.properties.find((property) => property.name === name);
}
