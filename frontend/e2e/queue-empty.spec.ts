import { expect, test } from '@playwright/test';
import { signInParticipant } from './support/participant';

// Runs first (project queue-empty), on a freshly started local staging with nothing imported yet.
// One code per e-mail per minute: no retries.
test.describe.configure({ retries: 0 });

test('with no items imported, a participant who starts labeling sees "no items yet" (T017)', async ({
  page,
  request,
}) => {
  await signInParticipant(page, request, 'Ana Souza', `e2e-queue-empty-${Date.now()}@example.com`);

  await page.getByRole('link', { name: 'Começar a rotular' }).click();

  await expect(page).toHaveURL(/\/pt-BR\/label\/done\?outcome=no-items-yet$/);
  await expect(page.getByText('Ainda não há itens para rotular.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Rotular outro bloco' })).toHaveCount(0);
  await expect(page.locator('section.done button.sign-out')).toBeVisible();
});
