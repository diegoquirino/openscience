import { join } from 'node:path';
import { expect, test, type BrowserContext, type Locator, type Page } from '@playwright/test';
import {
  FIELD_LABELS,
  IMPACT_EXPLANATIONS,
  IMPACT_LABELS,
  IMPACT_STATEMENT,
  KIND_LABELS,
  LOCALES,
  MISSING_USECASE,
  NONE,
  OPERATION_EXPLANATIONS,
  OPERATION_LABELS,
  OPERATION_STATEMENT,
  PRECEDENCE_INTRO,
  PRECEDENCE_ORDER,
  SCREEN,
  type Impact,
  type Kind,
  type Locale,
  type Operation,
} from './support/appendix-texts';
import { answerAllOverApi, reserveOverApi, signInEligibleParticipant, type HeldTask } from './support/participant';

// Spec 008: the rich item screen. Runs after `queue.spec.ts` (project `item`) on the same freshly started
// staging: the Minitest batch is imported and only the second role of blocks 2 to 4 is still free, which
// together hold an item of every kind (rows 1 to 38 of the batch). Serial, no retries: each test builds on
// the block held by the previous one.
test.describe.configure({ mode: 'serial', retries: 0 });

/** Optional screenshots for a visual review: set E2E_SHOTS to a folder. */
async function shot(page: Page, name: string): Promise<void> {
  const folder = process.env['E2E_SHOTS'];
  if (folder) {
    await page.screenshot({ path: join(folder, `${name}.png`), fullPage: true });
  }
}

const PIPELINE_TEXT = '(the use case does not exist in this version)';
const EDITED_MARKER = /[ \t]*<- edited[ \t]*$/;

interface ApiItem {
  position: number;
  itemId: number;
  system: string;
  uc: string;
  kind: Kind;
  field: string;
  originVersion: string;
  targetVersion: string;
  originUsecase: string;
  targetUsecase: string;
  origin: string | null;
  target: string | null;
}

async function readBlock(page: Page, task: HeldTask): Promise<ApiItem[]> {
  const items: ApiItem[] = [];
  for (let position = 1; position <= task.size; position++) {
    const response = await page.request.get(`/api/v1/tasks/${task.id}/items/${position}`);
    expect(response.status()).toBe(200);
    items.push((await response.json()) as ApiItem);
  }
  return items;
}

async function submitOverApi(page: Page, task: HeldTask): Promise<void> {
  await answerAllOverApi(page, task);
  const submitted = await page.request.post(`/api/v1/tasks/${task.id}/submission`);
  expect(submitted.status()).toBe(200);
}

function lineContents(text: string): string[] {
  const rows = text.split(/\r\n|\r|\n/);
  if (rows.length > 1 && rows[rows.length - 1] === '') {
    rows.pop();
  }
  return rows.map((row) => row.replace(EDITED_MARKER, '').replace(/^[ \t]*/, ''));
}

function editedCount(text: string): number {
  return text.split(/\r\n|\r|\n/).filter((row) => EDITED_MARKER.test(row)).length;
}

function column(page: Page, side: 'origin' | 'target'): Locator {
  return page.locator(`.column[data-side=${side}]`);
}

async function rawText(locator: Locator): Promise<string | null> {
  return locator.textContent();
}

/** The header, the edit blocks and the two buttons of one item, as Appendix A and the spec 008 layout say. */
async function expectEdit(page: Page, locale: Locale, item: ApiItem): Promise<void> {
  const screen = SCREEN[locale];
  const field = FIELD_LABELS[locale][item.field] ?? item.field;
  await expect(page.locator('.location')).toHaveText(`${field} · ${KIND_LABELS[locale][item.kind]}`);
  await expect(page.locator('.system')).toHaveText(item.system);
  await expect(page.locator('.uc')).toHaveText(item.uc);

  const origin = item.origin ?? '';
  const target = item.target ?? '';
  const originBlock = column(page, 'origin').locator('.block');
  const targetBlock = column(page, 'target').locator('.block');
  const titles = {
    MOD: [screen.before, screen.after],
    META: [screen.metaBefore(field), screen.metaAfter(field)],
    ADD: [null, screen.added],
    DEL: [screen.removed, null],
  }[item.kind];

  for (const [block, title, text] of [
    [originBlock, titles[0], origin],
    [targetBlock, titles[1], target],
  ] as const) {
    if (title === null) {
      await expect(block).toHaveCount(0);
      continue;
    }
    await expect(block.locator('.block-title')).toHaveText(title);
    if (text === '') {
      await expect(block.locator('.text')).toHaveText(NONE[locale]);
    } else {
      // The text reaches the screen exactly as captured, line breaks included.
      expect(await rawText(block.locator('.text'))).toBe(text);
    }
  }

  if (item.kind === 'MOD' && origin !== target) {
    // Something differs: removed words on the left and/or added words on the right (never the other way round).
    expect((await originBlock.locator('del').count()) + (await targetBlock.locator('ins').count())).toBeGreaterThan(0);
    expect(await originBlock.locator('ins').count()).toBe(0);
    expect(await targetBlock.locator('del').count()).toBe(0);
  }
  if (item.kind === 'ADD') {
    await expect(targetBlock.locator('ins')).toHaveCount(1);
  }
  if (item.kind === 'DEL') {
    await expect(originBlock.locator('del')).toHaveCount(1);
  }
  if (item.kind === 'META' && target !== '') {
    await expect(targetBlock.locator('ins').first()).toBeVisible();
  }

  await expect(column(page, 'origin').locator('button.open-usecase')).toHaveText(screen.viewUsecase(item.originVersion));
  await expect(column(page, 'target').locator('button.open-usecase')).toHaveText(screen.viewUsecase(item.targetVersion));
}

/** Statements, labels, explanations and the precedence rule of Appendices B.1 and B.2. */
async function expectQuestions(page: Page, locale: Locale): Promise<void> {
  await expect(page.locator('.impact legend')).toHaveText(IMPACT_STATEMENT[locale]);
  for (const code of ['LOW', 'HIGH'] as Impact[]) {
    await expect(page.getByRole('radio', { name: IMPACT_LABELS[locale][code], exact: true })).toBeVisible();
    await expect(page.locator(`#impact-${code}`)).toHaveText(IMPACT_EXPLANATIONS[locale][code]);
  }
  await expect(page.locator('.operation legend')).toHaveText(OPERATION_STATEMENT[locale]);
  await expect(page.locator('.operation input[type=radio]')).toHaveCount(8);
  for (const code of PRECEDENCE_ORDER) {
    await expect(page.getByRole('radio', { name: OPERATION_LABELS[locale][code], exact: true })).toBeVisible();
    await expect(page.locator(`#operation-${code}`)).toHaveText(OPERATION_EXPLANATIONS[locale][code]);
  }
  await expect(page.locator('.precedence .intro')).toHaveText(PRECEDENCE_INTRO[locale]);
  await expect(page.locator('.precedence .order')).toHaveText(
    PRECEDENCE_ORDER.map((code) => OPERATION_LABELS[locale][code]).join(' > '),
  );
}

/** Opens a use case modal and checks its title, lines, highlighted lines and scroll position. */
async function expectModal(page: Page, locale: Locale, item: ApiItem, side: 'origin' | 'target'): Promise<void> {
  const screen = SCREEN[locale];
  const text = side === 'origin' ? item.originUsecase : item.targetUsecase;
  const version = side === 'origin' ? item.originVersion : item.targetVersion;
  const button = column(page, side).locator('button.open-usecase');
  await button.click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  const title = side === 'origin' ? screen.previousTitle(item.uc, version) : screen.modifiedTitle(item.uc, version);
  await expect(dialog.locator('.dialog-title')).toHaveText(title);
  await expect(dialog).toHaveAccessibleName(title);

  if (text.trim() === PIPELINE_TEXT) {
    await expect(dialog.locator('.missing-usecase')).toHaveText(MISSING_USECASE[locale]);
    await expect(dialog.locator('.usecase-line')).toHaveCount(0);
  } else {
    const expected = lineContents(text);
    await expect(dialog.locator('.usecase-line')).toHaveCount(expected.length);
    const shown = await dialog.locator('.usecase-line').allTextContents();
    expect(shown).toEqual(expected);
    expect(await dialog.textContent()).not.toContain('<- edited');
    await expect(dialog.locator('.usecase-line.edited')).toHaveCount(editedCount(text));
    const fontFamily = await dialog.locator('.usecase').evaluate((element) => getComputedStyle(element).fontFamily);
    expect(fontFamily).toMatch(/mono|Consolas|Menlo/i);

    if (editedCount(text) > 0) {
      // Opened already scrolled to the first highlighted line.
      const first = dialog.locator('.usecase-line.edited').first();
      await expect(first).toBeVisible();
      const inside = await first.evaluate((line) => {
        const body = line.closest('.body') as HTMLElement;
        const lineBox = line.getBoundingClientRect();
        const bodyBox = body.getBoundingClientRect();
        return lineBox.top >= bodyBox.top - 1 && lineBox.bottom <= bodyBox.bottom + 1;
      });
      expect(inside).toBe(true);
    }
  }
  // The counter and the countdown stay visible with the modal open.
  await expect(dialog.locator('.counter')).toBeVisible();
  await expect(dialog.getByRole('timer')).toBeVisible();
}

async function closeModal(page: Page, how: 'button' | 'escape' | 'outside'): Promise<void> {
  const dialog = page.getByRole('dialog');
  if (how === 'button') {
    await dialog.getByRole('button').last().click();
  } else if (how === 'escape') {
    await page.keyboard.press('Escape');
  } else {
    await page.locator('.cdk-overlay-backdrop').click({ position: { x: 3, y: 3 } });
  }
  await expect(page.locator('mat-dialog-container')).toHaveCount(0);
}

test.describe('the four kinds, in every locale', () => {
  let context: BrowserContext;
  let page: Page;
  let held: HeldTask;
  let items: ApiItem[];
  const verified = new Set<Kind>();
  const questionsChecked = new Set<Locale>();

  test.beforeAll(async ({ browser, baseURL, request }) => {
    test.setTimeout(90_000);
    context = await browser.newContext({ baseURL });
    page = await context.newPage();
    await signInEligibleParticipant(page, request, 'Dora Reis', `e2e-item-${Date.now()}@example.com`);
  });

  test.afterAll(async () => {
    await context.close();
  });

  /** One item of each kind of the held block, on screen, in the three locales. */
  async function verifyBlock(): Promise<void> {
    held = await reserveOverApi(page);
    items = await readBlock(page, held);
    for (const kind of ['MOD', 'ADD', 'DEL', 'META'] as Kind[]) {
      if (verified.has(kind)) {
        continue;
      }
      const item = items.find((candidate) => candidate.kind === kind);
      if (!item) {
        continue;
      }
      for (const locale of LOCALES) {
        await page.goto(`/${locale}/label/items/${item.position}`);
        await expect(page.locator('.location')).toBeVisible();
        await expectEdit(page, locale, item);
        await shot(page, `${item.kind}-${locale}`);
        if (!questionsChecked.has(locale)) {
          await expectQuestions(page, locale);
          questionsChecked.add(locale);
        }
      }
      verified.add(kind);
    }
  }

  test('MOD and ADD items follow Appendices A and B in pt-BR, en-US and es-ES (US1, US3, US4, SC-004)', async () => {
    test.setTimeout(180_000);
    await verifyBlock();
    expect(verified.has('MOD')).toBe(true);
    expect(verified.has('ADD')).toBe(true);
  });

  test('both modals close by button, Esc and click outside; choices and focus survive (US2, US3)', async () => {
    test.setTimeout(120_000);
    const item = items.find((candidate) => candidate.kind === 'ADD' && editedCount(candidate.targetUsecase) > 0) ?? items[0];
    await page.goto(`/en-US/label/items/${item.position}`);
    await page.getByRole('radio', { name: 'Yes (high impact)', exact: true }).check();
    await page.getByRole('radio', { name: 'Flag', exact: true }).check();
    await page.getByLabel('Comment (optional)').fill('typed before the modal');

    const sides = ['origin', 'target'] as const;
    const ways = ['button', 'escape', 'outside'] as const;
    for (const [index, how] of ways.entries()) {
      const side = sides[index % 2];
      await expectModal(page, 'en-US', item, side);
      await closeModal(page, how);
      await expect(column(page, side).locator('button.open-usecase')).toBeFocused();
      await expect(page.getByRole('radio', { name: 'Yes (high impact)', exact: true })).toBeChecked();
      await expect(page.getByRole('radio', { name: 'Flag', exact: true })).toBeChecked();
      await expect(page.getByLabel('Comment (optional)')).toHaveValue('typed before the modal');
    }
    // Both versions of the same item.
    await expectModal(page, 'en-US', item, 'origin');
    await closeModal(page, 'escape');
    await expectModal(page, 'en-US', item, 'target');
    await closeModal(page, 'button');
  });

  test('buttons, options and modal are reachable by keyboard; focus returns to the button (US3 scenario 4)', async () => {
    const item = items[0];
    await page.goto(`/es-ES/label/items/${item.position}`);
    const button = column(page, 'target').locator('button.open-usecase');
    await button.focus();
    await page.keyboard.press('Enter');
    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByRole('button', { name: SCREEN['es-ES'].close })).toBeFocused();
    await page.keyboard.press('Escape');
    await expect(page.locator('mat-dialog-container')).toHaveCount(0);
    await expect(button).toBeFocused();

    // The radio groups answer to the arrow keys.
    await page.getByRole('radio', { name: 'Bajo', exact: true }).focus();
    await page.keyboard.press('ArrowRight');
    await expect(page.getByRole('radio', { name: 'Sim (alto impacto)', exact: true })).toBeChecked();
  });

  test('every locale answers the same stored codes, with the language of the save (FR-011, SC-004)', async () => {
    test.setTimeout(120_000);
    const answers: { locale: Locale; position: number }[] = [
      { locale: 'pt-BR', position: 1 },
      { locale: 'en-US', position: 2 },
      { locale: 'es-ES', position: 3 },
    ];
    for (const { locale, position } of answers) {
      await page.goto(`/${locale}/label/items/${position}`);
      await page.getByRole('radio', { name: IMPACT_LABELS[locale].HIGH, exact: true }).check();
      await page.getByRole('radio', { name: OPERATION_LABELS[locale].UPDATE, exact: true }).check();
      await page.locator('button[type=submit]').click();
      await expect(page).not.toHaveURL(new RegExp(`/label/items/${position}$`));
      const saved = await (await page.request.get(`/api/v1/tasks/${held.id}/items/${position}`)).json();
      expect(saved.draft.changeImpact).toBe('HIGH');
      expect(saved.draft.maintenanceOperation).toBe('UPDATE');
      expect(saved.draft.language).toBe(locale);
    }
  });

  test('a missing use case version and an unknown field are shown as the spec says (FR-009, FR-010)', async () => {
    test.setTimeout(120_000);
    const base = items.find((candidate) => candidate.kind === 'MOD') ?? items[0];
    await page.route(new RegExp(`/api/v1/tasks/${held.id}/items/${base.position}$`), async (route) => {
      const response = await route.fetch();
      const json = await response.json();
      json.targetUsecase = `\n  ${PIPELINE_TEXT}  \n`;
      json.field = 'custom_field';
      await route.fulfill({ response, json });
    });
    const mocked: ApiItem = { ...base, field: 'custom_field', targetUsecase: PIPELINE_TEXT };
    try {
      for (const locale of LOCALES) {
        await page.goto(`/${locale}/label/items/${base.position}`);
        await expect(page.locator('.location')).toHaveText(`custom_field · ${KIND_LABELS[locale][base.kind]}`);
        await expectModal(page, locale, mocked, 'target');
        // The previous version is still shown as captured.
        await closeModal(page, 'escape');
        await expectModal(page, locale, mocked, 'origin');
        await closeModal(page, 'escape');
      }
    } finally {
      await page.unroute(new RegExp(`/api/v1/tasks/${held.id}/items/${base.position}$`));
    }
    // Nothing stored changed: the real item still holds the captured text.
    const stored = await (await page.request.get(`/api/v1/tasks/${held.id}/items/${base.position}`)).json();
    expect(stored.targetUsecase).toBe(base.targetUsecase);
  });

  test('DEL and META items in the next block, with the empty side as (none)', async () => {
    test.setTimeout(180_000);
    await submitOverApi(page, held);
    await verifyBlock();
    if (!verified.has('DEL') || !verified.has('META')) {
      await submitOverApi(page, held);
      await verifyBlock();
    }
    expect([...verified].sort()).toEqual(['ADD', 'DEL', 'META', 'MOD']);
  });

  test('narrow screen: blocks stacked, no horizontal scroll, full-screen modal with the countdown (US1, US2)', async ({
    browser,
    baseURL,
  }) => {
    test.setTimeout(120_000);
    const phone = await browser.newContext({
      baseURL,
      storageState: await context.storageState(),
      viewport: { width: 360, height: 740 },
      isMobile: true,
      hasTouch: true,
    });
    const small = await phone.newPage();
    try {
      held = await reserveOverApi(small);
      items = await readBlock(small, held);
      const item = items.find((candidate) => candidate.kind === 'MOD' && candidate.originUsecase.length > 400) ?? items[0];
      await small.goto(`/en-US/label/items/${item.position}`);
      await expect(small.locator('.location')).toBeVisible();
      await shot(small, 'phone-item');

      const boxes = await Promise.all(
        (['origin', 'target'] as const).map((side) => column(small, side).boundingBox()),
      );
      expect(boxes[0] && boxes[1] && boxes[0].y + boxes[0].height <= boxes[1].y + 1).toBe(true);
      expect(await small.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
      await expect(small.locator('.counter')).toBeVisible();
      await expect(small.getByRole('timer')).toBeVisible();

      await column(small, 'target').locator('button.open-usecase').click();
      const dialog = small.getByRole('dialog');
      await expect(dialog).toBeVisible();
      const box = await small.locator('mat-dialog-container').boundingBox();
      expect(box?.width).toBeGreaterThanOrEqual(359);
      expect(box?.height).toBeGreaterThanOrEqual(730);
      await expect(dialog.locator('.counter')).toBeVisible();
      await expect(dialog.getByRole('timer')).toBeVisible();
      await shot(small, 'phone-dialog');

      // Long lines wrap inside the dialog and keep the indentation of the line (hanging indent).
      const body = dialog.locator('.body');
      expect(await body.evaluate((element) => element.scrollWidth <= element.clientWidth)).toBe(true);
      const longLines = await dialog.locator('.usecase-line').evaluateAll((lines) =>
        lines
          .filter((line) => (line.textContent ?? '').length > 150)
          .map((line) => ({
            height: line.getBoundingClientRect().height,
            lineHeight: parseFloat(getComputedStyle(line).lineHeight),
            padding: parseFloat(getComputedStyle(line).paddingLeft),
          })),
      );
      expect(longLines.length).toBeGreaterThan(0);
      for (const line of longLines) {
        expect(line.height).toBeGreaterThan(1.5 * line.lineHeight);
        expect(line.padding).toBeGreaterThan(10);
      }
      await small.keyboard.press('Escape');
      await expect(small.locator('mat-dialog-container')).toHaveCount(0);
    } finally {
      await phone.close();
    }
  });
});
