import { expect, test } from '@playwright/test';

test('reloading a deep link keeps the locale (US6 scenario 2)', async ({ page }) => {
  await page.goto('/es-ES/participate');
  await page.reload();

  await expect(page).toHaveURL('/es-ES/participate');
  await expect(page.locator('html')).toHaveAttribute('lang', 'es-ES');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Participar');
  await expect(page.getByRole('link', { name: 'Volver a la página de inicio' })).toBeVisible();
});

test('a prefix in another case ends at the canonical prefix', async ({ page }) => {
  await page.goto('/en-us/');

  await expect(page).toHaveURL('/en-US/');
  await expect(page.locator('html')).toHaveAttribute('lang', 'en-US');
});

for (const path of ['/fr-FR/', '/xyz']) {
  test(`${path} ends at the language choice`, async ({ page }) => {
    await page.goto(path);

    await expect(page).toHaveURL('/');
    await expect(page.getByRole('link', { name: 'English (US)' })).toBeVisible();
  });
}

test('a footer language link opens the same route under the other prefix with a page load (US6 scenarios 3 and 4)', async ({
  page,
}) => {
  await page.goto('/pt-BR/participate');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Participar');
  await page.evaluate(() => ((window as unknown as Record<string, unknown>)['vereditoMarker'] = true));

  await page.locator('footer').getByRole('link', { name: 'English (US)' }).click();

  await expect(page).toHaveURL('/en-US/participate');
  await expect(page.locator('html')).toHaveAttribute('lang', 'en-US');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Participate');
  const kept = await page.evaluate(() => (window as unknown as Record<string, unknown>)['vereditoMarker']);
  expect(kept).toBeUndefined();
});
