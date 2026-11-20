import { expect, test } from '@playwright/test';
import { latestMessageTo } from './support/mailpit';

// Runs against local staging (compose profile staging), whose e-mails land in Mailpit.
test('a new participant signs in with the code sent by e-mail (pt-BR)', async ({ page, request }, testInfo) => {
  const email = `e2e-${testInfo.project.name}-${Date.now()}@example.com`;

  await page.goto('/pt-BR/participate');
  await page.getByLabel('E-mail').fill(email);
  await page.getByRole('button', { name: 'Enviar código' }).click();

  await expect(page).toHaveURL(/\/pt-BR\/participate\/code$/);
  await expect(page.getByText(email)).toBeVisible();
  await expect(page.getByText('verifique a pasta de spam')).toBeVisible();
  await expect(page.getByRole('button', { name: /^Reenviar em \d+ s$/ })).toBeDisabled();

  const message = await latestMessageTo(request, email);
  expect(message.subject).toBe('Seu código de acesso ao Veredito');
  expect(message.text).not.toContain('Ana Souza');

  await page.getByLabel('Código de acesso').fill(`${message.code.slice(0, 3)} ${message.code.slice(3)}`);
  await page.getByRole('button', { name: 'Entrar' }).click();

  // Spec 013: profile (name + skill) before the consent form.
  await expect(page).toHaveURL(/\/pt-BR\/skill$/);
  await expect(page.getByLabel('Nome')).toBeVisible();
  const cookies = await page.context().cookies();
  expect(cookies.find((cookie) => cookie.name === 'veredito-session')).toMatchObject({
    httpOnly: true,
    secure: true,
    sameSite: 'Lax',
  });
});

test('a wrong code shows the attempts left (pt-BR)', async ({ page, request }, testInfo) => {
  const email = `e2e-wrong-${testInfo.project.name}-${Date.now()}@example.com`;

  await page.goto('/pt-BR/participate');
  await page.getByLabel('E-mail').fill(email);
  await page.getByRole('button', { name: 'Enviar código' }).click();
  const { code } = await latestMessageTo(request, email);

  const wrong = String((Number(code) + 1) % 1_000_000).padStart(6, '0');
  await page.getByLabel('Código de acesso').fill(wrong);
  await page.getByRole('button', { name: 'Entrar' }).click();

  // Spec 019: wrong-code feedback is a toast (MatSnackBar), not an inline role=alert.
  const toast = page.locator('.toast.mat-mdc-snack-bar-container');
  await expect(toast).toContainText('Código incorreto; restam 4 tentativas');
  await expect(page.getByRole('button', { name: 'Fechar' })).toBeVisible();
});
