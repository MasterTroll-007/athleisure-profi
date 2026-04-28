import { describe, it, expect, beforeEach, vi } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/msw/server'
import { ReactNode } from 'react'

// The hook pulls in authStore -> @/main (createRoot). Mock it.
vi.mock('@/main', () => ({
  queryClient: { invalidateQueries: () => {}, clear: () => {} },
}))

// Toast provider only exposes the hook — stub it out so we don't need a DOM tree.
vi.mock('@/components/ui/Toast', () => ({
  useToast: () => ({ showToast: vi.fn() }),
}))

// i18n — simple identity translator so tests are locale-free.
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

import { useCalendarMutations } from './useCalendarMutations'
import { useAuthStore } from '@/stores/authStore'
import { fakeClientUser } from '@/test/msw/handlers'

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

describe('useCalendarMutations', () => {
  beforeEach(() => {
    useAuthStore.setState({
      user: fakeClientUser,
      accessToken: 'tok',
      refreshToken: null,
      isAuthenticated: true,
      isLoading: false,
    })
  })

  it('createReservation success refreshes user credits', async () => {
    server.use(
      http.post('/api/reservations', () =>
        HttpResponse.json({
          id: 'r1',
          userId: fakeClientUser.id,
          date: '2026-05-10',
          startTime: '10:00',
          endTime: '11:00',
          status: 'confirmed',
          creditsUsed: 2,
        })
      ),
      // After booking, fresh user data includes the deducted credits.
      http.get('/api/auth/me', () => HttpResponse.json({ ...fakeClientUser, credits: 8 })),
    )

    const { result } = renderHook(() => useCalendarMutations(), { wrapper })

    await act(async () => {
      await result.current.createReservation.mutateAsync({
        date: '2026-05-10',
        startTime: '10:00',
        endTime: '11:00',
        slotId: 'slot-1',
      })
    })

    await waitFor(() => {
      expect(useAuthStore.getState().user?.credits).toBe(8)
    })
  })

  it('cancelReservation success adds refund amount back to balance', async () => {
    server.use(
      http.delete('/api/reservations/r1', () =>
        HttpResponse.json({
          reservation: {},
          refundAmount: 1,
          refundPercentage: 100,
          policyApplied: 'FULL_REFUND',
        })
      ),
    )

    // Seed user with fewer credits so the refund bumps them up.
    useAuthStore.setState({ ...useAuthStore.getState(), user: { ...fakeClientUser, credits: 3 } })

    const { result } = renderHook(() => useCalendarMutations(), { wrapper })

    await act(async () => {
      await result.current.cancelReservation.mutateAsync('r1')
    })

    expect(useAuthStore.getState().user?.credits).toBe(4)
  })
})
