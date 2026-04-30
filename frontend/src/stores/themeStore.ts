import { create } from 'zustand'

type Theme = 'dark'

interface ThemeState {
  theme: Theme
  resolvedTheme: Theme
  setTheme: (theme?: Theme) => void
  initTheme: () => void
}

function applyDarkTheme() {
  if (typeof document === 'undefined') return
  document.documentElement.classList.add('dark')
}

function clearStoredThemePreference() {
  if (typeof localStorage === 'undefined') return
  localStorage.removeItem('theme')
}

export const useThemeStore = create<ThemeState>((set) => ({
  theme: 'dark',
  resolvedTheme: 'dark',

  setTheme: () => {
    clearStoredThemePreference()
    applyDarkTheme()
    set({ theme: 'dark', resolvedTheme: 'dark' })
  },

  initTheme: () => {
    clearStoredThemePreference()
    applyDarkTheme()
    set({ theme: 'dark', resolvedTheme: 'dark' })
  },
}))
