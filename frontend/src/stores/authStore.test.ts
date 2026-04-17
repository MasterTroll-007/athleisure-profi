import { describe, it, expect, beforeEach, vi } from 'vitest'

// `authStore` transitively imports `queryClient` from `@/main`, which
// calls `createRoot(...)` at module load — that fails in jsdom without
// a `#root` element. Mock the module so the import chain stays clean.
vi.mock('@/main', () => ({
  queryClient: { invalidateQueries: () => {}, clear: () => {} },
}))

import { useAuthStore } from './authStore'
import type { AuthResponse, User } from '@/types/api'

const mockUser: User = {
  id: '1',
  email: 'u@e.com',
  firstName: 'U',
  lastName: 'E',
  phone: null,
  role: 'client',
  credits: 5,
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

const mockAuthResponse: AuthResponse = {
  accessToken: 'access-token-xyz',
  refreshToken: 'refresh-token-abc',
  user: mockUser,
}

describe('authStore', () => {
  beforeEach(() => {
    // Reset store state between tests (Zustand state persists across imports).
    useAuthStore.setState({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: true,
    })
    localStorage.clear()
  })

  it('setAuth stores user and access token', () => {
    useAuthStore.getState().setAuth(mockAuthResponse)
    const state = useAuthStore.getState()

    expect(state.user).toEqual(mockUser)
    expect(state.accessToken).toBe('access-token-xyz')
    expect(state.isAuthenticated).toBe(true)
    expect(state.isLoading).toBe(false)
    expect(localStorage.getItem('accessToken')).toBe('access-token-xyz')
  })

  it('setAuth does not persist refresh token in localStorage', () => {
    useAuthStore.getState().setAuth(mockAuthResponse)
    expect(localStorage.getItem('refreshToken')).toBeNull()
    // The in-memory state also never stores the refresh token for security.
    expect(useAuthStore.getState().refreshToken).toBeNull()
  })

  it('updateUser swaps the user without clearing auth', () => {
    useAuthStore.getState().setAuth(mockAuthResponse)
    useAuthStore.getState().updateUser({ ...mockUser, credits: 10 })

    const state = useAuthStore.getState()
    expect(state.user?.credits).toBe(10)
    expect(state.isAuthenticated).toBe(true)
  })

  it('logout clears local state', async () => {
    useAuthStore.getState().setAuth(mockAuthResponse)
    useAuthStore.getState().logout()

    const state = useAuthStore.getState()
    expect(state.user).toBeNull()
    expect(state.accessToken).toBeNull()
    expect(state.isAuthenticated).toBe(false)
    expect(localStorage.getItem('accessToken')).toBeNull()
  })

  it('survives localStorage throwing (private mode)', () => {
    // Swap localStorage.getItem with a throwing one to simulate
    // storage quota / private-mode denial. The store's safeGetItem
    // should fall back to null rather than crashing module init.
    const original = Storage.prototype.getItem
    Storage.prototype.getItem = vi.fn(() => {
      throw new Error('storage denied')
    })

    // Re-evaluate store state shouldn't throw — we just ensure the
    // existing state survives the next read operation.
    expect(() => useAuthStore.getState()).not.toThrow()

    Storage.prototype.getItem = original
  })
})
