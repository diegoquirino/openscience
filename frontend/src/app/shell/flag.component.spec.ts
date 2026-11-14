import { TestBed } from '@angular/core/testing';
import { Locale } from '../core/language.service';
import { FlagComponent } from './flag.component';

const SVG = 'http://www.w3.org/2000/svg';

describe('FlagComponent', () => {
  async function render(locale: Locale) {
    const fixture = TestBed.createComponent(FlagComponent);
    fixture.componentRef.setInput('locale', locale);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it.each([
    ['pt-BR', '0 0 20 14', 4],
    ['en-US', '0 0 38 20', 8],
    ['es-ES', '0 0 3 2', 2],
  ] as const)('draws the flag of %s as SVG shapes', async (locale, viewBox, shapes) => {
    const svg = (await render(locale)).querySelector('svg')!;
    expect(svg.getAttribute('viewBox')).toBe(viewBox);
    expect(svg.children).toHaveLength(shapes);
    expect(Array.from(svg.children).every((shape) => shape.namespaceURI === SVG)).toBe(true);
  });

  it('is hidden from screen readers and has no text', async () => {
    const element = await render('pt-BR');
    expect(element.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
    expect(element.textContent?.trim()).toBe('');
  });
});
