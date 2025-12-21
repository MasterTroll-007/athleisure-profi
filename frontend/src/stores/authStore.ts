import { create } from 'zustand'
import { User, AuthResponse, TokenResponse } from '@/types/api'
import { authApi } from '@/services/api'

interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  isLoading: boolean

  // Actions
  setAuth: (response: AuthResponse) => void
  setTokens: (response: TokenResponse) => void
  logout: () => void
  updateUser: (user: User) => void
  initAuth: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  accessToken: localStorage.getItem('accessToken'),
  refreshToken: localStorage.getItem('refreshToken'),
  isAuthenticated: false,
  isLoading: true,

  setAuth: (response: AuthResponse) => {
    localStorage.setItem('accessToken', response.accessToken)
    localStorage.setItem('refreshToken', response.refreshToken)
    set({
      user: response.user,
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      isAuthenticated: true,
      isLoading: false,
    })
  },

  setTokens: (response: TokenResponse) => {
    localStorage.setItem('accessToken', response.accessToken)
    localStorage.setItem('refreshToken', response.refreshToken)
    set({
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
    })
  },

  logout: () => {
    const refreshToken = get().refreshToken
    if (refreshToken) {
      authApi.logout(refreshToken).catch(() => {})
    }
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
    })
  },

  updateUser: (user: User) => {
    set({ user })
  },

  initAuth: async () => {
    const accessToken = localStorage.getItem('accessToken')

    if (!accessToken) {
      set({ isLoading: false })
      return
    }

    try {
      const user = await authApi.getMe()
      set({
        user,
        isAuthenticated: true,
        isLoading: false,
      })
    } catch {
      // Try to refresh token
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const tokens = await authApi.refresh(refreshToken)
          localStorage.setItem('accessToken', tokens.accessToken)
          localStorage.setItem('refreshToken', tokens.refreshToken)

          const user = await authApi.getMe()
          set({
            user,
            accessToken: tokens.accessToken,
            refreshToken: tokens.refreshToken,
            isAuthenticated: true,
            isLoading: false,
          })
        } catch {
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          set({
            user: null,
            accessToken: null,
            refreshToken: null,
            isAuthenticated: false,
            isLoading: false,
          })
        }
      } else {
        localStorage.removeItem('accessToken')
        set({
          user: null,
          accessToken: null,
          isAuthenticated: false,
          isLoading: false,
        })
      }
    }
  },
}))
