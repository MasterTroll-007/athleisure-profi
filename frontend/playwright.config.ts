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
    {
      name: 'chromium',
      testIgnore: [/mobile-smoke\.spec\.ts/, /cross-browser-smoke\.spec\.ts/],
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'mobile-chrome',
      testMatch: /mobile-smoke\.spec\.ts/,
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'firefox-smoke',
      testMatch: /cross-browser-smoke\.spec\.ts/,
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit-smoke',
      testMatch: /cross-browser-smoke\.spec\.ts/,
      use: { ...devices['Desktop Safari'] },
    },
  ],
})
