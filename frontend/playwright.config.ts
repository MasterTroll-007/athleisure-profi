import { defineConfig, devices } from '@playwright/test'
import { getSafeE2eUrls } from './e2e/safety'

const { frontendUrl } = getSafeE2eUrls()

/**
 * Playwright E2E config. It is intentionally guarded to run only in CI/CD
 * against the local docker-compose stack. Production URLs are blocked before
 * any browser/API request is made.
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
    baseURL: frontendUrl,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
})
