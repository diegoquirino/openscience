import { expect, test, type BrowserContext, type Page } from '@playwright/test';
import {
  answerAllOverApi,
  answerCurrentItem,
  reserveOverApi,
  signInParticipant,
  type HeldTask,
} from './support/participant';

// Runs after the administrator spec imported the Minitest batch (4 blocks of 10, 10, 10 and 8 items,
// 8 tasks) into a freshly started local staging (project queue). Serial, no retries: each test
// builds on the queue left by the previous one and a code is accepted once per minute.
test.describe.configure({ mode: 'serial', retries: 0 });

const EN_US = 'English (US)';
const ES_ES = 'Español (España)';
const PT_BR = 'Português (Brasil)';

async function switchLanguage(page: Page, from: string, to: string): Promise<void> {
  const current = from.replace(/[()]/g, '\\$&');
  await page.getByRole('button', { name: new RegExp(`: ${current}$`) }).click();
  await page.getByRole('menuitem', { name: to }).click();
}

test.describe('two participants', () => {
  test('receive different first tasks; answers survive a reload (T017, T023, SC-004)', async ({
    page,
    browser,
    baseURL,
    request,
  }) => {
    test.setTimeout(120_000);
    const stamp = Date.now();
    const otherContext = await browser.newContext({ baseURL });
    const other = await otherContext.newPage();
    try {
      await signInParticipant(page, request, 'Ana Souza', `e2e-queue-a-${stamp}@example.com`);
      await signInParticipant(other, request, 'Bruno Lima', `e2e-queue-b-${stamp}@example.com`);

      await page.getByRole('link', { name: 'Começar a rotular' }).click();
      await other.getByRole('link', { name: 'Começar a rotular' }).click();
      await expect(page).toHaveURL(/\/pt-BR\/label\/items\/1$/);
      await expect(other).toHaveURL(/\/pt-BR\/label\/items\/1$/);
      await expect(page.locator('.counter')).toHaveText('1 de 10');

      const first = await reserveOverApi(page);
      const second = await reserveOverApi(other);
      expect(second.id).not.toBe(first.id);
      expect(second.blockNumber).toBe(first.blockNumber);
      expect(second.role).not.toBe(first.role);

      await answerCurrentItem(page, { impact: 'Alto', operation: 'Manter', comment: 'primeiro item' });
      await expect(page).toHaveURL(/\/pt-BR\/label\/items\/2$/);
      await answerCurrentItem(page, { impact: 'Baixo', operation: 'Atualizar' });
      await expect(page).toHaveURL(/\/pt-BR\/label\/items\/3$/);

      // Reload on the item, then come back through the start of labeling: the third item, same block.
      await page.reload();
      await expect(page.locator('.counter')).toHaveText('3 de 10');
      await page.goto('/pt-BR/label');
      await expect(page).toHaveURL(/\/pt-BR\/label\/items\/3$/);
      expect((await reserveOverApi(page)).id).toBe(first.id);

      // The drafts are kept.
      await page.goto('/pt-BR/label/items/1');
      await expect(page.getByRole('radio', { name: 'Alto', exact: true })).toBeChecked();
      await expect(page.getByRole('radio', { name: 'Manter', exact: true })).toBeChecked();
      await expect(page.getByLabel('Comentário (opcional)')).toHaveValue('primeiro item');
      await page.goto('/pt-BR/label/items/2');
      await expect(page.getByRole('radio', { name: 'Baixo', exact: true })).toBeChecked();
      await expect(page.getByRole('radio', { name: 'Atualizar', exact: true })).toBeChecked();

      // The other participant still holds the other role of the block, untouched.
      expect((await reserveOverApi(other)).answeredCount).toBe(0);
    } finally {
      await otherContext.close();
    }
  });
});

test.describe('one participant labels blocks until none is left for them', () => {
  let context: BrowserContext;
  let page: Page;
  let held: HeldTask;

  test.beforeAll(async ({ browser, baseURL, request }) => {
    test.setTimeout(90_000);
    context = await browser.newContext({ baseURL });
    page = await context.newPage();
    await signInParticipant(page, request, 'Carla Dias', `e2e-queue-c-${Date.now()}@example.com`);
  });

  test.afterAll(async () => {
    await context.close();
  });

  test('switching the language keeps the reservation and the unsaved choices; language per draft (T039, SC-006)', async () => {
    test.setTimeout(120_000);
    await page.getByRole('link', { name: 'Começar a rotular' }).click();
    await expect(page).toHaveURL(/\/pt-BR\/label\/items\/1$/);
    held = await reserveOverApi(page);

    await answerCurrentItem(page, { impact: 'Baixo', operation: 'Manter' });
    await expect(page).toHaveURL(/\/pt-BR\/label\/items\/2$/);
    await page.getByRole('radio', { name: 'Alto', exact: true }).check();
    await page.getByRole('radio', { name: 'Atualizar', exact: true }).check();
    await page.getByLabel('Comentário (opcional)').fill('ainda não salvo');
    const timer = page.getByRole('timer');
    await expect(timer).toBeVisible();

    await switchLanguage(page, PT_BR, EN_US);
    await expect(page).toHaveURL(/\/en-US\/label\/items\/2$/);
    await expect(page.getByRole('radio', { name: 'High', exact: true })).toBeChecked();
    await expect(page.getByRole('radio', { name: 'Update', exact: true })).toBeChecked();
    await expect(page.getByLabel('Comment (optional)')).toHaveValue('ainda não salvo');
    await expect(page.getByRole('timer')).toBeVisible();
    expect((await reserveOverApi(page)).id).toBe(held.id);

    await switchLanguage(page, EN_US, ES_ES);
    await expect(page).toHaveURL(/\/es-ES\/label\/items\/2$/);
    await expect(page.getByRole('radio', { name: 'Alto', exact: true })).toBeChecked();
    await expect(page.getByRole('radio', { name: 'Actualizar', exact: true })).toBeChecked();
    await expect(page.getByLabel('Comentario (opcional)')).toHaveValue('ainda não salvo');

    // The switch saved nothing: item 2 has no draft yet; saving in es-ES records es-ES.
    const before = await (await page.request.get(`/api/v1/tasks/${held.id}/items/2`)).json();
    expect(before.draft).toBeNull();
    await page.getByRole('button', { name: 'Continuar' }).click();
    await expect(page).toHaveURL(/\/es-ES\/label\/items\/3$/);
    const second = await (await page.request.get(`/api/v1/tasks/${held.id}/items/2`)).json();
    const first = await (await page.request.get(`/api/v1/tasks/${held.id}/items/1`)).json();
    expect(second.draft.language).toBe('es-ES');
    expect(first.draft.language).toBe('pt-BR');
    expect((await reserveOverApi(page)).id).toBe(held.id);
  });

  test('completes the block on screen and submits it (T032)', async () => {
    test.setTimeout(180_000);
    await page.goto('/pt-BR/label');
    await expect(page).toHaveURL(/\/pt-BR\/label\/items\/3$/);

    for (let position = 3; position <= held.size; position++) {
      await expect(page.locator('.counter')).toHaveText(`${position} de ${held.size}`);
      await answerCurrentItem(page, {
        impact: position % 2 === 0 ? 'Alto' : 'Baixo',
        operation: 'Manter',
      });
    }

    await expect(page).toHaveURL(/\/pt-BR\/label\/review$/);
    await expect(page.locator('li.item')).toHaveCount(held.size);
    await expect(page.locator('li.item.pending')).toHaveCount(0);
    await page.getByRole('button', { name: 'Enviar bloco' }).click();

    await expect(page).toHaveURL(/\/pt-BR\/label\/done\?outcome=more-available$/);
    await expect(page.getByText('Você pode rotular outro bloco.')).toBeVisible();
    const done = await (await page.request.get(`/api/v1/tasks/${held.id}`)).json();
    expect(done.status).toBe('completed');
    expect(done.answeredCount).toBe(held.size);
    // The answers are locked: a late save is refused.
    const item = await (await page.request.get(`/api/v1/tasks/${held.id}/items/1`)).json();
    const late = await page.request.put(`/api/v1/tasks/${held.id}/responses/${item.itemId}`, {
      data: { changeImpact: 'HIGH', maintenanceOperation: 'FLAG', comment: null },
    });
    expect(late.status()).toBe(409);
  });

  test('labels one role of every block and then gets none-available-for-you (T035)', async () => {
    test.setTimeout(180_000);
    const remaining = [3, 4];
    for (const [index, blockNumber] of remaining.entries()) {
      await page.getByRole('link', { name: 'Rotular outro bloco' }).click();
      await expect(page).toHaveURL(/\/pt-BR\/label\/items\/1$/);
      const task = await reserveOverApi(page);
      expect(task.blockNumber).toBe(blockNumber);

      // Drafts over the API keep the test short; the review and the submit are on screen.
      await answerAllOverApi(page, task);
      await page.goto('/pt-BR/label');
      await expect(page).toHaveURL(/\/pt-BR\/label\/review$/);
      await page.getByRole('button', { name: 'Enviar bloco' }).click();

      if (index < remaining.length - 1) {
        await expect(page).toHaveURL(/\/pt-BR\/label\/done\?outcome=more-available$/);
      }
    }

    await expect(page).toHaveURL(/\/pt-BR\/label\/done\?outcome=none-available-for-you$/);
    await expect(page.getByText('Não há mais blocos para você no momento.')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Rotular outro bloco' })).toHaveCount(0);

    // Asking again over the API and through the start of labeling agree.
    const again = await page.request.post('/api/v1/tasks/reservations');
    expect(again.status()).toBe(200);
    expect((await again.json()).outcome).toBe('none-available-for-you');
    await page.goto('/pt-BR/label');
    await expect(page).toHaveURL(/\/pt-BR\/label\/done\?outcome=none-available-for-you$/);
  });

  test('the done screen shows the right end message and actions for each outcome (T035)', async () => {
    const expected = [
      { outcome: 'database-complete', text: 'A base de dados foi totalmente respondida.', another: false },
      { outcome: 'none-available-for-you', text: 'Não há mais blocos para você no momento.', another: false },
      { outcome: 'more-available', text: 'Você pode rotular outro bloco.', another: true },
      { outcome: 'no-items-yet', text: 'Ainda não há itens para rotular.', another: false },
    ];
    for (const { outcome, text, another } of expected) {
      await page.goto(`/pt-BR/label/done?outcome=${outcome}`);
      await expect(page.getByText(text)).toBeVisible();
      await expect(page.getByRole('link', { name: 'Rotular outro bloco' })).toHaveCount(another ? 1 : 0);
      await expect(page.locator('section.done button.sign-out')).toBeVisible();
    }
  });
});
