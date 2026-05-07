import { expect, type Page } from '@playwright/test'
import { E2E_PASSWORD } from './api'

export async function seedBrowserPreferences(page: Page, calendarView = '7'): Promise<void> {
  await page.addInitScript((view) => {
    localStorage.setItem('cookieConsent', 'accepted')
    localStorage.setItem('calendarView', view)
    localStorage.setItem('locale', 'cs')
    localStorage.setItem('theme', 'dark')
  }, calendarView)
}

export async function uiLogin(page: Page, email: string): Promise<void> {
  await page.goto('/login')
  await page.getByTestId('login-email').fill(email)
  await page.getByTestId('login-password').fill(E2E_PASSWORD)
  const loginResponse = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login') && response.request().method() === 'POST'
  )
  await page.getByTestId('login-submit').click()
  expect((await loginResponse).status()).toBe(200)
  await expect(page.getByTestId('profile-menu-button')).toBeVisible({ timeout: 15_000 })
}

export async function expectNoHorizontalOverflow(page: Page): Promise<void> {
  const pageMetrics = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }))

  expect(pageMetrics.scrollWidth).toBeLessThanOrEqual(pageMetrics.clientWidth + 2)
}

export async function expectNoWrappedActionButtons(page: Page, rootSelector = 'main'): Promise<void> {
  const issues = await page.locator(rootSelector).locator('button, a[role="button"]').evaluateAll((elements) =>
    elements.flatMap((element) => {
      const htmlElement = element as HTMLElement
      const style = window.getComputedStyle(htmlElement)
      const rect = htmlElement.getBoundingClientRect()
      const text = htmlElement.innerText.trim()

      if (
        !text ||
        style.display === 'none' ||
        style.visibility === 'hidden' ||
        rect.width === 0 ||
        rect.height === 0
      ) {
        return []
      }

      const lines = text.split('\n').map((line) => line.trim()).filter(Boolean)
      const iconTextSplit = lines.length > 1 && lines.some((line) => line.length <= 3)
      const clipsHorizontally = htmlElement.scrollWidth > htmlElement.clientWidth + 2

      return iconTextSplit || clipsHorizontally
        ? [`${text.replace(/\s+/g, ' ')} (${htmlElement.clientWidth}/${htmlElement.scrollWidth})`]
        : []
    })
  )

  expect(issues).toEqual([])
}
