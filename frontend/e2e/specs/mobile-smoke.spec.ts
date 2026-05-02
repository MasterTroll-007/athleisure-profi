import { expect, test } from '@playwright/test'
import { E2E_PASSWORD, E2E_USERS } from '../fixtures/api'
import { resetE2eData } from '../fixtures/db'

test.beforeEach(async ({ page }) => {
  await resetE2eData()
  await page.addInitScript(() => {
    localStorage.setItem('cookieConsent', 'accepted')
    localStorage.setItem('calendarView', '3')
    localStorage.setItem('locale', 'cs')
  })
})

test('mobile client can sign in and open reservation calendar', async ({ page }) => {
  await page.goto('/login')
  await page.getByTestId('login-email').fill(E2E_USERS.client1)
  await page.getByTestId('login-password').fill(E2E_PASSWORD)
  await page.getByTestId('login-submit').click()

  await expect(page).not.toHaveURL(/\/login$/, { timeout: 15_000 })

  await page.goto('/calendar')
  await expect(page.getByTestId('reservation-calendar-page')).toBeVisible()
  await expect(page.getByTestId('calendar-next')).toBeVisible()
})
