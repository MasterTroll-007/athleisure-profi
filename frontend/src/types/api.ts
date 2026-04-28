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
  trainerId: string | null
  trainerName: string | null
  calendarStartHour: number
  calendarEndHour: number
  isBlocked?: boolean
  avatarPath?: string | null
  emailRemindersEnabled: boolean
  reminderHoursBefore: number
  createdAt: string
}

// Admin Settings types
export interface AdminSettings {
  calendarStartHour: number
  calendarEndHour: number
  inviteCode: string | null
  inviteLink: string | null
  adjacentBookingRequired: boolean
}

// Cancellation Policy types
export interface CancellationPolicy {
  id: string
  fullRefundHours: number
  partialRefundHours: number | null
  partialRefundPercentage: number | null
  noRefundHours: number
  isActive: boolean
}

export interface CancellationRefundPreview {
  reservationId: string
  creditsUsed: number
  refundPercentage: number
  refundAmount: number
  hoursUntilReservation: number
  policyApplied: 'NO_POLICY' | 'FULL_REFUND' | 'PARTIAL_REFUND' | 'NO_REFUND'
}

export interface CancellationResult {
  reservation: Reservation
  refundAmount: number
  refundPercentage: number
  policyApplied: 'NO_POLICY' | 'FULL_REFUND' | 'PARTIAL_REFUND' | 'NO_REFUND'
}

export interface Trainer {
  id: string
  email: string
  firstName: string | null
  lastName: string | null
  calendarStartHour: number
  calendarEndHour: number
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
  slotId: string | null
  date: string
  startTime: string
  endTime: string
  status: 'confirmed' | 'cancelled' | 'completed' | 'no_show'
  creditsUsed: number
  pricingItemId: string | null
  pricingItemName: string | null
  createdAt: string
  cancelledAt: string | null
  completedAt: string | null
  note?: string | null
  locationId?: string | null
  locationName?: string | null
  locationAddress?: string | null
  locationColor?: string | null
}

export interface AvailableSlot {
  start: string
  end: string
  slotId: string
  isAvailable: boolean
  reservedByUserId: string | null
  pricingItems: PricingItemSummary[]
  locationId?: string | null
  locationName?: string | null
  locationAddress?: string | null
  locationColor?: string | null
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
  locationId?: string | null
  locationName?: string | null
  locationColor?: string | null
}

// Admin Calendar Slot (individual generated slots)
export interface AdminCalendarSlot {
  id: string
  slotId: string
  date: string
  startTime: string
  endTime: string
  status: 'available' | 'reserved' | 'blocked' | 'past'
  reservation: SlotReservation | null
  locationId?: string | null
  locationName?: string | null
  locationColor?: string | null
}

export interface SlotReservation {
  id: string
  userName: string | null
  userEmail: string | null
  status: string
  note: string | null
}

// Credit types
export type PackageHighlight = 'NONE' | 'BEST_SELLER' | 'BEST_VALUE'

export interface CreditPackage {
  id: string
  nameCs: string
  nameEn: string | null
  description: string | null
  credits: number
  priceCzk: number
  currency: string
  isActive: boolean
  sortOrder: number
  highlightType: PackageHighlight
  isBasic: boolean
  discountPercent: number | null
}

export interface CreditTransaction {
  id: string
  userId: string
  amount: number
  type: 'purchase' | 'reservation' | 'plan_purchase' | 'admin_adjustment' | 'refund'
  referenceId: string | null
  gopayPaymentId: string | null
  note: string | null
  expiresAt: string | null
  createdAt: string
}

export interface TrainingFeedback {
  id: string
  reservationId: string
  userId: string
  rating: number
  comment: string | null
  createdAt: string
}

export interface BodyMeasurement {
  id: string
  userId?: string
  date: string
  weight?: number | null
  bodyFat?: number | null
  chest?: number | null
  waist?: number | null
  hips?: number | null
  bicep?: number | null
  thigh?: number | null
  notes?: string | null
  createdAt?: string
}

export interface WorkoutExercise {
  name: string
  sets?: number | null
  reps?: number | null
  weight?: number | null
  duration?: string | null
  notes?: string | null
}

export interface WorkoutLog {
  id: string
  reservationId: string
  exercises: WorkoutExercise[]
  notes: string | null
  date: string | null
  createdAt: string
  updatedAt?: string
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
  durationMinutes?: number
  isActive: boolean
  sortOrder: number
}

// Training Plan types
export interface TrainingPlan {
  id: string
  name?: string
  description?: string | null
  nameCs?: string
  nameEn: string | null
  descriptionCs?: string | null
  descriptionEn: string | null
  credits: number
  price?: number
  currency?: string
  validityDays?: number
  sessionsCount?: number | null
  previewImage?: string | null
  hasFile?: boolean
  isActive: boolean
  createdAt?: string
}

export interface PurchasedPlan {
  id: string
  userId: string
  planId: string
  planName: string | null
  purchaseDate: string
  expiryDate: string
  sessionsRemaining: number | null
  status: string
}

export interface PurchasePlanResponse {
  purchasedPlan: PurchasedPlan
  newBalance: number
}

// Client Note types
export interface ClientNote {
  id: string
  // Backend serialises as `clientId` + `content`. The legacy aliases are
  // kept read-only so existing callers using `userId` / `note` still work.
  clientId: string
  adminId: string | null
  adminName: string | null
  content: string
  createdAt: string
  updatedAt?: string
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
  totalClients?: number
  todayReservations: number
  weekReservations: number
  todayList?: Reservation[]
}

// API Error
export interface ApiError {
  error: string
  message: string
}

// Pricing item summary (embedded in slots)
export interface PricingItemSummary {
  id: string
  nameCs: string
  nameEn: string | null
  credits: number
}

// New Slot System
export type SlotStatus = 'locked' | 'unlocked' | 'reserved' | 'blocked' | 'cancelled'

export interface Slot {
  id: string
  date: string
  startTime: string
  endTime: string
  durationMinutes: number
  status: SlotStatus
  assignedUserId: string | null
  assignedUserName: string | null
  assignedUserEmail: string | null
  note: string | null
  reservationId: string | null
  createdAt: string
  cancelledAt: string | null
  pricingItems: PricingItemSummary[]
  capacity: number
  currentBookings: number
  locationId?: string | null
  locationName?: string | null
  locationAddress?: string | null
  locationColor?: string | null
}

export interface SlotTemplate {
  id: string
  name: string
  slots: TemplateSlot[]
  isActive: boolean
  createdAt: string
  locationId?: string | null
  locationName?: string | null
  locationColor?: string | null
}

// Training Location types
export interface TrainingLocation {
  id: string
  nameCs: string
  nameEn: string | null
  addressCs: string | null
  addressEn: string | null
  color: string
  isActive: boolean
  createdAt: string
}

export interface TrainingLocationInput {
  nameCs: string
  nameEn?: string | null
  addressCs?: string | null
  addressEn?: string | null
  color: string
  isActive?: boolean
}

export interface TemplateSlot {
  id?: string
  dayOfWeek: number
  startTime: string
  endTime: string
  durationMinutes: number
  pricingItemIds: string[]
  capacity?: number
  locationId?: string | null
  locationName?: string | null
  locationColor?: string | null
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
