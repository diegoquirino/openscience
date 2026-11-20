import { expect, test, type Page } from '@playwright/test';
import { latestMessageTo, messageCountTo } from './support/mailpit';

// Runs against local staging (compose profile staging), whose e-mails land in Mailpit.
async function noHorizontalScrolling(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(0);
}

test('the session survives a new browser window and ends on "Sign out" (US3)', async ({
  page,
  browser,
  request,
  baseURL,
}, testInfo) => {
  const email = `e2e-session-${testInfo.project.name}-${Date.now()}@example.com`;
  await page.goto('/pt-BR/participate');
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.getByRole('button', { name: 'Enviar código' }).click();
  const { code } = await latestMessageTo(request, email);
  await page.getByLabel('Código de acesso').fill(code);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(/\/pt-BR\/skill$/);
  const sent = await messageCountTo(request, email);

  const { viewport, isMobile, hasTouch, userAgent, deviceScaleFactor } = testInfo.project.use;
  const reopened = await browser.newContext({
    viewport,
    isMobile,
    hasTouch,
    userAgent,
    deviceScaleFactor,
    baseURL,
    storageState: await page.context().storageState(),
  });
  const again = await reopened.newPage();
  await again.goto('/pt-BR/welcome');

  await expect(again).toHaveURL(/\/pt-BR\/skill$/);
  await expect(again.getByLabel('Nome')).toBeVisible();
  const header = again.locator('header');
  await expect(header.getByRole('button', { name: /Conta|Account/i })).toBeVisible();
  await noHorizontalScrolling(again);
  expect(await messageCountTo(request, email)).toBe(sent);

  await header.getByRole('button', { name: /Conta|Account/i }).click();
  await again.getByRole('menuitem', { name: 'Sair' }).click();

  await expect(again).toHaveURL(/\/pt-BR\/$/);
  await expect(header.getByRole('button', { name: /Conta|Account/i })).toHaveCount(0);
  const cookies = await reopened.cookies();
  expect(cookies.find((cookie) => cookie.name === 'veredito-session')).toBeUndefined();

  await again.goto('/pt-BR/welcome');
  await expect(again).toHaveURL(/\/pt-BR\/participate$/);
  await reopened.close();
});
