import type { AvailableSlot, Reservation, Slot } from './api'

// Calendar event (internal format used by useCalendarEvents hook)
export interface CalendarEvent {
  id: string
  title: string
  start: string
  end: string
  backgroundColor: string
  borderColor: string
  textColor: string
  pattern?: 'stripes' | null
  opacity?: number
  extendedProps: {
    slot?: AvailableSlot
    reservation?: Reservation
    adminSlot?: Slot
    type: 'slot' | 'reservation' | 'adminSlot'
  }
}

// Month view slot info (similar to mobile app's MonthSlotInfo)
export interface MonthSlotInfo {
  time: string        // "14:00"
  label: string | null // "J. Novák" for admin, null for user
  isReserved: boolean
  isLocked: boolean
  isCancelled: boolean
  isMyReservation: boolean
  isUnlocked?: boolean
}

// Slot colors type
export interface SlotColors {
  bg: string
  border: string
  text: string
  label?: string
  pattern?: 'stripes' | null
  opacity?: number
  icon?: string
}

// Calendar date range
export interface DateRange {
  start: string
  end: string
}

// Admin slot selection state
export interface AdminSlotSelectionState {
  selectedAdminSlot: Slot | null
  showUserSearch: boolean
  searchQuery: string
  selectedUser: import('./api').User | null
  deductCredits: boolean
  noteText: string
  isEditingNote: boolean
  showCancelConfirm: boolean
  cancelWithRefund: boolean
}

// User booking state
export interface UserBookingState {
  selectedSlot: AvailableSlot | null
  isModalOpen: boolean
  selectedReservation: Reservation | null
  isCancelModalOpen: boolean
}

// Create slot form state
export interface CreateSlotFormState {
  date: string
  time: string
  duration: number
  note: string
}
