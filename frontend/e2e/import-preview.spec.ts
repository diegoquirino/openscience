import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import path from 'node:path';
import { latestMessageTo, newestMessageId } from './support/mailpit';

const ADMINISTRATOR = 'diegoquirino@gmail.com';
const FIXTURE = path.resolve(
  process.cwd(),
  '../backend/src/test/resources/minitest/veredito_minitest_vazio.csv',
);

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

test('administrator previews Minitest with zero errors and can cancel', async ({ page, request }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'one code per minute for the single administrator address');
  await signIn(page, request, 'Diego Quirino', ADMINISTRATOR);
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