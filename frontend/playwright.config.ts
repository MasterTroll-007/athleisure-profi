import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright E2E config. Runs against a locally-started docker-compose stack
 * (backend + frontend + db). Assumes `http://localhost:3000` is reachable.
 *
 * For CI: start the stack with `docker compose -f docker-compose.local.yml up -d`
 * before `npx playwright test`.
 */
export default defineConfig({
  testDir: './e2e/specs',
  timeout: 45_000,
  globalSetup: './e2e/global-setup.ts',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
})
