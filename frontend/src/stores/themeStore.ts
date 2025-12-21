import { create } from 'zustand'

type Theme = 'light' | 'dark' | 'system'

interface ThemeState {
  theme: Theme
  resolvedTheme: 'light' | 'dark'
  setTheme: (theme: Theme) => void
  initTheme: () => void
}

function getSystemTheme(): 'light' | 'dark' {
  if (typeof window === 'undefined') return 'light'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function applyTheme(resolvedTheme: 'light' | 'dark') {
  if (typeof document === 'undefined') return

  if (resolvedTheme === 'dark') {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
}

export const useThemeStore = create<ThemeState>((set, get) => ({
  theme: (localStorage.getItem('theme') as Theme) || 'system',
  resolvedTheme: 'light',

  setTheme: (theme: Theme) => {
    localStorage.setItem('theme', theme)
    const resolvedTheme = theme === 'system' ? getSystemTheme() : theme
    applyTheme(resolvedTheme)
    set({ theme, resolvedTheme })
  },

  initTheme: () => {
    const savedTheme = (localStorage.getItem('theme') as Theme) || 'system'
    const resolvedTheme = savedTheme === 'system' ? getSystemTheme() : savedTheme
    applyTheme(resolvedTheme)
    set({ theme: savedTheme, resolvedTheme })

    // Listen for system theme changes
    if (typeof window !== 'undefined') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      mediaQuery.addEventListener('change', (e) => {
        const { theme } = get()
        if (theme === 'system') {
          const newResolvedTheme = e.matches ? 'dark' : 'light'
          applyTheme(newResolvedTheme)
          set({ resolvedTheme: newResolvedTheme })
        }
      })
    }
  },
}))
