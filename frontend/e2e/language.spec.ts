import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { latestMessageTo, newestMessageId } from './support/mailpit';

// Runs against local staging (compose profile staging), whose e-mails land in Mailpit.
const SCREENS = {
  'pt-BR': {
    name: 'Nome',
    email: 'E-mail',
    send: 'Enviar código',
    code: 'Código de acesso',
    signIn: 'Entrar',
    subject: 'Seu código de acesso ao Veredito',
    label: 'Português (Brasil)',
    welcome: 'Obrigado por se identificar',
  },
  'en-US': {
    name: 'Name',
    email: 'E-mail',
    send: 'Send code',
    code: 'Access code',
    signIn: 'Sign in',
    subject: 'Your Veredito access code',
    label: 'English (US)',
    welcome: 'Thank you for identifying yourself',
  },
  'es-ES': {
    name: 'Nombre',
    email: 'Correo electrónico',
    send: 'Enviar código',
    code: 'Código de acceso',
    signIn: 'Iniciar sesión',
    subject: 'Su código de acceso a Veredito',
    label: 'Español (España)',
    welcome: 'Gracias por identificarse',
  },
} as const;

type Locale = keyof typeof SCREENS;
// One code per e-mail per 60 s, plus the time between reading the clock and the request.
const NEXT_CODE_AFTER_MS = 65_000;

async function requestCode(page: Page, locale: Locale, name: string, email: string): Promise<void> {
  const screen = SCREENS[locale];
  await page.goto(`/${locale}/participate`);
  await page.getByLabel(screen.name, { exact: true }).fill(name);
  await page.getByLabel(screen.email, { exact: true }).fill(email);
  await page.getByRole('button', { name: screen.send }).click();
  await expect(page).toHaveURL(new RegExp(`/${locale}/participate/code$`));
}

/** Requests a code on the screens of `locale`, signs in with it and returns the subject of its e-mail. */
async function signIn(page: Page, request: APIRequestContext, locale: Locale, email: string): Promise<string> {
  const previousId = await newestMessageId(request, email);
  await requestCode(page, locale, 'Ana Souza', email);
  const message = await latestMessageTo(request, email, previousId);
  await page.getByLabel(SCREENS[locale].code).fill(message.code);
  await page.getByRole('button', { name: SCREENS[locale].signIn }).click();
  return message.subject;
}

async function switchLanguage(page: Page, from: Locale, to: Locale): Promise<void> {
  const current = SCREENS[from].label.replace(/[()]/g, '\\$&');
  await page.getByRole('button', { name: new RegExp(`: ${current}$`) }).click();
  await page.getByRole('menuitem', { name: SCREENS[to].label }).click();
}

async function savedLanguage(page: Page): Promise<string | undefined> {
  const cookies = await page.context().cookies();
  return cookies.find((cookie) => cookie.name === 'veredito-lang')?.value;
}

for (const [browserLocale, preselected] of [
  ['pt-BR', 'pt-BR'],
  ['es-MX', 'es-ES'],
  ['en-GB', 'en-US'],
  ['fr-FR', 'pt-BR'],
] as const) {
  test.describe(`a browser in ${browserLocale}`, () => {
    test.use({ locale: browserLocale });

    test(`sees ${preselected} preselected at the language choice (FR-010)`, async ({ page }) => {
      await page.goto('/');

      const choice = page.locator(`a[hreflang="${preselected}"]`);
      await expect(choice).toHaveAttribute('aria-current', 'true');
      await expect(choice).toBeFocused();
      await expect(page.locator('a[aria-current]')).toHaveCount(1);
    });
  });
}

test('the chosen language is saved and opens directly next time (FR-011)', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('link', { name: 'English (US)' }).click();
  await expect(page).toHaveURL('/en-US/');
  expect(await savedLanguage(page)).toBe('en-US');

  await page.goto('/');

  await expect(page).toHaveURL('/en-US/');
});

test('opening a prefix directly saves its language', async ({ page }) => {
  await page.goto('/es-ES/');
  await expect(page.locator('html')).toHaveAttribute('lang', 'es-ES');
  await expect.poll(() => savedLanguage(page)).toBe('es-ES');

  await page.goto('/');

  await expect(page).toHaveURL('/es-ES/');
});

test('an unsupported saved language shows the language choice', async ({ page, baseURL }) => {
  await page.context().addCookies([{ name: 'veredito-lang', value: 'xx-XX', url: baseURL! }]);

  await page.goto('/');

  await expect(page).toHaveURL('/');
  await expect(page.getByRole('link', { name: 'Español (España)' })).toBeVisible();
});

for (const locale of Object.keys(SCREENS) as Locale[]) {
  test(`a first sign-in in ${locale} gets the e-mail and the screens in ${locale}`, async ({ page, request }, testInfo) => {
    const email = `e2e-lang-${locale.toLowerCase()}-${testInfo.project.name}-${Date.now()}@example.com`;

    const subject = await signIn(page, request, locale, email);

    expect(subject).toBe(SCREENS[locale].subject);
    await expect(page).toHaveURL(new RegExp(`/${locale}/welcome$`));
    await expect(page.locator('html')).toHaveAttribute('lang', locale);
    await expect(page.getByText(SCREENS[locale].welcome)).toBeVisible();
    for (const other of Object.keys(SCREENS) as Locale[]) {
      if (other !== locale) {
        await expect(page.getByText(SCREENS[other].welcome)).toHaveCount(0);
      }
    }
  });
}

test('the header switch keeps the typed values (FR-013)', async ({ page }, testInfo) => {
  const email = `e2e-switch-${testInfo.project.name}-${Date.now()}@example.com`;
  await page.goto('/pt-BR/participate');
  await page.getByLabel('Nome', { exact: true }).fill('Ana Souza');
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.evaluate(() => ((window as unknown as Record<string, unknown>)['vereditoMarker'] = true));

  await switchLanguage(page, 'pt-BR', 'es-ES');

  await expect(page).toHaveURL('/es-ES/participate');
  await expect(page.locator('html')).toHaveAttribute('lang', 'es-ES');
  await expect(page.getByLabel('Nombre', { exact: true })).toHaveValue('Ana Souza');
  await expect(page.getByLabel('Correo electrónico', { exact: true })).toHaveValue(email);
  const kept = await page.evaluate(() => (window as unknown as Record<string, unknown>)['vereditoMarker']);
  expect(kept).toBeUndefined();
  expect(await savedLanguage(page)).toBe('es-ES');
});

test('the profile language follows the header switch and wins at the next sign-in (SC-006)', async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(150_000);
  const email = `e2e-profile-${testInfo.project.name}-${Date.now()}@example.com`;

  const firstCodeAt = Date.now();
  await signIn(page, request, 'pt-BR', email);
  await expect(page).toHaveURL(/\/pt-BR\/welcome$/);
  await switchLanguage(page, 'pt-BR', 'es-ES');
  await expect(page).toHaveURL(/\/es-ES\/welcome$/);
  await expect(page.getByText('Gracias por identificarse')).toBeVisible();

  await page.context().clearCookies();
  await page.waitForTimeout(Math.max(0, firstCodeAt + NEXT_CODE_AFTER_MS - Date.now()));
  const subject = await signIn(page, request, 'en-US', email);

  expect(subject).toBe(SCREENS['en-US'].subject);
  await expect(page).toHaveURL(/\/es-ES\/welcome$/);
  await expect(page.getByText('Gracias por identificarse')).toBeVisible();
  expect(await savedLanguage(page)).toBe('es-ES');
});
