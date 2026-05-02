import { describe, it, expect, beforeEach, vi } from 'vitest'

// `authStore` transitively imports `queryClient` from `@/main`, which
// calls `createRoot(...)` at module load — that fails in jsdom without
// a `#root` element. Mock the module so the import chain stays clean.
vi.mock('@/main', () => ({
  queryClient: { invalidateQueries: () => {}, clear: () => {} },
}))

const { logoutMock } = vi.hoisted(() => ({
  logoutMock: vi.fn(),
}))

vi.mock('@/services/api', () => ({
  authApi: {
    logout: logoutMock,
  },
}))

import { useAuthStore } from './authStore'
import { clearAccessToken, getAccessToken } from '@/services/tokenStore'
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
    clearAccessToken()
    localStorage.clear()
    logoutMock.mockReset()
    logoutMock.mockResolvedValue(undefined)
  })

  it('setAuth stores user and access token in memory only', () => {
    useAuthStore.getState().setAuth(mockAuthResponse)
    const state = useAuthStore.getState()

    expect(state.user).toEqual(mockUser)
    expect(state.accessToken).toBe('access-token-xyz')
    expect(state.isAuthenticated).toBe(true)
    expect(state.isLoading).toBe(false)
    expect(getAccessToken()).toBe('access-token-xyz')
    expect(localStorage.getItem('accessToken')).toBeNull()
  })

  it('setAuth does not persist tokens in localStorage', () => {
    useAuthStore.getState().setAuth(mockAuthResponse)
    expect(localStorage.getItem('accessToken')).toBeNull()
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
    await useAuthStore.getState().logout()

    const state = useAuthStore.getState()
    expect(logoutMock).toHaveBeenCalledTimes(1)
    expect(state.user).toBeNull()
    expect(state.accessToken).toBeNull()
    expect(state.isAuthenticated).toBe(false)
    expect(getAccessToken()).toBeNull()
    expect(localStorage.getItem('accessToken')).toBeNull()
  })

  it('logout still clears local state when server logout fails', async () => {
    logoutMock.mockRejectedValueOnce(new Error('network down'))
    useAuthStore.getState().setAuth(mockAuthResponse)

    await useAuthStore.getState().logout()

    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(getAccessToken()).toBeNull()
  })

  it('setAuth does not depend on localStorage availability', () => {
    const original = Storage.prototype.setItem
    Storage.prototype.setItem = vi.fn(() => {
      throw new Error('storage denied')
    })

    expect(() => useAuthStore.getState().setAuth(mockAuthResponse)).not.toThrow()
    expect(getAccessToken()).toBe('access-token-xyz')

    Storage.prototype.setItem = original
  })
})
