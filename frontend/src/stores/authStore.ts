import { create } from 'zustand'
import { User, AuthResponse, TokenResponse } from '@/types/api'
import { authApi } from '@/services/api'
import { clearAccessToken, setAccessToken } from '@/services/tokenStore'
import { queryClient } from '@/main'

interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  isLoading: boolean

  // Actions
  setAuth: (response: AuthResponse) => void
  setTokens: (response: TokenResponse) => void
  logout: () => Promise<void>
  updateUser: (user: User) => void
  initAuth: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  refreshToken: null,  // Stored in HttpOnly cookie, not accessible to JS
  isAuthenticated: false,
  isLoading: true,

  setAuth: (response: AuthResponse) => {
    setAccessToken(response.accessToken)
    // refreshToken is stored in HttpOnly cookie by backend, not in localStorage
    set({
      user: response.user,
      accessToken: response.accessToken,
      refreshToken: null,  // Not stored client-side for security
      isAuthenticated: true,
      isLoading: false,
    })
  },

  setTokens: (response: TokenResponse) => {
    setAccessToken(response.accessToken)
    // refreshToken is stored in HttpOnly cookie by backend
    set({
      accessToken: response.accessToken,
      refreshToken: null,
    })
  },

  logout: async () => {
    try {
      await authApi.logout()
    } catch {
      // Local cleanup still needs to happen if the server-side logout request fails.
    }
    clearAccessToken()
    queryClient.clear() // Clear all cached data to prevent data leaking between users
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
    try {
      const tokens = await authApi.refresh()
      setAccessToken(tokens.accessToken)
      const user = await authApi.getMe()
      set({
        user,
        accessToken: tokens.accessToken,
        refreshToken: null,
        isAuthenticated: true,
        isLoading: false,
      })
    } catch {
      clearAccessToken()
      set({
        user: null,
        accessToken: null,
        refreshToken: null,
        isAuthenticated: false,
        isLoading: false,
      })
    }
  },
}))
