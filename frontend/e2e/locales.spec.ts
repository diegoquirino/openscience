import { expect, test } from '@playwright/test';

test('reloading a deep link keeps the locale (US6 scenario 2)', async ({ page }) => {
  await page.goto('/es-ES/participate');
  await page.reload();

  await expect(page).toHaveURL('/es-ES/participate');
  await expect(page.locator('html')).toHaveAttribute('lang', 'es-ES');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Participar');
  await expect(page.getByLabel('Nombre')).toBeVisible();
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