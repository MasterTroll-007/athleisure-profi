import { expect, test } from '@playwright/test'
import { E2E_USERS } from '../fixtures/api'
import { resetE2eData } from '../fixtures/db'
import {
  expectNoHorizontalOverflow,
  expectNoWrappedActionButtons,
  seedBrowserPreferences,
  uiLogin,
} from '../fixtures/ui'

test.use({ viewport: { width: 1440, height: 1000 } })

test.beforeEach(async ({ page }) => {
  await resetE2eData()
  await seedBrowserPreferences(page)
})

test('admin calendar keeps toolbar buttons and time grid visually stable', async ({ page }) => {
  await uiLogin(page, E2E_USERS.admin)
  await page.goto('/calendar')

  const calendarPage = page.getByTestId('admin-calendar-page')
  await expect(calendarPage).toBeVisible()
  await expect(page.locator('[data-testid="reservation-slot"]').first()).toBeVisible({ timeout: 15_000 })
  await expectNoHorizontalOverflow(page)
  await expectNoWrappedActionButtons(page)

  const slots = await page.locator('[data-testid="reservation-slot"]').evaluateAll((elements) =>
    elements.map((element) => {
      const rect = element.getBoundingClientRect()
      return {
        width: rect.width,
        height: rect.height,
        text: (element as HTMLElement).innerText.trim(),
      }
    })
  )
  expect(slots.length).toBeGreaterThan(0)
  expect(slots.every((slot) => slot.width >= 40 && slot.height >= 18)).toBe(true)
  expect(slots.some((slot) => slot.text.length > 0)).toBe(true)
})

test('admin pricing and accounting pages do not regress into wrapped controls or horizontal overflow', async ({ page }) => {
  await uiLogin(page, E2E_USERS.admin)

  for (const path of ['/admin/pricing', '/admin/accounting']) {
    await page.goto(path)
    await expect(page.locator('main')).toBeVisible()
    await expectNoHorizontalOverflow(page)
    await expectNoWrappedActionButtons(page)
  }
})
