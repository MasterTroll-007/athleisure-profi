import { http, HttpResponse } from 'msw'
import type { User, AuthResponse } from '@/types/api'

// Reusable fake entities — tests can import or extend these, or override
// per-test with `server.use(...)`.
export const fakeAdminUser: User = {
  id: '00000000-0000-0000-0000-000000000001',
  email: 'admin@test.com',
  firstName: 'Test',
  lastName: 'Admin',
  phone: null,
  role: 'admin',
  credits: 10,
  locale: 'cs',
  theme: 'system',
  trainerId: null,
  trainerName: null,
  calendarStartHour: 6,
  calendarEndHour: 22,
  emailRemindersEnabled: true,
  reminderHoursBefore: 24,
  createdAt: '2026-01-01T00:00:00Z',
}

export const fakeClientUser: User = {
  ...fakeAdminUser,
  id: '00000000-0000-0000-0000-000000000002',
  email: 'client@test.com',
  firstName: 'Test',
  lastName: 'Client',
  role: 'client',
  trainerId: '00000000-0000-0000-0000-000000000001',
  trainerName: 'Test Admin',
}

export const fakeAuthResponse = (user: User): AuthResponse => ({
  accessToken: 'fake-access-token',
  refreshToken: 'fake-refresh-token',
  user,
})

// Default handlers — API endpoints most tests don't care about. Tests that
// need something specific call `server.use(http.post('/api/auth/login', ...))`.
export const handlers = [
  http.post('/api/auth/login', async () => HttpResponse.json(fakeAuthResponse(fakeClientUser))),

  http.get('/api/auth/me', () => HttpResponse.json(fakeClientUser)),

  http.post('/api/auth/logout', () => HttpResponse.json({ message: 'ok' })),

  http.get('/api/health', () => HttpResponse.json({ status: 'ok', version: 'test' })),

  http.get('/api/locations', () => HttpResponse.json([])),

  http.get('/api/admin/locations', () => HttpResponse.json([])),

  http.get('/api/reservations/my', () => HttpResponse.json([])),

  http.get('/api/reservations/available', () =>
    HttpResponse.json({ slots: [] })
  ),

  http.get('/api/availability/calendar-settings', () =>
    HttpResponse.json({
      calendarStartHour: 6,
      calendarEndHour: 22,
      inviteCode: null,
      inviteLink: null,
      adjacentBookingRequired: true,
    })
  ),

  http.get('/api/admin/slots', () => HttpResponse.json([])),
  http.get('/api/admin/templates', () => HttpResponse.json([])),
  http.get('/api/admin/pricing', () => HttpResponse.json([])),
]
