import { defineConfig, devices } from '@playwright/test';

const ci = !!process.env['CI'];

export default defineConfig({
  testDir: '.',
  outputDir: '../test-results',
  globalSetup: './global-setup.ts',
  fullyParallel: true,
  forbidOnly: ci,
  retries: ci ? 1 : 0,
  reporter: ci ? [['list'], ['html', { outputFolder: '../playwright-report', open: 'never' }]] : 'list',
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:8080',
    trace: 'retain-on-failure',
  },
  // The queue specs (spec 007) depend on the state of the database, so they bracket the others on a
  // freshly started local staging: queue-empty (nothing imported) -> desktop and phone (the
  // administrator spec imports the Minitest batch) -> queue (labels blocks of that batch) -> item (the rich item screen of spec 008, on the blocks left).
  projects: [
    { name: 'queue-empty', testMatch: 'queue-empty.spec.ts', use: { ...devices['Desktop Chrome'] } },
    {
      name: 'desktop',
      testIgnore: /(queue.*|item-screen)\.spec\.ts/,
      dependencies: ['queue-empty'],
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'phone',
      testIgnore: /(queue.*|item-screen)\.spec\.ts/,
      dependencies: ['queue-empty'],
      use: { ...devices['Desktop Chrome'], viewport: { width: 360, height: 740 }, isMobile: true, hasTouch: true },
    },
    {
      name: 'queue',
      testMatch: 'queue.spec.ts',
      dependencies: ['desktop', 'phone'],
      use: { ...devices['Desktop Chrome'] },
    },
    // Spec 008: the item screen, on the blocks that queue leaves free (second role of blocks 2 to 4).
    {
      name: 'item',
      testMatch: 'item-screen.spec.ts',
      dependencies: ['queue'],
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
