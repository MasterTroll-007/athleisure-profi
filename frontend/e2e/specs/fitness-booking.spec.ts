import { expect, test, type APIRequestContext, type Page } from '@playwright/test'
import {
  API_URL,
  E2E_PASSWORD,
  E2E_USERS,
  SEEDED_WEEK_END,
  SEEDED_WEEK_START,
  TEMPLATE_WEEK_END,
  TEMPLATE_WEEK_START,
  addDays,
  authHeaders,
  expectJson,
  findSlot,
  getAdminSlots,
  isoDate,
  loginApi,
  reservationPayload,
  type AdminSettingsDTO,
  type AuthResponse,
  type AvailableSlotsResponse,
  type CancellationRefundPreviewDTO,
  type CancellationResultDTO,
  type CreditBalanceResponse,
  type CreditTransactionDTO,
  type PageDTO,
  type ReservationDTO,
  type SlotDTO,
  type SlotTemplateDTO,
  type UserDTO,
} from '../fixtures/api'
import { activateE2eUser, resetE2eData } from '../fixtures/db'

test.use({ viewport: { width: 1440, height: 1000 } })

test.beforeEach(async ({ page }) => {
  await resetE2eData()
  await page.addInitScript(() => {
    localStorage.setItem('cookieConsent', 'accepted')
    localStorage.setItem('calendarView', '7')
    localStorage.setItem('locale', 'cs')
    localStorage.setItem('theme', 'dark')
  })
})

async function uiLogin(page: Page, email: string): Promise<void> {
  await page.goto('/login')
  await page.getByTestId('login-email').fill(email)
  await page.getByTestId('login-password').fill(E2E_PASSWORD)
  await page.getByTestId('login-submit').click()
  await expect(page).not.toHaveURL(/\/login$/, { timeout: 15_000 })
}

function nextClicksToSeededWeek(): number {
  const today = new Date()
  const dayOfWeek = today.getDay() === 0 ? 7 : today.getDay()
  const currentMonday = new Date(today)
  currentMonday.setDate(today.getDate() - dayOfWeek + 1)
  const start = new Date(`${isoDate(currentMonday)}T12:00:00`)
  const target = new Date(`${SEEDED_WEEK_START}T12:00:00`)
  const weekMs = 7 * 24 * 60 * 60 * 1000
  return Math.max(0, Math.round((target.getTime() - start.getTime()) / weekMs))
}

async function navigateToSeededWeek(page: Page): Promise<void> {
  for (let i = 0; i < nextClicksToSeededWeek(); i += 1) {
    await page.getByTestId('calendar-next').click()
    await page.waitForTimeout(350)
  }
}

async function getBalance(request: APIRequestContext, token: string): Promise<number> {
  const balance = await expectJson<CreditBalanceResponse>(
    await request.get(`${API_URL}/credits/balance`, { headers: authHeaders(token) })
  )
  return balance.balance
}

async function createReservationApi(
  request: APIRequestContext,
  token: string,
  slot: SlotDTO
): Promise<ReservationDTO> {
  return expectJson<ReservationDTO>(
    await request.post(`${API_URL}/reservations`, {
      headers: authHeaders(token),
      data: reservationPayload(slot),
    }),
    201
  )
}

async function getTransactions(
  request: APIRequestContext,
  token: string
): Promise<PageDTO<CreditTransactionDTO>> {
  return expectJson<PageDTO<CreditTransactionDTO>>(
    await request.get(`${API_URL}/credits/transactions?size=50`, { headers: authHeaders(token) })
  )
}

async function getAvailableSlots(
  request: APIRequestContext,
  token: string,
  start = SEEDED_WEEK_START,
  end = SEEDED_WEEK_END
): Promise<AvailableSlotsResponse> {
  return expectJson<AvailableSlotsResponse>(
    await request.get(`${API_URL}/reservations/available?start=${start}&end=${end}`, {
      headers: authHeaders(token),
    })
  )
}

test('auth, role guards, UI login, and logout work', async ({ page, request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  expect(admin.accessToken).toBeTruthy()
  expect(admin.user.email).toBe(E2E_USERS.admin)
  expect(admin.user.role).toBe('admin')

  const client = await loginApi(request, E2E_USERS.client1)
  expect(client.accessToken).toBeTruthy()
  expect(client.user.email).toBe(E2E_USERS.client1)
  expect(client.user.role).toBe('client')

  const me = await expectJson<UserDTO>(
    await request.get(`${API_URL}/auth/me`, { headers: authHeaders(client.accessToken) })
  )
  expect(me.email).toBe(E2E_USERS.client1)
  expect(me.role).toBe('client')

  const adminSlotsUrl = `${API_URL}/admin/slots?start=${SEEDED_WEEK_START}&end=${SEEDED_WEEK_END}`
  expect((await request.get(adminSlotsUrl)).status()).toBe(401)
  expect((await request.get(adminSlotsUrl, { headers: authHeaders(client.accessToken) })).status()).toBe(403)
  expect((await request.get(adminSlotsUrl, { headers: authHeaders(admin.accessToken) })).status()).toBe(200)

  await page.goto('/login')
  await page.getByTestId('login-email').fill('missing-e2e-user@example.com')
  await page.getByTestId('login-password').fill('Wrong1234')
  await page.getByTestId('login-submit').click()
  await expect(page.locator('.err')).toBeVisible()

  await uiLogin(page, E2E_USERS.admin)
  await page.getByTestId('profile-menu-button').click()
  await page.getByTestId('logout-button').click()
  await expect(page).toHaveURL(/\/login$/, { timeout: 15_000 })

  await uiLogin(page, E2E_USERS.client1)
  await expect(page.getByTestId('profile-menu-button')).toBeVisible()
})

test('admin can see seeded slots and templates', async ({ page, request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const slots = await getAdminSlots(request, admin.accessToken)
  const seededDates = new Set(slots.filter((slot) => slot.note === 'E2E seeded week slot').map((slot) => slot.date))
  for (let offset = 0; offset < 7; offset += 1) {
    expect(seededDates.has(addDays(SEEDED_WEEK_START, offset))).toBe(true)
  }
  for (const slot of slots) {
    expect(slot.id).toBeTruthy()
    expect(slot.date).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    expect(slot.startTime).toMatch(/^\d{2}:\d{2}$/)
    expect(slot.endTime).toMatch(/^\d{2}:\d{2}$/)
    expect(slot.status).toMatch(/locked|unlocked|reserved|cancelled|blocked/)
  }

  const templates = await expectJson<SlotTemplateDTO[]>(
    await request.get(`${API_URL}/admin/templates`, { headers: authHeaders(admin.accessToken) })
  )
  expect(templates.map((template) => template.name)).toEqual(
    expect.arrayContaining(['E2E Morning Template', 'E2E Weekend Template'])
  )

  const mondaySlot = findSlot(slots, 'E2E seeded week slot', '09:00')
  await uiLogin(page, E2E_USERS.admin)
  await page.goto('/calendar')
  await expect(page.getByTestId('admin-calendar-page')).toBeVisible()
  await navigateToSeededWeek(page)
  await expect(page.locator(`[data-testid="reservation-slot"][data-slot-id="${mondaySlot.id}"]`)).toBeVisible({
    timeout: 15_000,
  })
  await page.getByTestId('apply-template-action').first().click()
  await expect(page.locator('[data-testid="template-option"][data-template-name="E2E Morning Template"]')).toBeVisible()
  await expect(page.locator('[data-testid="template-option"][data-template-name="E2E Weekend Template"]')).toBeVisible()
})

test('template application creates locked slots and unlocking exposes them to clients', async ({ request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client = await loginApi(request, E2E_USERS.client1)
  const templates = await expectJson<SlotTemplateDTO[]>(
    await request.get(`${API_URL}/admin/templates`, { headers: authHeaders(admin.accessToken) })
  )
  const template = templates.find((candidate) => candidate.name === 'E2E Morning Template')
  expect(template).toBeTruthy()

  const applyResult = await expectJson<{ createdSlots: number; slots: SlotDTO[] }>(
    await request.post(`${API_URL}/admin/slots/apply-template`, {
      headers: authHeaders(admin.accessToken),
      data: { templateId: template?.id, weekStartDate: TEMPLATE_WEEK_START },
    })
  )
  expect(applyResult.createdSlots).toBeGreaterThan(0)
  expect(applyResult.slots.every((slot) => slot.status === 'locked')).toBe(true)

  const createdIds = new Set(applyResult.slots.map((slot) => slot.id))
  const beforeUnlock = await getAvailableSlots(request, client.accessToken, TEMPLATE_WEEK_START, TEMPLATE_WEEK_END)
  expect(beforeUnlock.slots.some((slot) => createdIds.has(slot.slotId))).toBe(false)

  const unlockResult = await expectJson<{ unlockedCount: number }>(
    await request.post(`${API_URL}/admin/slots/unlock-week`, {
      headers: authHeaders(admin.accessToken),
      data: { weekStartDate: TEMPLATE_WEEK_START, endDate: TEMPLATE_WEEK_END },
    })
  )
  expect(unlockResult.unlockedCount).toBeGreaterThan(0)

  const afterUnlock = await getAvailableSlots(request, client.accessToken, TEMPLATE_WEEK_START, TEMPLATE_WEEK_END)
  expect(afterUnlock.slots.some((slot) => createdIds.has(slot.slotId))).toBe(true)
})

test('client books a slot through UI and backend state is consistent', async ({ page, request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client = await loginApi(request, E2E_USERS.client1)
  const slots = await getAdminSlots(request, admin.accessToken)
  const slot = findSlot(slots, 'E2E seeded week slot', '09:00')
  const balanceBefore = await getBalance(request, client.accessToken)

  await uiLogin(page, E2E_USERS.client1)
  await page.goto('/calendar')
  await expect(page.getByTestId('reservation-calendar-page')).toBeVisible()
  await navigateToSeededWeek(page)

  await page.locator(`[data-testid="reservation-slot"][data-slot-id="${slot.id}"]`).click()
  await expect(page.getByTestId('reservation-confirm-button')).toBeEnabled()
  const reservationResponse = page.waitForResponse((response) =>
    response.url().endsWith('/api/reservations') && response.request().method() === 'POST'
  )
  await page.getByTestId('reservation-confirm-button').click()
  const reservation = await expectJson<ReservationDTO>(await reservationResponse, 201)

  expect(reservation.status).toBe('confirmed')
  expect(reservation.slotId).toBe(slot.id)
  expect(reservation.creditsUsed).toBe(slot.pricingItems[0].credits)

  const reservations = await expectJson<PageDTO<ReservationDTO>>(
    await request.get(`${API_URL}/reservations?scope=all&size=50`, { headers: authHeaders(client.accessToken) })
  )
  expect(reservations.content.some((candidate) => candidate.id === reservation.id)).toBe(true)

  const upcoming = await expectJson<PageDTO<ReservationDTO>>(
    await request.get(`${API_URL}/reservations/upcoming?size=50`, { headers: authHeaders(client.accessToken) })
  )
  expect(upcoming.content.some((candidate) => candidate.id === reservation.id)).toBe(true)

  expect(await getBalance(request, client.accessToken)).toBe(balanceBefore - reservation.creditsUsed)
  const transactions = await getTransactions(request, client.accessToken)
  expect(transactions.content.some((transaction) =>
    transaction.referenceId === reservation.id &&
    transaction.type === 'reservation' &&
    transaction.amount === -reservation.creditsUsed
  )).toBe(true)

  const adminSlots = await getAdminSlots(request, admin.accessToken)
  const updatedSlot = adminSlots.find((candidate) => candidate.id === slot.id)
  expect(updatedSlot?.status).toBe('reserved')
  expect(updatedSlot?.currentBookings).toBe(1)
})

test('double booking is rejected without changing the second client balance', async ({ request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client1 = await loginApi(request, E2E_USERS.client1)
  const client2 = await loginApi(request, E2E_USERS.client2)
  const slot = findSlot(await getAdminSlots(request, admin.accessToken), 'E2E double booking slot')

  const secondBalanceBefore = await getBalance(request, client2.accessToken)
  const firstReservation = await createReservationApi(request, client1.accessToken, slot)
  expect(firstReservation.status).toBe('confirmed')

  const secondAttempt = await request.post(`${API_URL}/reservations`, {
    headers: authHeaders(client2.accessToken),
    data: reservationPayload(slot),
  })
  expect(secondAttempt.status()).not.toBe(201)
  expect(await getBalance(request, client2.accessToken)).toBe(secondBalanceBefore)

  const adminSlots = await getAdminSlots(request, admin.accessToken)
  const updatedSlot = adminSlots.find((candidate) => candidate.id === slot.id)
  expect(updatedSlot?.status).toBe('reserved')
  expect(updatedSlot?.currentBookings).toBe(1)
})

test('client cancellation refunds credits and unlocks capacity', async ({ request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client = await loginApi(request, E2E_USERS.client1)
  const slot = findSlot(await getAdminSlots(request, admin.accessToken), 'E2E seeded week slot', '09:00')
  const balanceBefore = await getBalance(request, client.accessToken)
  const reservation = await createReservationApi(request, client.accessToken, slot)

  const preview = await expectJson<CancellationRefundPreviewDTO>(
    await request.get(`${API_URL}/reservations/${reservation.id}/refund-preview`, {
      headers: authHeaders(client.accessToken),
    })
  )
  expect(preview.refundAmount).toBe(reservation.creditsUsed)

  const result = await expectJson<CancellationResultDTO>(
    await request.delete(`${API_URL}/reservations/${reservation.id}`, {
      headers: authHeaders(client.accessToken),
    })
  )
  expect(result.reservation.status).toBe('cancelled')
  expect(result.refundAmount).toBe(preview.refundAmount)
  expect(await getBalance(request, client.accessToken)).toBe(balanceBefore)

  const transactions = await getTransactions(request, client.accessToken)
  expect(transactions.content.some((transaction) =>
    transaction.referenceId === reservation.id &&
    transaction.type === 'refund' &&
    transaction.amount === reservation.creditsUsed
  )).toBe(true)

  const updatedSlot = (await getAdminSlots(request, admin.accessToken)).find((candidate) => candidate.id === slot.id)
  expect(updatedSlot?.status).toBe('cancelled')
  expect(updatedSlot?.currentBookings).toBe(0)

  const availableAgain = await getAvailableSlots(request, client.accessToken)
  expect(availableAgain.slots.some((candidate) => candidate.slotId === slot.id)).toBe(true)
})

test('admin can create and cancel a client reservation with credit refund', async ({ request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client = await loginApi(request, E2E_USERS.client3)
  const slot = findSlot(await getAdminSlots(request, admin.accessToken), 'E2E admin reservation slot')
  const balanceBefore = await getBalance(request, client.accessToken)

  const reservation = await expectJson<ReservationDTO>(
    await request.post(`${API_URL}/admin/reservations`, {
      headers: authHeaders(admin.accessToken),
      data: {
        userId: client.user.id,
        ...reservationPayload(slot),
        deductCredits: true,
      },
    }),
    201
  )
  expect(reservation.status).toBe('confirmed')
  expect(reservation.slotId).toBe(slot.id)
  expect(reservation.creditsUsed).toBe(slot.pricingItems[0].credits)
  expect(await getBalance(request, client.accessToken)).toBe(balanceBefore - reservation.creditsUsed)

  const cancelled = await expectJson<ReservationDTO>(
    await request.delete(`${API_URL}/admin/reservations/${reservation.id}?refundCredits=true`, {
      headers: authHeaders(admin.accessToken),
    })
  )
  expect(cancelled.status).toBe('cancelled')
  expect(await getBalance(request, client.accessToken)).toBe(balanceBefore)
})

test('new client can register under trainer, activate, log in, see slots, and book', async ({ page, request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const settings = await expectJson<AdminSettingsDTO>(
    await request.get(`${API_URL}/admin/settings`, { headers: authHeaders(admin.accessToken) })
  )
  const inviteCode = settings.inviteCode
  expect(inviteCode).toBeTruthy()

  await expectJson<{ firstName: string | null; lastName: string | null }>(
    await request.get(`${API_URL}/auth/trainer/${inviteCode}`)
  )

  const email = `e2e-client-${Date.now()}@e2e.test`
  await page.goto(`/register/${inviteCode}`)
  await page.getByTestId('register-first-name').fill('E2E')
  await page.getByTestId('register-last-name').fill('Registered')
  await page.getByTestId('register-email').fill(email)
  await page.getByTestId('register-phone').fill('+420123456789')
  await page.getByTestId('register-password').fill(E2E_PASSWORD)
  await page.getByTestId('register-confirm-password').fill(E2E_PASSWORD)
  await page.getByTestId('register-accept-terms').check()

  const registerResponse = page.waitForResponse((response) =>
    response.url().endsWith('/api/auth/register') && response.request().method() === 'POST'
  )
  await page.getByTestId('register-submit').click()
  expect((await registerResponse).status()).toBe(201)

  const inactiveLogin = await request.post(`${API_URL}/auth/login`, {
    data: { email, password: E2E_PASSWORD, rememberMe: false },
  })
  expect(inactiveLogin.status()).not.toBe(200)

  await activateE2eUser(email)
  const activated = await expectJson<AuthResponse>(
    await request.post(`${API_URL}/auth/login`, {
      data: { email, password: E2E_PASSWORD, rememberMe: false },
    })
  )
  expect(activated.user.role).toBe('client')
  expect(activated.user.trainerId).toBe(admin.user.id)

  const available = await getAvailableSlots(request, activated.accessToken)
  expect(available.slots.length).toBeGreaterThan(0)
  const slot = available.slots.find((candidate) => candidate.date === SEEDED_WEEK_START && candidate.start.endsWith('09:00'))
  expect(slot).toBeTruthy()

  const reservation = await expectJson<ReservationDTO>(
    await request.post(`${API_URL}/reservations`, {
      headers: authHeaders(activated.accessToken),
      data: {
        date: slot?.date,
        startTime: slot?.start.split('T')[1],
        endTime: slot?.end.split('T')[1],
        slotId: slot?.slotId,
        pricingItemId: slot?.pricingItems[0]?.id,
      },
    }),
    201
  )
  expect(reservation.status).toBe('confirmed')
  expect(reservation.slotId).toBe(slot?.slotId)
})
