import { expect, test } from '@playwright/test';

// Runs against local staging (compose profile staging), as in CI.
const BANNERS = [
  { code: 'pt-BR', text: 'AMBIENTE DE ENSAIO' },
  { code: 'en-US', text: 'STAGING ENVIRONMENT' },
  { code: 'es-ES', text: 'ENTORNO DE PRUEBAS' },
];

test('the entry point reports the staging environment', async ({ request }) => {
  const entryPoint = await (await request.get('/api/v1')).json();
  expect(entryPoint.environment).toBe('staging');
});

for (const banner of BANNERS) {
  test(`the staging banner in ${banner.code} (US4 scenario 2)`, async ({ page }) => {
    await page.goto(`/${banner.code}/`);
    await expect(page.locator('header').getByRole('note')).toHaveText(banner.text);
  });
}
