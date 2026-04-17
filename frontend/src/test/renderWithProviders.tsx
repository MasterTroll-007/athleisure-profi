import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, RenderOptions } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ReactElement, ReactNode } from 'react'
import i18n from '@/i18n'
import { I18nextProvider } from 'react-i18next'

/**
 * Single entrypoint for rendering components under test with the providers
 * they typically depend on (React Query, Router, i18n). Keeps individual
 * test files terse and ensures consistent fresh QueryClient per test.
 */
export function renderWithProviders(
  ui: ReactElement,
  options: { route?: string; renderOptions?: Omit<RenderOptions, 'wrapper'> } = {}
) {
  const { route = '/', renderOptions } = options
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  })

  const Wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <I18nextProvider i18n={i18n}>
        <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
      </I18nextProvider>
    </QueryClientProvider>
  )

  return { ...render(ui, { wrapper: Wrapper, ...renderOptions }), queryClient }
}
