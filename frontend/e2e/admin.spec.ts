import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import path from 'node:path';
import { latestMessageTo, newestMessageId } from './support/mailpit';

// Runs against local staging (compose profile staging), whose e-mails land in Mailpit.
const ADMINISTRATOR = 'diegoquirino@gmail.com';
const NEXT_CODE_AFTER_MS = 61_000;
const FIXTURE = path.resolve(
  process.cwd(),
  '../backend/src/test/resources/minitest/veredito_minitest_vazio.csv',
);

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

test.describe('administrator (single address)', () => {
  test.describe.configure({ mode: 'serial' });

  let lastAdminCodeAt = 0;

  async function signInAdministrator(page: Page, request: APIRequestContext): Promise<void> {
    await page.waitForTimeout(Math.max(0, lastAdminCodeAt + NEXT_CODE_AFTER_MS - Date.now()));
    const previousId = await newestMessageId(request, ADMINISTRATOR);
    await page.goto('/pt-BR/participate');
    await page.getByLabel('Nome').fill('Diego Quirino');
    await page.getByLabel('E-mail').fill(ADMINISTRATOR);
    await page.getByRole('button', { name: 'Enviar código' }).click();
    await expect(page).toHaveURL(/\/pt-BR\/participate\/code$/);
    lastAdminCodeAt = Date.now();
    const { code } = await latestMessageTo(request, ADMINISTRATOR, previousId);
    await page.getByLabel('Código de acesso').fill(code);
    await page.getByRole('button', { name: 'Entrar' }).click();
  }

  test('the administrator signs in and reaches the administration (pt-BR)', async ({ page, request }, testInfo) => {
    test.skip(testInfo.project.name !== 'desktop', 'one code per minute for the single administrator address');
    await signInAdministrator(page, request);

    await expect(page).toHaveURL(/\/pt-BR\/admin$/);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Administração');
    await expect(page.getByText('Você entrou como Diego Quirino')).toBeVisible();
    const menu = page.getByRole('group', { name: 'Funções administrativas' });
    await expect(menu.getByRole('link', { name: 'Importar' })).toBeVisible();
    for (const item of ['Exportar', 'Painel de andamento', 'Participantes']) {
      await expect(menu.getByRole('button', { name: item })).toBeDisabled();
    }

    await page.goto('/pt-BR/participate');
    await expect(page).toHaveURL(/\/pt-BR\/admin$/);
  });

  test('administrator previews Minitest, confirms, and re-import is skipped', async ({
    page,
    request,
  }, testInfo) => {
    test.skip(testInfo.project.name !== 'desktop', 'one code per minute for the single administrator address');
    await signInAdministrator(page, request);
    await expect(page).toHaveURL(/\/pt-BR\/admin$/);

    await page.getByRole('link', { name: 'Importar' }).click();
    await expect(page).toHaveURL(/\/pt-BR\/admin\/import$/);
    await page.locator('input[type="file"]').setInputFiles(FIXTURE);

    await expect(page.getByText(/Incluídas:\s*38/)).toBeVisible();
    await expect(page.getByText(/Erros:\s*0/)).toBeVisible();
    await expect(page.getByText(/Blocos:\s*4\s*\(10,\s*10,\s*10,\s*8\)/)).toBeVisible();

    await page.getByRole('button', { name: 'Confirmar importação' }).click();
    await expect(page.getByRole('heading', { name: 'Importação confirmada' })).toBeVisible();
    await expect(page.getByText(/Posições na fila:\s*1\s*a\s*8/)).toBeVisible();

    await page.getByRole('button', { name: 'Importar outro arquivo' }).click();
    await page.locator('input[type="file"]').setInputFiles(FIXTURE);
    await expect(page.getByText(/Incluídas:\s*0/)).toBeVisible();
    await expect(page.getByText(/Ignoradas:\s*38/)).toBeVisible();
  });
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
