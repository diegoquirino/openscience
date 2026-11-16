import { expect, test, type APIRequestContext, type Download, type Page } from '@playwright/test';
import path from 'node:path';
import { latestMessageTo, newestMessageId } from './support/mailpit';

// Runs against local staging (compose profile staging), whose e-mails land in Mailpit.
const ADMINISTRATOR = 'diegoquirino@gmail.com';
const FIXTURE = path.resolve(
  process.cwd(),
  '../backend/src/test/resources/minitest/veredito_minitest_vazio.csv',
);

/** The text of a Playwright download. */
async function readDownload(download: Download): Promise<string> {
  const stream = await download.createReadStream();
  const chunks: Buffer[] = [];
  for await (const chunk of stream) {
    chunks.push(Buffer.from(chunk));
  }
  return Buffer.concat(chunks).toString('utf8');
}

/** Signs in through the pt-BR screens with the code read from Mailpit. */
async function signIn(page: Page, request: APIRequestContext, name: string, email: string): Promise<void> {
  const previousId = await newestMessageId(request, email);
  await page.goto('/pt-BR/participate');
  await page.getByLabel('Nome', { exact: true }).fill(name);
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.getByRole('button', { name: 'Enviar código' }).click();
  await expect(page).toHaveURL(/\/pt-BR\/participate\/code$/, { timeout: 15_000 });
  const { code } = await latestMessageTo(request, email, previousId);
  await page.getByLabel('Código de acesso').fill(code);
  await page.getByRole('button', { name: 'Entrar' }).click();
}

test.describe('administrator (single address)', () => {
  // One code per minute; a Playwright retry would hit code-resend-too-soon.
  test.describe.configure({ mode: 'serial', retries: 0 });

  test('administrator opens Import, previews Minitest and confirms', async ({ page, request, browser }, testInfo) => {
    test.skip(testInfo.project.name !== 'desktop', 'one code per minute for the single administrator address');
    test.setTimeout(180_000);

    await signIn(page, request, 'Diego Quirino', ADMINISTRATOR);

    await expect(page).toHaveURL(/\/pt-BR\/admin$/);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Administração');
    await expect(page.getByText('Você entrou como Diego Quirino')).toBeVisible();
    const importLink = page.getByRole('link', { name: 'Importar' });
    await expect(importLink).toBeVisible();
    await expect(page.getByRole('link', { name: 'Exportar' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Painel de andamento' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Participantes' })).toBeVisible();

    await page.goto('/pt-BR/participate');
    await expect(page).toHaveURL(/\/pt-BR\/admin$/);

    await importLink.click();
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

    // Spec 009: the Export screen offers the CSV downloads through the HAL links.
    await page.goto('/pt-BR/admin');
    await page.getByRole('link', { name: 'Exportar' }).click();
    await expect(page).toHaveURL(/\/pt-BR\/admin\/export$/);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Exportar');
    await expect(page.getByRole('heading', { level: 2, name: 'Sistema minitest' })).toBeVisible();

    const wideDownload = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Baixar tabela ampla' }).click();
    const wide = await wideDownload;
    expect(wide.suggestedFilename()).toBe('veredito_wide.csv');
    const wideText = await readDownload(wide);
    expect(wideText.startsWith('system,edit_key,uc,kind,field,origin,target,rater_1_name,')).toBe(true);
    expect(wideText.split('\n').filter((line) => line.startsWith('minitest,')).length).toBeGreaterThanOrEqual(38);

    const raterDownload = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Baixar avaliador 1' }).click();
    const rater = await raterDownload;
    expect(rater.suggestedFilename()).toBe('t1_minitest_rater1.csv');
    expect(await readDownload(rater)).toContain('edit_key,uc,kind,field,origin,target,change_impact,maintenance_operation,comment');

    // Spec 010: the Dashboard shows the imported queue and Refresh reads it again without reloading.
    await page.goto('/pt-BR/admin');
    await page.getByRole('link', { name: 'Painel de andamento' }).click();
    await expect(page).toHaveURL(/\/pt-BR\/admin\/dashboard$/);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Painel de andamento');
    const overall = page.locator('[data-row="overall"]');
    await expect(overall).toContainText('Todos os sistemas');
    await expect(overall.locator('td').first()).toHaveText('38');
    await expect(page.locator('[data-testid="panel-progress"] [data-row="system"]')).toContainText('minitest');
    await expect(page.locator('[data-language]')).toHaveCount(3);
    await expect(page.getByTestId('dashboard-empty')).toHaveCount(0);
    await expect(page.getByTestId('panel-participants')).not.toContainText('@');
    const generated = await page.getByTestId('dashboard-generated').textContent();
    await page.getByTestId('dashboard-refresh').click();
    await expect(page.getByTestId('dashboard-generated')).toBeVisible();
    await expect(page).toHaveURL(/\/pt-BR\/admin\/dashboard$/);
    expect(generated).toContain('Atualizado em');

    // Spec 011: Participants is enabled; search and keep-responses withdrawal.
    const withdrawEmail = `e2e-withdraw-${Date.now()}@example.com`;
    const participantContext = await browser.newContext();
    try {
      const participantPage = await participantContext.newPage();
      await signIn(participantPage, request, 'Ana Souza', withdrawEmail);
      await expect(participantPage).toHaveURL(/\/pt-BR\/consent$/);
    } finally {
      await participantContext.close();
    }

    await page.goto('/pt-BR/admin');
    await page.getByRole('link', { name: 'Participantes' }).click();
    await expect(page).toHaveURL(/\/pt-BR\/admin\/participants$/);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Participantes');
    await page.getByLabel('E-mail', { exact: true }).fill(withdrawEmail);
    await page.getByRole('button', { name: 'Buscar' }).click();
    await expect(page.getByTestId('participant-card')).toBeVisible();
    await page.getByTestId('keep-responses').click();
    await expect(page.getByTestId('confirm-dialog')).toBeVisible();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await expect(page.getByTestId('withdrawal-result')).toContainText('Retirada registrada');
  });
});

test('a participant who opens the administration lands on the consent form (pt-BR)', async ({
  page,
  request,
}, testInfo) => {
  const email = `e2e-admin-${testInfo.project.name}-${Date.now()}@example.com`;
  await signIn(page, request, 'Ana Souza', email);
  await expect(page).toHaveURL(/\/pt-BR\/consent$/);

  await page.goto('/pt-BR/admin');

  // The welcome resolver sends a participant without consent to the consent form.
  await expect(page).toHaveURL(/\/pt-BR\/consent$/);
  await expect(page.getByRole('heading', { name: 'Termo de consentimento' })).toBeVisible();
});

test('a participant has no Export link and the export screen sends them to the consent form (pt-BR)', async ({
  page,
  request,
}, testInfo) => {
  const email = `e2e-export-${testInfo.project.name}-${Date.now()}@example.com`;
  await signIn(page, request, 'Ana Souza', email);
  await expect(page).toHaveURL(/\/pt-BR\/consent$/);
  await expect(page.getByRole('link', { name: 'Exportar' })).toHaveCount(0);

  await page.goto('/pt-BR/admin/export');

  await expect(page).toHaveURL(/\/pt-BR\/consent$/);
});

test('a participant has no Dashboard link and the dashboard screen sends them to the consent form (pt-BR)', async ({
  page,
  request,
}, testInfo) => {
  const email = `e2e-dashboard-${testInfo.project.name}-${Date.now()}@example.com`;
  await signIn(page, request, 'Ana Souza', email);
  await expect(page).toHaveURL(/\/pt-BR\/consent$/);
  await expect(page.getByRole('link', { name: 'Painel de andamento' })).toHaveCount(0);

  await page.goto('/pt-BR/admin/dashboard');

  await expect(page).toHaveURL(/\/pt-BR\/consent$/);
});

test('a participant has no Participants link and the participants screen sends them to the consent form (pt-BR)', async ({
  page,
  request,
}, testInfo) => {
  const email = `e2e-participants-${testInfo.project.name}-${Date.now()}@example.com`;
  await signIn(page, request, 'Ana Souza', email);
  await expect(page).toHaveURL(/\/pt-BR\/consent$/);
  await expect(page.getByRole('link', { name: 'Participantes' })).toHaveCount(0);

  await page.goto('/pt-BR/admin/participants');

  await expect(page).toHaveURL(/\/pt-BR\/consent$/);
});

test('a visitor who opens an area lands on the sign-in form (pt-BR)', async ({ page }) => {
  for (const area of [
    '/pt-BR/admin',
    '/pt-BR/admin/export',
    '/pt-BR/admin/dashboard',
    '/pt-BR/admin/participants',
    '/pt-BR/welcome',
  ]) {
    await page.goto(area);
    await expect(page).toHaveURL(/\/pt-BR\/participate$/);
  }
});
