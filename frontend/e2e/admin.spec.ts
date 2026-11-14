import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { latestMessageTo, newestMessageId } from './support/mailpit';

// Runs against local staging (compose profile staging), whose e-mails land in Mailpit.
const ADMINISTRATOR = 'diegoquirino@gmail.com';

/** Signs in through the pt-BR screens with the code read from Mailpit. */
async function signIn(page: Page, request: APIRequestContext, name: string, email: string): Promise<void> {
  const previousId = await newestMessageId(request, email);
  await page.goto('/pt-BR/participate');
  await page.getByLabel('Nome').fill(name);
  await page.getByLabel('E-mail').fill(email);
  await page.getByRole('button', { name: 'Enviar código' }).click();
  await expect(page).toHaveURL(/\/pt-BR\/participate\/code$/);
  const { code } = await latestMessageTo(request, email, previousId);
  await page.getByLabel('Código de acesso').fill(code);
  await page.getByRole('button', { name: 'Entrar' }).click();
}

test('the administrator signs in and reaches the administration (pt-BR)', async ({ page, request }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'one code per minute for the single administrator address');
  await signIn(page, request, 'Diego Quirino', ADMINISTRATOR);

  await expect(page).toHaveURL(/\/pt-BR\/admin$/);
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Administração');
  await expect(page.getByText('Você entrou como Diego Quirino')).toBeVisible();
  const menu = page.getByRole('group', { name: 'Funções administrativas' });
  for (const item of ['Importar', 'Exportar', 'Painel de andamento', 'Participantes']) {
    await expect(menu.getByRole('button', { name: item })).toBeDisabled();
  }

  await page.goto('/pt-BR/participate');
  await expect(page).toHaveURL(/\/pt-BR\/admin$/);
});

test('a participant who opens the administration lands on the welcome screen (pt-BR)', async ({
  page,
  request,
}, testInfo) => {
  const email = `e2e-admin-${testInfo.project.name}-${Date.now()}@example.com`;
  await signIn(page, request, 'Ana Souza', email);
  await expect(page).toHaveURL(/\/pt-BR\/welcome$/);

  await page.goto('/pt-BR/admin');

  await expect(page).toHaveURL(/\/pt-BR\/welcome$/);
  await expect(page.getByText('Obrigado por se identificar')).toBeVisible();
});

test('a visitor who opens an area lands on the sign-in form (pt-BR)', async ({ page }) => {
  for (const area of ['/pt-BR/admin', '/pt-BR/welcome']) {
    await page.goto(area);
    await expect(page).toHaveURL(/\/pt-BR\/participate$/);
  }
});
