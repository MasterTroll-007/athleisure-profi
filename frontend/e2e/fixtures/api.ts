import { expect, type APIRequestContext, type APIResponse } from '@playwright/test'

export const API_URL = process.env.E2E_API_URL ?? 'http://localhost:8080/api'
export const E2E_PASSWORD = 'Test1234'

export const E2E_USERS = {
  admin: 'admin@test.com',
  client1: 'test1@test.com',
  client2: 'test2@test.com',
  client3: 'test3@test.com',
} as const

export interface UserDTO {
  id: string
  email: string
  firstName: string | null
  lastName: string | null
  role: 'admin' | 'client'
  credits: number
  trainerId: string | null
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: UserDTO
}

export interface PricingItem {
  id: string
  nameCs: string
  nameEn: string | null
  credits: number
}

export interface SlotDTO {
  id: string
  date: string
  startTime: string
  endTime: string
  status: string
  note: string | null
  reservationId: string | null
  pricingItems: PricingItem[]
  capacity: number
  currentBookings: number
  locationId: string | null
}

export interface SlotTemplateDTO {
  id: string
  name: string
  slots: Array<{
    dayOfWeek: number
    startTime: string
    endTime: string
    pricingItemIds: string[]
  }>
}

export interface AvailableSlotDTO {
  slotId: string
  date: string
  start: string
  end: string
  isAvailable: boolean
  pricingItems: PricingItem[]
}

export interface AvailableSlotsResponse {
  slots: AvailableSlotDTO[]
}

export interface ReservationDTO {
  id: string
  userId: string
  slotId: string | null
  date: string
  startTime: string
  endTime: string
  status: string
  creditsUsed: number
  pricingItemId: string | null
}

export interface PageDTO<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  hasNext: boolean
  hasPrevious: boolean
}

export interface CreditBalanceResponse {
  balance: number
  userId: string
}

export interface CreditTransactionDTO {
  id: string
  amount: number
  type: string
  referenceId: string | null
  note: string | null
}

export interface CancellationRefundPreviewDTO {
  reservationId: string
  creditsUsed: number
  refundPercentage: number
  refundAmount: number
}

export interface CancellationResultDTO {
  reservation: ReservationDTO
  refundAmount: number
  refundPercentage: number
}

export interface AdminSettingsDTO {
  inviteCode: string | null
  inviteLink: string | null
  adjacentBookingRequired: boolean
}

export function authHeaders(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` }
}

export async function expectJson<T>(response: APIResponse, expectedStatus = 200): Promise<T> {
  expect(response.status()).toBe(expectedStatus)
  return response.json() as Promise<T>
}

export async function loginApi(request: APIRequestContext, email: string): Promise<AuthResponse> {
  return expectJson<AuthResponse>(
    await request.post(`${API_URL}/auth/login`, {
      data: {
        email,
        password: E2E_PASSWORD,
        rememberMe: false,
      },
    })
  )
}

export function isoDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function addDays(iso: string, days: number): string {
  const date = new Date(`${iso}T12:00:00`)
  date.setDate(date.getDate() + days)
  return isoDate(date)
}

export function mondayAfter(daysFromToday: number): string {
  const date = new Date()
  date.setDate(date.getDate() + daysFromToday)
  const dayOfWeek = date.getDay() === 0 ? 7 : date.getDay()
  date.setDate(date.getDate() - dayOfWeek + 1)
  return isoDate(date)
}

export const SEEDED_WEEK_START = mondayAfter(21)
export const SEEDED_WEEK_END = addDays(SEEDED_WEEK_START, 6)
export const TEMPLATE_WEEK_START = mondayAfter(42)
export const TEMPLATE_WEEK_END = addDays(TEMPLATE_WEEK_START, 6)

export async function getAdminSlots(request: APIRequestContext, token: string, start = SEEDED_WEEK_START, end = SEEDED_WEEK_END): Promise<SlotDTO[]> {
  return expectJson<SlotDTO[]>(
    await request.get(`${API_URL}/admin/slots?start=${start}&end=${end}`, {
      headers: authHeaders(token),
    })
  )
}

export function findSlot(slots: SlotDTO[], note: string, startTime?: string): SlotDTO {
  const slot = slots.find((candidate) =>
    candidate.note === note && (!startTime || candidate.startTime === startTime)
  )
  expect(slot, `Seed slot with note "${note}"`).toBeTruthy()
  return slot as SlotDTO
}

export function reservationPayload(slot: SlotDTO): {
  date: string
  startTime: string
  endTime: string
  slotId: string
  pricingItemId?: string
} {
  return {
    date: slot.date,
    startTime: slot.startTime,
    endTime: slot.endTime,
    slotId: slot.id,
    pricingItemId: slot.pricingItems[0]?.id,
  }
}
