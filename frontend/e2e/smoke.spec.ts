import { test, expect } from '@playwright/test'

/**
 * Smoke — whole stack is alive. Requires `docker compose up` locally.
 */
test('homepage returns 200 and renders', async ({ page }) => {
  const response = await page.goto('/')
  expect(response?.status()).toBeLessThan(400)
  await expect(page.locator('body')).toBeVisible()
})
