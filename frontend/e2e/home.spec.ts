import { expect, test } from '@playwright/test';

const LOCALES = [
  { code: 'pt-BR', label: 'Português (Brasil)', participate: 'Participar' },
  { code: 'en-US', label: 'English (US)', participate: 'Participate' },
  { code: 'es-ES', label: 'Español (España)', participate: 'Participar' },
];

test('the root page offers the three languages', async ({ page }) => {
  await page.goto('/');
  for (const locale of LOCALES) {
    await expect(page.getByRole('link', { name: locale.label })).toHaveAttribute('href', `/${locale.code}/`);
  }
});

for (const locale of LOCALES) {
  test(`the home page in ${locale.code}`, async ({ page, request }) => {
    const entryPoint = await (await request.get('/api/v1')).json();

    await page.goto(`/${locale.code}/`);

    await expect(page.locator('html')).toHaveAttribute('lang', locale.code);
    await expect(page.locator('header')).toContainText('Veredito');
    await expect(page.getByRole('link', { name: locale.participate, exact: true })).toBeVisible();
    await expect(page.locator('footer')).toContainText(entryPoint.systemVersion);
  });

  test(`no horizontal scrolling in ${locale.code}`, async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'phone', 'phone width only');
    await page.goto(`/${locale.code}/`);
    await expect(page.locator('header')).toBeVisible();
    const fits = await page.evaluate(
      () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
    );
    expect(fits).toBe(true);
  });
}
