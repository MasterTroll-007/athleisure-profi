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
  type AdminCreditPackageDTO,
  type AdminFeedbackDTO,
  type AdminPricingItemDTO,
  type AuthResponse,
  type AvailableSlotsResponse,
  type CancellationRefundPreviewDTO,
  type CancellationResultDTO,
  type ClientNoteDTO,
  type CreditPackageDTO,
  type CreditBalanceResponse,
  type CreditTransactionDTO,
  type FeedbackSummaryDTO,
  type MeasurementDTO,
  type PageDTO,
  type ReservationDTO,
  type SlotDTO,
  type SlotTemplateDTO,
  type TrainingFeedbackDTO,
  type TrainingLocationDTO,
  type UserDTO,
  type WorkoutLogDTO,
} from '../fixtures/api'
import { activateE2eUser, queryE2eSql, resetE2eData } from '../fixtures/db'

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

async function getSeedCatalog(
  request: APIRequestContext,
  adminToken: string
): Promise<{ location: TrainingLocationDTO; pricing: AdminPricingItemDTO }> {
  const locations = await expectJson<TrainingLocationDTO[]>(
    await request.get(`${API_URL}/admin/locations`, { headers: authHeaders(adminToken) })
  )
  const pricing = await expectJson<AdminPricingItemDTO[]>(
    await request.get(`${API_URL}/admin/pricing`, { headers: authHeaders(adminToken) })
  )
  const location = locations.find((candidate) => candidate.nameCs === 'E2E Gym')
  const pricingItem = pricing.find((candidate) => candidate.nameCs === 'E2E Jeden trenink')
  expect(location, 'Seed location').toBeTruthy()
  expect(pricingItem, 'Seed pricing item').toBeTruthy()
  return {
    location: location as TrainingLocationDTO,
    pricing: pricingItem as AdminPricingItemDTO,
  }
}

async function createUnlockedSlot(
  request: APIRequestContext,
  adminToken: string,
  data: {
    date: string
    startTime: string
    note: string
    pricingItemIds: string[]
    locationId: string
    durationMinutes?: number
    capacity?: number
  }
): Promise<SlotDTO> {
  const created = await expectJson<SlotDTO>(
    await request.post(`${API_URL}/admin/slots`, {
      headers: authHeaders(adminToken),
      data: {
        durationMinutes: 60,
        capacity: 1,
        ...data,
      },
    }),
    201
  )
  return expectJson<SlotDTO>(
    await request.patch(`${API_URL}/admin/slots/${created.id}`, {
      headers: authHeaders(adminToken),
      data: { status: 'unlocked' },
    })
  )
}

async function createPastCompletedReservation(
  email = E2E_USERS.client1
): Promise<string> {
  const safeEmail = email.replace(/'/g, "''")
  return queryE2eSql(`
    WITH admin_user AS (
      SELECT id FROM users WHERE email = 'admin@test.com'
    ),
    client_user AS (
      SELECT id FROM users WHERE email = '${safeEmail}'
    ),
    seed_location AS (
      SELECT id
      FROM training_locations
      WHERE name_cs = 'E2E Gym' AND admin_id = (SELECT id FROM admin_user)
      LIMIT 1
    ),
    seed_pricing AS (
      SELECT id
      FROM pricing_items
      WHERE name_cs = 'E2E Jeden trenink' AND admin_id = (SELECT id FROM admin_user)
      LIMIT 1
    ),
    created_slot AS (
      INSERT INTO slots (
        id, date, start_time, end_time, duration_minutes, status,
        admin_id, location_id, capacity, note, created_at
      )
      SELECT gen_random_uuid(), CURRENT_DATE - INTERVAL '2 days', TIME '08:00', TIME '09:00',
             60, 'RESERVED', admin_user.id, seed_location.id, 1, 'E2E feedback past slot', NOW()
      FROM admin_user, seed_location
      RETURNING id, date, start_time, end_time
    ),
    linked_pricing AS (
      INSERT INTO slot_pricing_items (id, slot_id, pricing_item_id)
      SELECT gen_random_uuid(), created_slot.id, seed_pricing.id
      FROM created_slot, seed_pricing
      RETURNING slot_id
    ),
    created_reservation AS (
      INSERT INTO reservations (
        id, user_id, slot_id, date, start_time, end_time, status,
        credits_used, pricing_item_id, created_at, completed_at
      )
      SELECT gen_random_uuid(), client_user.id, created_slot.id, created_slot.date,
             created_slot.start_time, created_slot.end_time, 'completed',
             1, seed_pricing.id, NOW(), NOW()
      FROM client_user, created_slot, seed_pricing, linked_pricing
      RETURNING id
    )
    SELECT id FROM created_reservation;
  `)
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

test('admin manages clients with pagination, notes, credits, and blocking', async ({ request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client = await loginApi(request, E2E_USERS.client1)

  const clients = await expectJson<PageDTO<UserDTO>>(
    await request.get(`${API_URL}/admin/clients?page=0&size=2`, { headers: authHeaders(admin.accessToken) })
  )
  expect(clients.content.length).toBeLessThanOrEqual(2)
  expect(clients.totalElements).toBeGreaterThanOrEqual(3)
  expect(clients.size).toBe(2)

  const search = await expectJson<UserDTO[]>(
    await request.get(`${API_URL}/admin/clients/search?q=Client%20One`, { headers: authHeaders(admin.accessToken) })
  )
  const clientOne = search.find((candidate) => candidate.email === E2E_USERS.client1)
  expect(clientOne).toBeTruthy()

  const tooLongQuery = await request.get(`${API_URL}/admin/clients/search?q=${'x'.repeat(101)}`, {
    headers: authHeaders(admin.accessToken),
  })
  expect(tooLongQuery.status()).toBe(400)

  const note = await expectJson<ClientNoteDTO>(
    await request.post(`${API_URL}/admin/clients/${client.user.id}/notes`, {
      headers: authHeaders(admin.accessToken),
      data: { content: 'E2E progress note' },
    }),
    201
  )
  expect(note.content).toBe('E2E progress note')

  const notes = await expectJson<PageDTO<ClientNoteDTO>>(
    await request.get(`${API_URL}/admin/clients/${client.user.id}/notes?size=5`, {
      headers: authHeaders(admin.accessToken),
    })
  )
  expect(notes.content.some((candidate) => candidate.id === note.id)).toBe(true)

  await expectJson<{ message: string }>(
    await request.delete(`${API_URL}/admin/notes/${note.id}`, { headers: authHeaders(admin.accessToken) })
  )
  const notesAfterDelete = await expectJson<PageDTO<ClientNoteDTO>>(
    await request.get(`${API_URL}/admin/clients/${client.user.id}/notes?size=5`, {
      headers: authHeaders(admin.accessToken),
    })
  )
  expect(notesAfterDelete.content.some((candidate) => candidate.id === note.id)).toBe(false)

  const balanceBefore = await getBalance(request, client.accessToken)
  const adjusted = await expectJson<CreditBalanceResponse>(
    await request.post(`${API_URL}/admin/clients/${client.user.id}/adjust-credits`, {
      headers: authHeaders(admin.accessToken),
      data: { amount: 3, reason: 'E2E adjustment' },
    })
  )
  expect(adjusted.balance).toBe(balanceBefore + 3)

  const adminTransactions = await expectJson<PageDTO<CreditTransactionDTO>>(
    await request.get(`${API_URL}/admin/clients/${client.user.id}/transactions?size=5`, {
      headers: authHeaders(admin.accessToken),
    })
  )
  expect(adminTransactions.content.some((transaction) =>
    transaction.amount === 3 &&
    transaction.type === 'admin_adjustment' &&
    transaction.note === 'E2E adjustment'
  )).toBe(true)

  const blocked = await expectJson<UserDTO>(
    await request.post(`${API_URL}/admin/clients/${client.user.id}/block`, {
      headers: authHeaders(admin.accessToken),
    })
  )
  expect(blocked.isBlocked).toBe(true)

  const blockedLogin = await request.post(`${API_URL}/auth/login`, {
    data: { email: E2E_USERS.client1, password: E2E_PASSWORD, rememberMe: false },
  })
  expect(blockedLogin.status()).not.toBe(200)

  const slot = findSlot(await getAdminSlots(request, admin.accessToken), 'E2E seeded week slot', '09:00')
  const blockedBooking = await request.post(`${API_URL}/reservations`, {
    headers: authHeaders(client.accessToken),
    data: reservationPayload(slot),
  })
  expect(blockedBooking.status()).not.toBe(201)

  const unblocked = await expectJson<UserDTO>(
    await request.post(`${API_URL}/admin/clients/${client.user.id}/unblock`, {
      headers: authHeaders(admin.accessToken),
    })
  )
  expect(unblocked.isBlocked).toBe(false)
  await loginApi(request, E2E_USERS.client1)
})

test('admin catalog CRUD controls client-visible locations, pricing, and packages', async ({ request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client = await loginApi(request, E2E_USERS.client1)

  const inactiveLocation = await expectJson<TrainingLocationDTO>(
    await request.post(`${API_URL}/admin/locations`, {
      headers: authHeaders(admin.accessToken),
      data: {
        nameCs: 'E2E Hidden Studio',
        nameEn: 'E2E Hidden Studio',
        addressCs: 'Hidden address',
        addressEn: 'Hidden address',
        color: '#14B8A6',
        isActive: false,
      },
    }),
    201
  )
  const hiddenClientLocations = await expectJson<TrainingLocationDTO[]>(
    await request.get(`${API_URL}/locations`, { headers: authHeaders(client.accessToken) })
  )
  expect(hiddenClientLocations.some((location) => location.id === inactiveLocation.id)).toBe(false)

  const activeLocation = await expectJson<TrainingLocationDTO>(
    await request.patch(`${API_URL}/admin/locations/${inactiveLocation.id}`, {
      headers: authHeaders(admin.accessToken),
      data: { isActive: true, nameCs: 'E2E Visible Studio', color: '#22C55E' },
    })
  )
  expect(activeLocation.isActive).toBe(true)
  const visibleClientLocations = await expectJson<TrainingLocationDTO[]>(
    await request.get(`${API_URL}/locations`, { headers: authHeaders(client.accessToken) })
  )
  expect(visibleClientLocations.some((location) =>
    location.id === activeLocation.id &&
    location.nameCs === 'E2E Visible Studio' &&
    location.color === '#22C55E'
  )).toBe(true)

  const inactivePricing = await expectJson<AdminPricingItemDTO>(
    await request.post(`${API_URL}/admin/pricing`, {
      headers: authHeaders(admin.accessToken),
      data: {
        nameCs: 'E2E Hidden Pricing',
        nameEn: 'E2E Hidden Pricing',
        descriptionCs: 'Hidden',
        descriptionEn: 'Hidden',
        credits: 4,
        durationMinutes: 75,
        isActive: false,
        sortOrder: 50,
      },
    }),
    201
  )
  const hiddenClientPricing = await expectJson<AdminPricingItemDTO[]>(
    await request.get(`${API_URL}/credits/pricing`, { headers: authHeaders(client.accessToken) })
  )
  expect(hiddenClientPricing.some((pricing) => pricing.id === inactivePricing.id)).toBe(false)

  const activePricing = await expectJson<AdminPricingItemDTO>(
    await request.patch(`${API_URL}/admin/pricing/${inactivePricing.id}`, {
      headers: authHeaders(admin.accessToken),
      data: { isActive: true, credits: 5, nameCs: 'E2E Visible Pricing' },
    })
  )
  expect(activePricing.credits).toBe(5)
  const visibleClientPricing = await expectJson<AdminPricingItemDTO[]>(
    await request.get(`${API_URL}/credits/pricing`, { headers: authHeaders(client.accessToken) })
  )
  expect(visibleClientPricing.some((pricing) =>
    pricing.id === activePricing.id &&
    pricing.nameCs === 'E2E Visible Pricing' &&
    pricing.credits === 5
  )).toBe(true)

  const inactivePackage = await expectJson<AdminCreditPackageDTO>(
    await request.post(`${API_URL}/admin/packages`, {
      headers: authHeaders(admin.accessToken),
      data: {
        nameCs: 'E2E Hidden Package',
        nameEn: 'E2E Hidden Package',
        description: 'Hidden package',
        credits: 6,
        priceCzk: 600,
        currency: 'CZK',
        isActive: false,
        sortOrder: 50,
        highlightType: 'NONE',
        isBasic: false,
      },
    }),
    201
  )
  const hiddenClientPackages = await expectJson<CreditPackageDTO[]>(
    await request.get(`${API_URL}/credits/packages`, { headers: authHeaders(client.accessToken) })
  )
  expect(hiddenClientPackages.some((pkg) => pkg.id === inactivePackage.id)).toBe(false)

  const activePackage = await expectJson<AdminCreditPackageDTO>(
    await request.patch(`${API_URL}/admin/packages/${inactivePackage.id}`, {
      headers: authHeaders(admin.accessToken),
      data: { isActive: true, nameCs: 'E2E Visible Package', highlightType: 'BEST_VALUE' },
    })
  )
  expect(activePackage.isActive).toBe(true)
  const visibleClientPackages = await expectJson<CreditPackageDTO[]>(
    await request.get(`${API_URL}/credits/packages`, { headers: authHeaders(client.accessToken) })
  )
  expect(visibleClientPackages.some((pkg) =>
    pkg.id === activePackage.id &&
    pkg.name === 'E2E Visible Package' &&
    pkg.highlightType === 'BEST_VALUE'
  )).toBe(true)

  await expectJson<{ message: string }>(
    await request.delete(`${API_URL}/admin/packages/${activePackage.id}`, { headers: authHeaders(admin.accessToken) })
  )
  await expectJson<{ message: string }>(
    await request.delete(`${API_URL}/admin/pricing/${activePricing.id}`, { headers: authHeaders(admin.accessToken) })
  )
  await expectJson<{ message: string }>(
    await request.delete(`${API_URL}/admin/locations/${activeLocation.id}`, { headers: authHeaders(admin.accessToken) })
  )
})

test('adjacent booking setting is per trainer and off allows multiple same-day non-adjacent bookings', async ({ request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client = await loginApi(request, E2E_USERS.client1)
  const catalog = await getSeedCatalog(request, admin.accessToken)

  const strictSettings = await expectJson<AdminSettingsDTO>(
    await request.patch(`${API_URL}/admin/settings`, {
      headers: authHeaders(admin.accessToken),
      data: { adjacentBookingRequired: true },
    })
  )
  expect(strictSettings.adjacentBookingRequired).toBe(true)

  const nonAdjacentSlot = await createUnlockedSlot(request, admin.accessToken, {
    date: SEEDED_WEEK_START,
    startTime: '14:00',
    note: 'E2E non adjacent setting slot',
    pricingItemIds: [catalog.pricing.id],
    locationId: catalog.location.id,
  })
  const firstSlot = findSlot(await getAdminSlots(request, admin.accessToken), 'E2E seeded week slot', '09:00')
  await createReservationApi(request, client.accessToken, firstSlot)

  const strictAvailable = await getAvailableSlots(request, client.accessToken)
  expect(strictAvailable.slots.some((slot) => slot.slotId === nonAdjacentSlot.id)).toBe(false)

  const strictAttempt = await request.post(`${API_URL}/reservations`, {
    headers: authHeaders(client.accessToken),
    data: reservationPayload(nonAdjacentSlot),
  })
  expect(strictAttempt.status()).not.toBe(201)

  const relaxedSettings = await expectJson<AdminSettingsDTO>(
    await request.patch(`${API_URL}/admin/settings`, {
      headers: authHeaders(admin.accessToken),
      data: { adjacentBookingRequired: false },
    })
  )
  expect(relaxedSettings.adjacentBookingRequired).toBe(false)

  const relaxedAvailable = await getAvailableSlots(request, client.accessToken)
  expect(relaxedAvailable.slots.some((slot) =>
    slot.slotId === nonAdjacentSlot.id &&
    slot.isAvailable
  )).toBe(true)

  const secondReservation = await createReservationApi(request, client.accessToken, nonAdjacentSlot)
  expect(secondReservation.status).toBe('confirmed')
  expect(secondReservation.slotId).toBe(nonAdjacentSlot.id)
})

test('pagination is enforced on reservation and transaction lists', async ({ request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client = await loginApi(request, E2E_USERS.client1)
  const slots = await getAdminSlots(request, admin.accessToken)
  const bookableSlots = [
    findSlot(slots, 'E2E seeded week slot', '09:00'),
    findSlot(slots, 'E2E double booking slot'),
    slots.find((slot) => slot.note === 'E2E seeded week slot' && slot.date === addDays(SEEDED_WEEK_START, 2)),
  ]
  expect(bookableSlots.every(Boolean)).toBe(true)

  for (const slot of bookableSlots) {
    await createReservationApi(request, client.accessToken, slot as SlotDTO)
  }

  const reservations = await expectJson<PageDTO<ReservationDTO>>(
    await request.get(`${API_URL}/reservations?scope=all&page=0&size=2`, {
      headers: authHeaders(client.accessToken),
    })
  )
  expect(reservations.content).toHaveLength(2)
  expect(reservations.totalElements).toBeGreaterThanOrEqual(3)
  expect(reservations.hasNext).toBe(true)

  const adminReservations = await expectJson<PageDTO<ReservationDTO>>(
    await request.get(`${API_URL}/admin/clients/${client.user.id}/reservations?page=0&size=2`, {
      headers: authHeaders(admin.accessToken),
    })
  )
  expect(adminReservations.content).toHaveLength(2)
  expect(adminReservations.totalElements).toBeGreaterThanOrEqual(3)
  expect(adminReservations.hasNext).toBe(true)

  const transactions = await expectJson<PageDTO<CreditTransactionDTO>>(
    await request.get(`${API_URL}/credits/transactions?page=0&size=2`, {
      headers: authHeaders(client.accessToken),
    })
  )
  expect(transactions.content).toHaveLength(2)
  expect(transactions.totalElements).toBeGreaterThanOrEqual(3)
  expect(transactions.hasNext).toBe(true)

  const adminTransactions = await expectJson<PageDTO<CreditTransactionDTO>>(
    await request.get(`${API_URL}/admin/clients/${client.user.id}/transactions?page=0&size=2`, {
      headers: authHeaders(admin.accessToken),
    })
  )
  expect(adminTransactions.content).toHaveLength(2)
  expect(adminTransactions.totalElements).toBeGreaterThanOrEqual(3)
  expect(adminTransactions.hasNext).toBe(true)
})

test('workout logs and measurements are visible only through the right trainer and client flows', async ({ request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client = await loginApi(request, E2E_USERS.client3)
  const otherClient = await loginApi(request, E2E_USERS.client2)
  const slot = findSlot(await getAdminSlots(request, admin.accessToken), 'E2E admin reservation slot')

  const reservation = await expectJson<ReservationDTO>(
    await request.post(`${API_URL}/admin/reservations`, {
      headers: authHeaders(admin.accessToken),
      data: {
        userId: client.user.id,
        ...reservationPayload(slot),
        deductCredits: false,
      },
    }),
    201
  )

  const workout = await expectJson<WorkoutLogDTO>(
    await request.post(`${API_URL}/admin/reservations/${reservation.id}/workout`, {
      headers: authHeaders(admin.accessToken),
      data: {
        exercises: [
          { name: 'Bench press', sets: 3, reps: 8, weight: 80, notes: 'E2E working sets' },
          { name: 'Plank', duration: '60s', notes: 'E2E core finisher' },
        ],
        notes: 'E2E workout notes',
      },
    }),
    201
  )
  expect(workout.reservationId).toBe(reservation.id)
  expect(workout.exercises[0].sets).toBe(3)

  const adminWorkout = await expectJson<WorkoutLogDTO>(
    await request.get(`${API_URL}/admin/reservations/${reservation.id}/workout`, {
      headers: authHeaders(admin.accessToken),
    })
  )
  expect(adminWorkout.notes).toBe('E2E workout notes')

  const clientWorkout = await expectJson<WorkoutLogDTO>(
    await request.get(`${API_URL}/reservations/${reservation.id}/workout`, {
      headers: authHeaders(client.accessToken),
    })
  )
  expect(clientWorkout.exercises.map((exercise) => exercise.name)).toEqual(['Bench press', 'Plank'])

  const otherClientWorkout = await request.get(`${API_URL}/reservations/${reservation.id}/workout`, {
    headers: authHeaders(otherClient.accessToken),
  })
  expect(otherClientWorkout.status()).toBe(403)

  const myWorkouts = await expectJson<PageDTO<WorkoutLogDTO>>(
    await request.get(`${API_URL}/reservations/workouts/my?size=5`, { headers: authHeaders(client.accessToken) })
  )
  expect(myWorkouts.content.some((candidate) => candidate.reservationId === reservation.id)).toBe(true)

  const adminClientWorkouts = await expectJson<PageDTO<WorkoutLogDTO>>(
    await request.get(`${API_URL}/admin/clients/${client.user.id}/workouts?size=5`, {
      headers: authHeaders(admin.accessToken),
    })
  )
  expect(adminClientWorkouts.content.some((candidate) => candidate.reservationId === reservation.id)).toBe(true)

  const measurement = await expectJson<MeasurementDTO>(
    await request.post(`${API_URL}/admin/clients/${client.user.id}/measurements`, {
      headers: authHeaders(admin.accessToken),
      data: {
        date: SEEDED_WEEK_START,
        weight: 82.4,
        bodyFat: 17.5,
        waist: 84,
        notes: 'E2E body measurement',
      },
    }),
    201
  )
  expect(measurement.userId).toBe(client.user.id)
  expect(measurement.weight).toBe(82.4)

  const adminMeasurements = await expectJson<PageDTO<MeasurementDTO>>(
    await request.get(`${API_URL}/admin/clients/${client.user.id}/measurements?size=5`, {
      headers: authHeaders(admin.accessToken),
    })
  )
  expect(adminMeasurements.content.some((candidate) => candidate.id === measurement.id)).toBe(true)

  const clientMeasurements = await expectJson<PageDTO<MeasurementDTO>>(
    await request.get(`${API_URL}/auth/me/measurements?size=5`, { headers: authHeaders(client.accessToken) })
  )
  expect(clientMeasurements.content.some((candidate) => candidate.id === measurement.id)).toBe(true)
})

test('feedback is accepted only after completed sessions and appears in trainer summaries', async ({ request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client = await loginApi(request, E2E_USERS.client1)
  const futureSlot = findSlot(await getAdminSlots(request, admin.accessToken), 'E2E seeded week slot', '09:00')
  const futureReservation = await createReservationApi(request, client.accessToken, futureSlot)

  const futureFeedback = await request.post(`${API_URL}/feedback`, {
    headers: authHeaders(client.accessToken),
    data: { reservationId: futureReservation.id, rating: 5, comment: 'Too early' },
  })
  expect(futureFeedback.status()).not.toBe(201)

  const pastReservationId = await createPastCompletedReservation()
  const feedback = await expectJson<TrainingFeedbackDTO>(
    await request.post(`${API_URL}/feedback`, {
      headers: authHeaders(client.accessToken),
      data: { reservationId: pastReservationId, rating: 5, comment: 'E2E great training' },
    }),
    201
  )
  expect(feedback.reservationId).toBe(pastReservationId)

  const duplicateFeedback = await request.post(`${API_URL}/feedback`, {
    headers: authHeaders(client.accessToken),
    data: { reservationId: pastReservationId, rating: 4, comment: 'Duplicate' },
  })
  expect(duplicateFeedback.status()).not.toBe(201)

  const feedbackForReservation = await expectJson<TrainingFeedbackDTO>(
    await request.get(`${API_URL}/feedback/reservation/${pastReservationId}`, {
      headers: authHeaders(client.accessToken),
    })
  )
  expect(feedbackForReservation.id).toBe(feedback.id)

  const myFeedback = await expectJson<PageDTO<TrainingFeedbackDTO>>(
    await request.get(`${API_URL}/feedback/my?size=5`, { headers: authHeaders(client.accessToken) })
  )
  expect(myFeedback.content.some((candidate) => candidate.id === feedback.id)).toBe(true)

  const summary = await expectJson<FeedbackSummaryDTO>(
    await request.get(`${API_URL}/admin/feedback/summary`, { headers: authHeaders(admin.accessToken) })
  )
  expect(summary.totalCount).toBeGreaterThanOrEqual(1)
  expect(summary.averageRating).toBeGreaterThanOrEqual(5)
  expect(summary.distribution['5']).toBeGreaterThanOrEqual(1)

  const adminFeedback = await expectJson<PageDTO<AdminFeedbackDTO>>(
    await request.get(`${API_URL}/admin/feedback?size=5`, { headers: authHeaders(admin.accessToken) })
  )
  expect(adminFeedback.content.some((candidate) =>
    candidate.id === feedback.id &&
    candidate.comment === 'E2E great training'
  )).toBe(true)
})

test('client calendar and admin CSV exports include current booking data', async ({ request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client = await loginApi(request, E2E_USERS.client1)
  const slot = findSlot(await getAdminSlots(request, admin.accessToken), 'E2E seeded week slot', '09:00')
  const reservation = await createReservationApi(request, client.accessToken, slot)

  const icalResponse = await request.get(`${API_URL}/reservations/ical`, {
    headers: authHeaders(client.accessToken),
  })
  expect(icalResponse.status()).toBe(200)
  const ical = await icalResponse.text()
  expect(ical).toContain('BEGIN:VCALENDAR')
  expect(ical).toContain(`${reservation.id}@rezervace-pankova.online`)

  const clientsCsvResponse = await request.get(`${API_URL}/admin/export/clients`, {
    headers: authHeaders(admin.accessToken),
  })
  expect(clientsCsvResponse.status()).toBe(200)
  const clientsCsv = await clientsCsvResponse.text()
  expect(clientsCsv).toContain('id,email,first_name,last_name,phone,credits,created_at')
  expect(clientsCsv).toContain(E2E_USERS.client1)

  const reservationsCsvResponse = await request.get(
    `${API_URL}/admin/export/reservations?start=${SEEDED_WEEK_START}&end=${SEEDED_WEEK_END}`,
    { headers: authHeaders(admin.accessToken) }
  )
  expect(reservationsCsvResponse.status()).toBe(200)
  const reservationsCsv = await reservationsCsvResponse.text()
  expect(reservationsCsv).toContain('id,client,email,date,start,end,status,credits,training_type')
  expect(reservationsCsv).toContain(reservation.id)
  expect(reservationsCsv).toContain(E2E_USERS.client1)

  const paymentsCsvResponse = await request.get(`${API_URL}/admin/export/payments`, {
    headers: authHeaders(admin.accessToken),
  })
  expect(paymentsCsvResponse.status()).toBe(200)
  expect(await paymentsCsvResponse.text()).toContain('id,client,amount,currency,state,package,created_at,stripe_session_id')
})

test('profile preferences persist and GDPR export contains the updated user data', async ({ request }) => {
  const client = await loginApi(request, E2E_USERS.client2)

  const updated = await expectJson<UserDTO>(
    await request.patch(`${API_URL}/auth/me`, {
      headers: authHeaders(client.accessToken),
      data: {
        firstName: 'E2E',
        lastName: 'Client Two Updated',
        phone: '+420111222333',
        locale: 'en',
        theme: 'light',
        emailRemindersEnabled: false,
        reminderHoursBefore: 6,
      },
    })
  )
  expect(updated.lastName).toBe('Client Two Updated')
  expect(updated.locale).toBe('en')
  expect(updated.theme).toBe('light')
  expect(updated.emailRemindersEnabled).toBe(false)
  expect(updated.reminderHoursBefore).toBe(6)

  const me = await expectJson<UserDTO>(
    await request.get(`${API_URL}/auth/me`, { headers: authHeaders(client.accessToken) })
  )
  expect(me.phone).toBe('+420111222333')
  expect(me.locale).toBe('en')

  const exportResponse = await request.get(`${API_URL}/auth/me/export`, {
    headers: authHeaders(client.accessToken),
  })
  expect(exportResponse.status()).toBe(200)
  const exported = JSON.parse(await exportResponse.text()) as {
    profile: { email: string; firstName: string; lastName: string; locale: string }
    reservations: unknown[]
    creditTransactions: unknown[]
  }
  expect(exported.profile.email).toBe(E2E_USERS.client2)
  expect(exported.profile.lastName).toBe('Client Two Updated')
  expect(exported.profile.locale).toBe('en')
  expect(Array.isArray(exported.reservations)).toBe(true)
  expect(Array.isArray(exported.creditTransactions)).toBe(true)
})
