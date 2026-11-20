import { expect, type APIRequestContext, type Page } from '@playwright/test';
import { latestMessageTo, newestMessageId } from './mailpit';

/**
 * Signs in through the pt-BR screens with the code read from Mailpit (e-mail under example.com). A new
 * participant lands on the consent form (spec 006): the resolver at /welcome opens the pending step.
 */
export async function signInParticipant(
  page: Page,
  request: APIRequestContext,
  name: string,
  email: string,
): Promise<void> {
  const previousId = await newestMessageId(request, email);
  await page.goto('/pt-BR/participate');
  await page.getByLabel('Nome', { exact: true }).fill(name);
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.getByRole('button', { name: 'Enviar código' }).click();
  await expect(page).toHaveURL(/\/pt-BR\/participate\/code$/, { timeout: 15_000 });
  const { code } = await latestMessageTo(request, email, previousId);
  await page.getByLabel('Código de acesso').fill(code);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(/\/pt-BR\/consent$/);
}

/** Accepts the current consent form, chooses a skill and answers the screening over the API (cookies of the page). */
export async function completeOnboardingOverApi(page: Page, answer = 'yes_professional'): Promise<void> {
  const headers = { 'Accept-Language': 'pt-BR' };
  const form = await page.request.get('/api/v1/consent-forms/current', { headers });
  expect(form.status()).toBe(200);
  const { version } = await form.json();
  const consent = await page.request.put('/api/v1/participants/me/consent', {
    headers,
    data: { decision: 'accepted', consentFormVersion: version },
  });
  expect(consent.status()).toBe(200);
  const skill = await page.request.put('/api/v1/participants/me/skill', { headers, data: { skill: 'test_analyst' } });
  expect(skill.status()).toBe(200);
  const screening = await page.request.put('/api/v1/participants/me/screening', { headers, data: { answer } });
  expect(screening.status()).toBe(200);
}

/** Signs in a new participant and completes the onboarding over the API, so the participant can label. */
export async function signInEligibleParticipant(
  page: Page,
  request: APIRequestContext,
  name: string,
  email: string,
): Promise<void> {
  await signInParticipant(page, request, name, email);
  await completeOnboardingOverApi(page);
}

/** The block held by the signed-in participant, read from the API with the session of the page. */
export interface HeldTask {
  id: number;
  blockNumber: number;
  role: number;
  size: number;
  status: string;
  answeredCount: number;
  outcome?: string | null;
}

/** Asks for a block (or resumes the held one) over the API using the cookies of the page. */
export async function reserveOverApi(page: Page): Promise<HeldTask> {
  const response = await page.request.post('/api/v1/tasks/reservations');
  expect([200, 201]).toContain(response.status());
  const body = await response.json();
  expect(body.id, 'a task, not an end-of-queue outcome').toBeDefined();
  return body as HeldTask;
}

/** Saves a draft for every item of the held block over the API (drafts only; no submission). */
export async function answerAllOverApi(page: Page, task: HeldTask): Promise<void> {
  for (let position = 1; position <= task.size; position++) {
    const item = await (await page.request.get(`/api/v1/tasks/${task.id}/items/${position}`)).json();
    const saved = await page.request.put(`/api/v1/tasks/${task.id}/responses/${item.itemId}`, {
      data: {
        changeImpact: position % 2 === 0 ? 'HIGH' : 'LOW',
        maintenanceOperation: 'KEEP',
        comment: null,
      },
    });
    expect(saved.status()).toBe(200);
  }
}

/** Answers the item on screen (pt-BR labels) and continues to the next screen. */
export async function answerCurrentItem(
  page: Page,
  answer: { impact: string; operation: string; comment?: string },
): Promise<void> {
  await page.getByRole('radio', { name: answer.impact, exact: true }).check();
  await page.getByRole('radio', { name: answer.operation, exact: true }).check();
  if (answer.comment !== undefined) {
    await page.getByLabel('Comentário (opcional)').fill(answer.comment);
  }
  const before = page.url();
  await page.getByRole('button', { name: 'Continuar' }).click();
  await expect(page).not.toHaveURL(before);
}
