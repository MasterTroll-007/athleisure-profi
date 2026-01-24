import { useQuery } from '@tanstack/react-query'
import { reservationApi, creditApi, adminApi, calendarApi } from '@/services/api'
import type { DateRange } from '@/types/calendar'

interface UseCalendarQueriesOptions {
  isAdmin: boolean
  dateRange: DateRange
}

export function useCalendarQueries({ isAdmin, dateRange }: UseCalendarQueriesOptions) {
  // User slots query
  const {
    data: slotsResponse,
    isLoading: isUserLoading,
    isFetching: isUserFetching,
  } = useQuery({
    queryKey: ['availableSlots', 'range', dateRange],
    queryFn: () => reservationApi.getAvailableSlotsRange(dateRange.start, dateRange.end),
    enabled: !isAdmin,
    placeholderData: (previousData) => previousData,
    staleTime: 30000,
  })

  // Admin slots query
  const {
    data: adminSlots,
    isLoading: isAdminLoading,
    isFetching: isAdminFetching,
  } = useQuery({
    queryKey: ['admin', 'slots', dateRange],
    queryFn: () => adminApi.getSlots(dateRange.start, dateRange.end),
    enabled: isAdmin,
    placeholderData: (previousData) => previousData,
    staleTime: 30000,
  })

  // User's reservations
  const { data: myReservations } = useQuery({
    queryKey: ['myReservations'],
    queryFn: () => reservationApi.getMyReservations(),
    enabled: !isAdmin,
  })

  // Pricing items (for default pricing)
  const { data: pricingItems } = useQuery({
    queryKey: ['pricing'],
    queryFn: creditApi.getPricing,
  })

  // Admin templates
  const { data: templates } = useQuery({
    queryKey: ['admin', 'templates'],
    queryFn: () => adminApi.getTemplates(),
    enabled: isAdmin,
  })

  // Calendar settings
  const { data: calendarSettings } = useQuery({
    queryKey: ['calendarSettings'],
    queryFn: calendarApi.getSettings,
  })

  const defaultPricing = pricingItems?.find((p) => p.credits === 1)
  const isLoading = isAdmin ? isAdminLoading : isUserLoading
  const isFetching = isAdmin ? isAdminFetching : isUserFetching

  return {
    // Data
    slotsResponse,
    adminSlots,
    myReservations,
    pricingItems,
    defaultPricing,
    templates,
    calendarSettings,
    // Loading states
    isLoading,
    isFetching,
  }
}
