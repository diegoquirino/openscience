import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { latestMessageTo, newestMessageId } from './support/mailpit';

// Spec 006: consent form, professional skill and screening, against local staging with the fixture
// consent form `fixture-1` (three languages). One code per e-mail per minute: no retries.
test.describe.configure({ retries: 0 });

const SCREENS = {
  'pt-BR': {
    name: 'Nome',
    email: 'E-mail',
    send: 'Enviar código',
    code: 'Código de acesso',
    signIn: 'Entrar',
    consent: 'Termo de consentimento',
    accept: 'Li o termo e concordo em participar',
    refuse: 'Não concordo',
    declined: 'Obrigado por considerar o estudo',
    again: 'Ler o termo novamente',
    skill: 'Sua habilidade profissional',
    skillOption: 'Analista de testes / QA',
    other: 'Outro (descrever)',
    otherField: 'Descreva sua habilidade',
    continue: 'Continuar',
    screening: 'Triagem',
    yes: 'Sim, na prática profissional',
    no: 'Não',
    closed: 'Obrigado pelo seu interesse',
    referenceHeading: 'Texto de referência (português, Brasil)',
    referenceLink: 'Ler o texto de referência (pt-BR)',
    label: 'Português (Brasil)',
  },
  'en-US': {
    name: 'Name',
    email: 'E-mail',
    send: 'Send code',
    code: 'Access code',
    signIn: 'Sign in',
    consent: 'Consent form',
    accept: 'I have read it and agree to participate',
    refuse: 'I do not agree',
    declined: 'Thank you for considering the study',
    again: 'Read the consent form again',
    skill: 'Your professional skill',
    skillOption: 'Test analyst / QA',
    other: 'Other (describe)',
    otherField: 'Describe your skill',
    continue: 'Continue',
    screening: 'Screening',
    yes: 'Yes, in professional practice',
    no: 'No',
    closed: 'Thank you for your interest',
    referenceHeading: 'Reference text (Portuguese, Brazil)',
    referenceLink: 'Read the reference text (pt-BR)',
    label: 'English (US)',
  },
  'es-ES': {
    name: 'Nombre',
    email: 'Correo electrónico',
    send: 'Enviar código',
    code: 'Código de acceso',
    signIn: 'Iniciar sesión',
    consent: 'Consentimiento informado',
    accept: 'He leído el consentimiento y acepto participar',
    refuse: 'No acepto',
    declined: 'Gracias por considerar el estudio',
    again: 'Leer el consentimiento de nuevo',
    skill: 'Tu habilidad profesional',
    skillOption: 'Analista de pruebas / QA',
    other: 'Otro (describir)',
    otherField: 'Describe tu habilidad',
    continue: 'Continuar',
    screening: 'Selección',
    yes: 'Sí, en la práctica profesional',
    no: 'No',
    closed: 'Gracias por tu interés',
    referenceHeading: 'Texto de referencia (portugués, Brasil)',
    referenceLink: 'Leer el texto de referencia (pt-BR)',
    label: 'Español (España)',
  },
} as const;

type Locale = keyof typeof SCREENS;
const LOCALES = Object.keys(SCREENS) as Locale[];

/** A new participant signs in on the screens of `locale` and lands on the consent form. */
async function signIn(page: Page, request: APIRequestContext, locale: Locale, email: string): Promise<void> {
  const screen = SCREENS[locale];
  const previousId = await newestMessageId(request, email);
  await page.goto(`/${locale}/participate`);
  await page.getByLabel(screen.name, { exact: true }).fill('Ana Souza');
  await page.getByLabel(screen.email, { exact: true }).fill(email);
  await page.getByRole('button', { name: screen.send }).click();
  await expect(page).toHaveURL(new RegExp(`/${locale}/participate/code$`), { timeout: 15_000 });
  const { code } = await latestMessageTo(request, email, previousId);
  await page.getByLabel(screen.code).fill(code);
  await page.getByRole('button', { name: screen.signIn }).click();
  await expect(page).toHaveURL(new RegExp(`/${locale}/consent$`));
  await expect(page.getByRole('heading', { name: screen.consent })).toBeVisible();
}

const addressOf = (name: string, locale: string, project: string) =>
  `e2e-onb-${name}-${locale.toLowerCase()}-${project}-${Date.now()}@example.com`;

async function api(page: Page, path: string, locale: Locale) {
  const response = await page.request.get(path, { headers: { 'Accept-Language': locale } });
  expect(response.status()).toBe(200);
  return response.json();
}

async function switchLanguage(page: Page, from: Locale, to: Locale): Promise<void> {
  const current = SCREENS[from].label.replace(/[()]/g, '\\$&');
  await page.getByRole('button', { name: new RegExp(`: ${current}$`) }).click();
  await page.getByRole('menuitem', { name: SCREENS[to].label }).click();
}

for (const locale of LOCALES) {
  test(`a new participant accepts, chooses a skill and answers the screening in ${locale} (T016, T020, T024, T027)`, async ({
    page,
    request,
  }, testInfo) => {
    test.setTimeout(90_000);
    const screen = SCREENS[locale];
    // The labeling asks for a block as soon as it opens; answer that call here, so these participants do not
    // take tasks of the queue that the queue specs count on.
    await page.route('**/api/v1/tasks/reservations', (route) =>
      route.fulfill({ status: 200, json: { outcome: 'no-items-yet' } }),
    );
    await signIn(page, request, locale, addressOf('flow', locale, testInfo.project.name));

    // Consent: the translation note and the pt-BR reference only outside pt-BR.
    const note = page.locator('p.translation');
    if (locale === 'pt-BR') {
      await expect(note).toHaveCount(0);
    } else {
      await expect(note).toBeVisible();
      const popupPromise = page.waitForEvent('popup');
      await page.getByRole('link', { name: screen.referenceLink }).click();
      const reference = await popupPromise;
      await expect(reference).toHaveURL(new RegExp(`/${locale}/consent/reference/fixture-1$`));
      await expect(reference.getByRole('heading', { name: screen.referenceHeading })).toBeVisible();
      await expect(reference.locator('[lang="pt-BR"]').filter({ hasText: 'TERMO DE CONSENTIMENTO' })).toBeVisible();
      await expect(reference.locator('html')).toHaveAttribute('lang', locale);
      await reference.close();
    }
    await expect(page.getByRole('region').first()).toContainText('FIXTURE');
    await page.getByRole('button', { name: screen.accept }).click();

    // Skill: Continue is disabled until a valid answer.
    await expect(page).toHaveURL(new RegExp(`/${locale}/skill$`));
    await expect(page.getByRole('heading', { name: screen.skill })).toBeVisible();
    const proceed = page.getByRole('button', { name: screen.continue });
    await expect(proceed).toBeDisabled();
    await page.getByRole('radio', { name: screen.other, exact: true }).check();
    await expect(proceed).toBeDisabled();
    await page.getByLabel(screen.otherField).fill('Technical writer');
    await expect(proceed).toBeEnabled();
    await page.getByRole('radio', { name: screen.skillOption, exact: true }).check();
    await expect(page.getByLabel(screen.otherField)).toHaveCount(0);
    await proceed.click();

    // Screening: the example is English in every locale.
    await expect(page).toHaveURL(new RegExp(`/${locale}/screening$`));
    await expect(page.getByRole('heading', { name: screen.screening, exact: true })).toBeVisible();
    await expect(page.locator('.cards[lang="en"]')).toContainText('Reset password');
    const submit = page.getByRole('button', { name: screen.continue });
    await expect(submit).toBeDisabled();
    await page.getByRole('radio', { name: screen.yes, exact: true }).check();
    await submit.click();

    // Eligible: the welcome resolver opens the labeling, which asks for a block (answered above).
    await expect(page).toHaveURL(new RegExp(`/${locale}/label/done\\?outcome=no-items-yet$`));

    // What was stored: the language of the text shown and the English codes.
    const consent = await api(page, '/api/v1/participants/me/consent', locale);
    expect(consent).toMatchObject({
      decision: 'accepted',
      consentFormVersion: 'fixture-1',
      language: locale,
      current: true,
    });
    expect(await api(page, '/api/v1/participants/me/skill', locale)).toMatchObject({
      skill: 'test_analyst',
      skillOther: null,
    });
    expect(await api(page, '/api/v1/participants/me/screening', locale)).toMatchObject({
      answer: 'yes_professional',
      eligible: true,
    });
  });
}

test('a participant who refuses can read the form again and accept (T016)', async ({ page, request }, testInfo) => {
  test.setTimeout(60_000);
  const screen = SCREENS['pt-BR'];
  await signIn(page, request, 'pt-BR', addressOf('refuse', 'pt-BR', testInfo.project.name));

  await page.getByRole('button', { name: screen.refuse }).click();

  await expect(page).toHaveURL(/\/pt-BR\/consent\/declined$/);
  await expect(page.getByRole('heading', { name: screen.declined })).toBeVisible();
  await expect(page.getByRole('link', { name: /label/ })).toHaveCount(0);
  const decision = await api(page, '/api/v1/participants/me/consent', 'pt-BR');
  expect(decision).toMatchObject({ decision: 'refused', language: 'pt-BR' });

  // Back on the welcome route the pending step is still the consent form.
  await page.goto('/pt-BR/welcome');
  await expect(page).toHaveURL(/\/pt-BR\/consent$/);
  await page.goto('/pt-BR/consent/declined');
  await page.getByRole('link', { name: screen.again }).click();
  await expect(page).toHaveURL(/\/pt-BR\/consent$/);
  await page.getByRole('button', { name: screen.accept }).click();
  await expect(page).toHaveURL(/\/pt-BR\/skill$/);
});

test('a participant who answers "No" gets the closing message and cannot label (T024)', async ({ page, request }, testInfo) => {
  test.setTimeout(60_000);
  const screen = SCREENS['pt-BR'];
  await signIn(page, request, 'pt-BR', addressOf('no', 'pt-BR', testInfo.project.name));
  await page.getByRole('button', { name: screen.accept }).click();
  await page.getByRole('radio', { name: screen.skillOption, exact: true }).check();
  await page.getByRole('button', { name: screen.continue }).click();
  await expect(page).toHaveURL(/\/pt-BR\/screening$/);

  await page.getByRole('radio', { name: screen.no, exact: true }).check();
  await page.getByRole('button', { name: screen.continue }).click();

  await expect(page).toHaveURL(/\/pt-BR\/screening\/closed$/);
  await expect(page.getByRole('heading', { name: screen.closed })).toBeVisible();

  // The same message after a reload, and every other screen leads back to it.
  for (const route of ['/pt-BR/welcome', '/pt-BR/label', '/pt-BR/consent', '/pt-BR/skill', '/pt-BR/screening']) {
    await page.goto(route);
    await expect(page).toHaveURL(/\/pt-BR\/screening\/closed$/);
    await expect(page.getByRole('heading', { name: screen.closed })).toBeVisible();
  }

  // The gate of the API says the same, and offers no way to a block.
  const reservation = await page.request.post('/api/v1/tasks/reservations');
  expect(reservation.status()).toBe(403);
  expect((await reservation.json()).type).toBe('urn:veredito:problem:not-eligible');
  const entry = await (await page.request.get('/api/v1')).json();
  expect(entry._links['vd:next-task']).toBeUndefined();
});

test('typing the labeling address before finishing returns to the pending step (T024)', async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(60_000);
  const screen = SCREENS['pt-BR'];
  await signIn(page, request, 'pt-BR', addressOf('pending', 'pt-BR', testInfo.project.name));

  await page.goto('/pt-BR/label');
  await expect(page).toHaveURL(/\/pt-BR\/consent$/);

  await page.getByRole('button', { name: screen.accept }).click();
  await expect(page).toHaveURL(/\/pt-BR\/skill$/);
  await page.goto('/pt-BR/label');
  await expect(page).toHaveURL(/\/pt-BR\/skill$/);
  await page.goto('/pt-BR/consent');
  await expect(page).toHaveURL(/\/pt-BR\/skill$/);

  await page.getByRole('radio', { name: screen.skillOption, exact: true }).check();
  await page.getByRole('button', { name: screen.continue }).click();
  await expect(page).toHaveURL(/\/pt-BR\/screening$/);
  await page.reload();
  await expect(page).toHaveURL(/\/pt-BR\/screening$/);
  await page.goto('/pt-BR/label');
  await expect(page).toHaveURL(/\/pt-BR\/screening$/);
});

test('switching the language on the screens records nothing and keeps the typed values (T027, T019)', async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(60_000);
  await signIn(page, request, 'pt-BR', addressOf('switch', 'pt-BR', testInfo.project.name));

  await switchLanguage(page, 'pt-BR', 'es-ES');
  await expect(page).toHaveURL(/\/es-ES\/consent$/);
  await expect(page.getByRole('heading', { name: SCREENS['es-ES'].consent })).toBeVisible();
  await expect(page.getByRole('region').first()).toContainText('FORMULARIO DE CONSENTIMIENTO');
  expect(await api(page, '/api/v1/participants/me/consent', 'es-ES')).toMatchObject({
    decision: null,
    language: null,
    current: false,
  });

  await page.getByRole('button', { name: SCREENS['es-ES'].accept }).click();
  await expect(page).toHaveURL(/\/es-ES\/skill$/);
  await page.getByRole('radio', { name: SCREENS['es-ES'].other, exact: true }).check();
  await page.getByLabel(SCREENS['es-ES'].otherField).fill('Redactora');

  await switchLanguage(page, 'es-ES', 'en-US');

  await expect(page).toHaveURL(/\/en-US\/skill$/);
  await expect(page.getByRole('radio', { name: SCREENS['en-US'].other, exact: true })).toBeChecked();
  await expect(page.getByLabel(SCREENS['en-US'].otherField)).toHaveValue('Redactora');
  expect(await api(page, '/api/v1/participants/me/skill', 'en-US')).toMatchObject({ skill: null });
  expect(await api(page, '/api/v1/participants/me/consent', 'en-US')).toMatchObject({
    decision: 'accepted',
    language: 'es-ES',
  });
});
