// User types
export interface User {
  id: string
  email: string
  firstName: string | null
  lastName: string | null
  phone: string | null
  role: 'client' | 'admin'
  credits: number
  locale: string
  theme: string
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: User
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
}

// Reservation types
export interface Reservation {
  id: string
  userId: string
  userName: string | null
  userEmail: string | null
  blockId: string | null
  date: string
  startTime: string
  endTime: string
  status: 'confirmed' | 'cancelled' | 'completed'
  creditsUsed: number
  pricingItemId: string | null
  pricingItemName: string | null
  createdAt: string
  cancelledAt: string | null
}

export interface AvailableSlot {
  start: string
  end: string
  blockId: string
  isAvailable: boolean
}

export interface AvailableSlotsResponse {
  slots: AvailableSlot[]
}

export interface ReservationCalendarEvent {
  id: string
  title: string
  start: string
  end: string
  status: string
  clientName: string | null
  clientEmail: string | null
}

// Availability Block types
export interface AvailabilityBlock {
  id: string
  name: string | null
  daysOfWeek: number[]
  startTime: string
  endTime: string
  slotDurationMinutes: number
  breakAfterSlots: number | null
  breakDurationMinutes: number | null
  isActive: boolean
  createdAt: string
}

// Admin Calendar Slot (individual generated slots)
export interface AdminCalendarSlot {
  id: string
  blockId: string
  date: string
  startTime: string
  endTime: string
  status: 'available' | 'reserved' | 'blocked' | 'past'
  reservation: SlotReservation | null
}

export interface SlotReservation {
  id: string
  userName: string | null
  userEmail: string | null
  status: string
  note: string | null
}

// Credit types
export interface CreditPackage {
  id: string
  nameCs: string
  nameEn: string | null
  credits: number
  bonusCredits: number
  priceCzk: number
  isActive: boolean
  sortOrder: number
}

export interface CreditTransaction {
  id: string
  userId: string
  amount: number
  type: 'purchase' | 'reservation' | 'plan_purchase' | 'admin_adjustment' | 'refund'
  referenceId: string | null
  gopayPaymentId: string | null
  note: string | null
  createdAt: string
}

export interface CreditBalance {
  balance: number
  userId: string
}

// Pricing types
export interface PricingItem {
  id: string
  nameCs: string
  nameEn: string | null
  descriptionCs: string | null
  descriptionEn: string | null
  credits: number
  isActive: boolean
  sortOrder: number
}

// Training Plan types
export interface TrainingPlan {
  id: string
  nameCs: string
  nameEn: string | null
  descriptionCs: string | null
  descriptionEn: string | null
  credits: number
  previewImage: string | null
  hasFile: boolean
  isActive: boolean
  createdAt: string
}

export interface PurchasedPlan {
  id: string
  userId: string
  planId: string
  planName: string
  creditsUsed: number
  purchasedAt: string
}

// Client Note types
export interface ClientNote {
  id: string
  userId: string
  adminId: string | null
  adminName: string | null
  note: string
  createdAt: string
}

// Payment types
export interface GopayPayment {
  id: string
  userId: string
  userName: string | null
  gopayId: number | null
  amount: number
  currency: string
  state: string
  creditPackageId: string | null
  creditPackageName: string | null
  createdAt: string
  updatedAt: string
}

export interface PaymentResponse {
  paymentId: string
  gwUrl: string | null
  status?: string
  credits?: number
  newBalance?: number
}

// Dashboard types
export interface DashboardStats {
  todayReservations: number
  weekReservations: number
  todayList: Reservation[]
}

// API Error
export interface ApiError {
  error: string
  message: string
}

// Pagination
export interface PageDTO<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  hasNext: boolean
  hasPrevious: boolean
}
