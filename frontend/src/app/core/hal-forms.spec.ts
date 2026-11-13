import { defaultTemplate, templateProperty } from './hal-forms';

describe('HAL-FORMS template reader', () => {
  it('reads the method, the properties and the inline options of the default template', () => {
    const template = defaultTemplate({
      _templates: {
        default: {
          method: 'put',
          properties: [
            { name: 'preferredLanguage', required: true, options: { inline: ['pt-BR', 'en-US', { value: 'es-ES' }] } },
            { name: 'comment' },
          ],
        },
      },
    });

    expect(template?.method).toBe('PUT');
    expect(templateProperty(template, 'preferredLanguage')).toEqual({
      name: 'preferredLanguage',
      required: true,
      options: ['pt-BR', 'en-US', 'es-ES'],
    });
    expect(templateProperty(template, 'comment')).toEqual({ name: 'comment', required: false, options: [] });
    expect(templateProperty(template, 'unknown')).toBeUndefined();
  });

  it('gives undefined when the resource offers no template', () => {
    expect(defaultTemplate({})).toBeUndefined();
    expect(defaultTemplate(null)).toBeUndefined();
    expect(templateProperty(undefined, 'preferredLanguage')).toBeUndefined();
  });
});
