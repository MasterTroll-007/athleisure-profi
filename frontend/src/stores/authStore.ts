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
  refreshToken: null,  // Stored in HttpOnly cookie, not accessible to JS
  isAuthenticated: false,
  isLoading: true,

  setAuth: (response: AuthResponse) => {
    localStorage.setItem('accessToken', response.accessToken)
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
    localStorage.setItem('accessToken', response.accessToken)
    // refreshToken is stored in HttpOnly cookie by backend
    set({
      accessToken: response.accessToken,
      refreshToken: null,
    })
  },

  logout: () => {
    authApi.logout().catch(() => {})
    localStorage.removeItem('accessToken')
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
      // Token might be expired - the API interceptor will automatically
      // try to refresh using the HttpOnly cookie
      localStorage.removeItem('accessToken')
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
