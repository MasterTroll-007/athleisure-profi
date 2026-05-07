import { expect, test, type Page } from '@playwright/test'
import { E2E_USERS } from '../fixtures/api'
import { resetE2eData } from '../fixtures/db'
import { expectNoHorizontalOverflow, seedBrowserPreferences, uiLogin } from '../fixtures/ui'

test.beforeEach(async ({ page }) => {
  await resetE2eData()
  await seedBrowserPreferences(page)
})

async function spaNavigate(page: Page, path: string): Promise<void> {
  await page.evaluate((targetPath) => {
    window.history.pushState({}, '', targetPath)
    window.dispatchEvent(new PopStateEvent('popstate'))
  }, path)
}

test('client calendar renders in secondary desktop browsers', async ({ page }) => {
  await uiLogin(page, E2E_USERS.client1)
  await spaNavigate(page, '/calendar')

  await expect(page.getByTestId('reservation-calendar-page')).toBeVisible()
  await expect(page.locator('[data-testid="reservation-slot"]').first()).toBeVisible({ timeout: 15_000 })
  await expectNoHorizontalOverflow(page)
})

test('admin settings and credit package screens render in secondary desktop browsers', async ({ page }) => {
  await uiLogin(page, E2E_USERS.admin)

  await spaNavigate(page, '/admin/settings')
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await spaNavigate(page, '/admin/pricing')
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  await expectNoHorizontalOverflow(page)
})
