import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import type {
  User,
  AuthResponse,
  TokenResponse,
  Reservation,
  AvailableSlotsResponse,
  ReservationCalendarEvent,
  CreditPackage,
  CreditTransaction,
  CreditBalance,
  PricingItem,
  TrainingPlan,
  PurchasedPlan,
  PurchasePlanResponse,
  ClientNote,
  GopayPayment,
  PaymentResponse,
  DashboardStats,
  PageDTO,
  Slot,
  SlotTemplate,
  TemplateSlot,
  AdminSettings,
  Trainer,
  CancellationPolicy,
  CancellationRefundPreview,
  CancellationResult,
  TrainingFeedback,
  TrainingLocation,
  TrainingLocationInput,
  WaitlistEntry,
  BodyMeasurement,
  WorkoutExercise,
  WorkoutLog,
} from '@/types/api'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,  // Include cookies in all requests
})

// Request interceptor for auth token
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Helper to handle logout and redirect
const handleAuthFailure = () => {
  localStorage.removeItem('accessToken')
  // Redirect to login - the page reload will clear all React state
  if (window.location.pathname !== '/login' &&
      window.location.pathname !== '/register' &&
      window.location.pathname !== '/verify-email') {
    window.location.href = '/login'
  }
}

// Response interceptor for token refresh
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    // Skip auth handling for public endpoints. Use `endsWith` instead of
    // `includes` so a path like `/auth/login-history` (future endpoint) is
    // not falsely matched by `/auth/login`.
    const publicPaths = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/verify-email', '/auth/resend-verification']
    const urlPath = originalRequest.url?.split('?')[0] ?? ''
    const isPublicEndpoint = publicPaths.some(path => urlPath.endsWith(path))

    if (error.response?.status === 401 && !originalRequest._retry && !isPublicEndpoint) {
      originalRequest._retry = true

      try {
        // Refresh token is sent automatically via HttpOnly cookie
        const response = await axios.post<TokenResponse>('/api/auth/refresh', {}, {
          withCredentials: true  // Include cookies in request
        })

        localStorage.setItem('accessToken', response.data.accessToken)
        // Note: refreshToken is stored in HttpOnly cookie, not localStorage

        originalRequest.headers.Authorization = `Bearer ${response.data.accessToken}`
        return api(originalRequest)
      } catch {
        handleAuthFailure()
        return Promise.reject(error)
      }
    }

    // Handle 401 on public endpoints (like invalid credentials)
    if (error.response?.status === 401 && isPublicEndpoint) {
      return Promise.reject(error)
    }

    // Handle 403 (forbidden) - user is authenticated but not authorized
    if (error.response?.status === 403) {
      // Optionally redirect to home or show error
      return Promise.reject(error)
    }

    return Promise.reject(error)
  }
)

// Auth API
export const authApi = {
  login: async (email: string, password: string, rememberMe: boolean = false): Promise<AuthResponse> => {
    const { data } = await api.post<AuthResponse>('/auth/login', { email, password, rememberMe })
    return data
  },

  register: async (params: {
    email: string
    password: string
    firstName?: string
    lastName?: string
    phone?: string
    trainerCode: string
  }): Promise<{ message: string; email: string }> => {
    const { data } = await api.post<{ message: string; email: string }>('/auth/register', params)
    return data
  },

  getTrainerByCode: async (code: string): Promise<{ firstName: string | null; lastName: string | null }> => {
    const { data } = await api.get<{ firstName: string | null; lastName: string | null }>(`/auth/trainer/${code}`)
    return data
  },

  verifyEmail: async (token: string): Promise<AuthResponse> => {
    const { data } = await api.post<AuthResponse>('/auth/verify-email', { token })
    return data
  },

  resendVerification: async (email: string): Promise<{ message: string }> => {
    const { data } = await api.post<{ message: string }>('/auth/resend-verification', { email })
    return data
  },

  refresh: async (refreshToken: string): Promise<TokenResponse> => {
    const { data } = await api.post<TokenResponse>('/auth/refresh', { refreshToken })
    return data
  },

  logout: async (): Promise<void> => {
    // Refresh token is sent automatically via HttpOnly cookie
    await api.post('/auth/logout', {})
  },

  getMe: async (): Promise<User> => {
    const { data } = await api.get<User>('/auth/me')
    return data
  },

  updateProfile: async (params: {
    firstName?: string
    lastName?: string
    phone?: string
    locale?: string
    theme?: string
    emailRemindersEnabled?: boolean
    reminderHoursBefore?: number
  }): Promise<User> => {
    const { data } = await api.patch<User>('/auth/me', params)
    return data
  },

  changePassword: async (currentPassword: string, newPassword: string): Promise<void> => {
    await api.post('/auth/change-password', { currentPassword, newPassword })
  },

  exportMyData: async (): Promise<Blob> => {
    const response = await api.get('/auth/me/export', { responseType: 'blob' })
    return response.data
  },

  deleteMyAccount: async (): Promise<void> => {
    await api.delete('/auth/me')
  },

  getMyMeasurements: async (): Promise<BodyMeasurement[]> => {
    const { data } = await api.get('/auth/me/measurements')
    return data
  },

  forgotPassword: async (email: string): Promise<{ message: string }> => {
    const { data } = await api.post<{ message: string }>('/auth/forgot-password', { email })
    return data
  },

  resetPassword: async (token: string, newPassword: string): Promise<{ message: string }> => {
    const { data } = await api.post<{ message: string }>('/auth/reset-password', { token, newPassword })
    return data
  },
}

// Reservation API
export const reservationApi = {
  getAvailableSlots: async (date: string): Promise<AvailableSlotsResponse> => {
    const { data } = await api.get<AvailableSlotsResponse>(`/reservations/available/${date}`)
    return data
  },

  getAvailableSlotsRange: async (start: string, end: string): Promise<AvailableSlotsResponse> => {
    const { data } = await api.get<AvailableSlotsResponse>(`/reservations/available`, {
      params: { start, end }
    })
    return data
  },

  create: async (params: {
    date: string
    startTime: string
    endTime: string
    blockId: string
    pricingItemId?: string
  }): Promise<Reservation> => {
    const { data } = await api.post<Reservation>('/reservations', params)
    return data
  },

  getMyReservations: async (): Promise<Reservation[]> => {
    const { data } = await api.get<Reservation[]>('/reservations')
    return data
  },

  getUpcoming: async (): Promise<Reservation[]> => {
    const { data } = await api.get<Reservation[]>('/reservations/upcoming')
    return data
  },

  cancel: async (id: string): Promise<CancellationResult> => {
    const { data } = await api.delete<CancellationResult>(`/reservations/${id}`)
    return data
  },

  getRefundPreview: async (id: string): Promise<CancellationRefundPreview> => {
    const { data } = await api.get<CancellationRefundPreview>(`/reservations/${id}/refund-preview`)
    return data
  },

  exportIcal: (): string => '/api/reservations/ical',

  downloadIcal: async (): Promise<Blob> => {
    const response = await api.get('/reservations/ical', { responseType: 'blob' })
    return response.data
  },

  joinWaitlist: async (slotId: string): Promise<{ id: string; status: string }> => {
    const { data } = await api.post(`/reservations/waitlist/${slotId}`)
    return data
  },

  leaveWaitlist: async (slotId: string): Promise<void> => {
    await api.delete(`/reservations/waitlist/${slotId}`)
  },

  getMyWorkouts: async (): Promise<WorkoutLog[]> => {
    const { data } = await api.get('/reservations/workouts/my')
    return data
  },

  getMyWaitlist: async (): Promise<WaitlistEntry[]> => {
    const { data } = await api.get('/reservations/waitlist/my')
    return data
  },
}

// Feedback API
export const feedbackApi = {
  create: async (reservationId: string, rating: number, comment?: string): Promise<TrainingFeedback> => {
    const { data } = await api.post<TrainingFeedback>('/feedback', { reservationId, rating, comment })
    return data
  },

  getForReservation: async (reservationId: string): Promise<TrainingFeedback | null> => {
    const { data } = await api.get<TrainingFeedback | null>(`/feedback/reservation/${reservationId}`)
    return data
  },

  getMyFeedback: async (): Promise<TrainingFeedback[]> => {
    const { data } = await api.get<TrainingFeedback[]>('/feedback/my')
    return data
  },
}

// Credit API
export const creditApi = {
  getBalance: async (): Promise<CreditBalance> => {
    const { data } = await api.get<CreditBalance>('/credits/balance')
    return data
  },

  getPackages: async (): Promise<CreditPackage[]> => {
    const { data } = await api.get<CreditPackage[]>('/credits/packages')
    return data
  },

  getHistory: async (limit = 50): Promise<CreditTransaction[]> => {
    const { data } = await api.get<CreditTransaction[]>(`/credits/history?limit=${limit}`)
    return data
  },

  getReceiptUrl: (transactionId: string): string => `/api/credits/transactions/${transactionId}/receipt`,

  getReceipt: async (transactionId: string): Promise<Blob> => {
    const response = await api.get(`/credits/transactions/${transactionId}/receipt`, { responseType: 'blob' })
    return response.data
  },

  getPricing: async (): Promise<PricingItem[]> => {
    const { data } = await api.get<PricingItem[]>('/credits/pricing')
    return data
  },

  purchase: async (packageId: string): Promise<PaymentResponse> => {
    const { data } = await api.post<PaymentResponse>('/credits/purchase', { packageId })
    return data
  },

  simulatePayment: async (paymentId: string): Promise<GopayPayment> => {
    const { data } = await api.post<GopayPayment>(`/credits/payment/${paymentId}/simulate-success`)
    return data
  },
}

// Plan API
export const planApi = {
  getAll: async (): Promise<TrainingPlan[]> => {
    const { data } = await api.get<TrainingPlan[]>('/plans')
    return data
  },

  getById: async (id: string): Promise<TrainingPlan> => {
    const { data } = await api.get<TrainingPlan>(`/plans/${id}`)
    return data
  },

  purchase: async (id: string): Promise<PurchasePlanResponse> => {
    const { data } = await api.post<PurchasePlanResponse>(`/plans/${id}/purchase`)
    return data
  },

  getMyPlans: async (): Promise<PurchasedPlan[]> => {
    const { data } = await api.get<PurchasedPlan[]>('/plans/my')
    return data
  },

  checkPurchase: async (id: string): Promise<boolean> => {
    const { data } = await api.get<{ purchased: boolean }>(`/plans/${id}/check-purchase`)
    return data.purchased
  },

  getDownloadUrl: (id: string): string => `/api/plans/${id}/download`,
}

// Admin API
export const adminApi = {
  getStats: async (): Promise<DashboardStats> => {
    const { data } = await api.get<DashboardStats>('/admin/dashboard')
    return data
  },

  // Settings
  getSettings: async (): Promise<AdminSettings> => {
    const { data } = await api.get<AdminSettings>('/admin/settings')
    return data
  },

  updateSettings: async (params: Partial<AdminSettings>): Promise<AdminSettings> => {
    const { data } = await api.patch<AdminSettings>('/admin/settings', params)
    return data
  },

  regenerateInviteCode: async (): Promise<AdminSettings> => {
    const { data } = await api.post<AdminSettings>('/admin/settings/regenerate-code')
    return data
  },

  // Cancellation Policy
  getCancellationPolicy: async (): Promise<CancellationPolicy> => {
    const { data } = await api.get<CancellationPolicy>('/admin/settings/cancellation-policy')
    return data
  },

  updateCancellationPolicy: async (params: Partial<CancellationPolicy>): Promise<CancellationPolicy> => {
    const { data } = await api.patch<CancellationPolicy>('/admin/settings/cancellation-policy', params)
    return data
  },

  // Trainers
  getTrainers: async (): Promise<Trainer[]> => {
    const { data } = await api.get<Trainer[]>('/admin/trainers')
    return data
  },

  assignTrainer: async (clientId: string, trainerId: string): Promise<User> => {
    const { data } = await api.post<User>(`/admin/clients/${clientId}/assign-trainer`, { trainerId })
    return data
  },

  getTodayReservations: async (): Promise<ReservationCalendarEvent[]> => {
    // Use local timezone to avoid UTC date shifting
    const now = new Date()
    const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    const { data } = await api.get<ReservationCalendarEvent[]>(`/admin/reservations?start=${today}&end=${today}`)
    return data
  },

  // ============ SLOTS SYSTEM ============

  getSlots: async (start: string, end: string): Promise<Slot[]> => {
    const { data } = await api.get<Slot[]>(`/admin/slots?start=${start}&end=${end}`)
    return data
  },

  createSlot: async (params: {
    date: string
    startTime: string
    durationMinutes?: number
    note?: string
    assignedUserId?: string
    pricingItemIds?: string[]
    locationId?: string | null
  }): Promise<Slot> => {
    const { data } = await api.post<Slot>('/admin/slots', params)
    return data
  },

  updateSlot: async (id: string, params: {
    status?: string
    note?: string
    assignedUserId?: string
    date?: string
    startTime?: string
    endTime?: string
    pricingItemIds?: string[]
    locationId?: string | null
    clearLocation?: boolean
  }): Promise<Slot> => {
    const { data } = await api.patch<Slot>(`/admin/slots/${id}`, params)
    return data
  },

  getSlotCancellationPreview: async (id: string): Promise<{ slotId: string; affectedReservations: Array<{ reservationId: string; userId: string; userName: string | null; userEmail: string | null; creditsUsed: number }>; totalCreditsToRefund: number }> => {
    const { data } = await api.get(`/admin/slots/${id}/cancellation-preview`)
    return data
  },

  deleteSlot: async (id: string): Promise<void> => {
    await api.delete(`/admin/slots/${id}`)
  },

  unlockWeek: async (params: { weekStartDate: string; endDate?: string }): Promise<{ unlockedCount: number }> => {
    const { data } = await api.post<{ unlockedCount: number }>('/admin/slots/unlock-week', params)
    return data
  },

  applyTemplate: async (templateId: string, weekStartDate: string): Promise<{ createdSlots: number; slots: Slot[] }> => {
    const { data } = await api.post<{ createdSlots: number; slots: Slot[] }>('/admin/slots/apply-template', { templateId, weekStartDate })
    return data
  },

  // ============ TEMPLATES ============

  getTemplates: async (): Promise<SlotTemplate[]> => {
    const { data } = await api.get<SlotTemplate[]>('/admin/templates')
    return data
  },

  getTemplate: async (id: string): Promise<SlotTemplate> => {
    const { data } = await api.get<SlotTemplate>(`/admin/templates/${id}`)
    return data
  },

  createTemplate: async (params: {
    name: string
    slots: TemplateSlot[]
    locationId?: string | null
  }): Promise<SlotTemplate> => {
    const { data } = await api.post<SlotTemplate>('/admin/templates', params)
    return data
  },

  updateTemplate: async (id: string, params: {
    name?: string
    slots?: TemplateSlot[]
    isActive?: boolean
    locationId?: string | null
    clearLocation?: boolean
  }): Promise<SlotTemplate> => {
    const { data } = await api.patch<SlotTemplate>(`/admin/templates/${id}`, params)
    return data
  },

  deleteTemplate: async (id: string): Promise<void> => {
    await api.delete(`/admin/templates/${id}`)
  },

  // Admin Reservation Management
  createReservation: async (params: {
    userId: string
    date: string
    startTime: string
    endTime: string
    blockId: string
    deductCredits?: boolean
    pricingItemId?: string
  }): Promise<Reservation> => {
    const { data } = await api.post<Reservation>('/admin/reservations', params)
    return data
  },

  cancelReservation: async (id: string, refundCredits: boolean = true): Promise<Reservation> => {
    const { data } = await api.delete<Reservation>(`/admin/reservations/${id}?refundCredits=${refundCredits}`)
    return data
  },

  updateReservationNote: async (id: string, note: string | null): Promise<Reservation> => {
    const { data } = await api.patch<Reservation>(`/admin/reservations/${id}/note`, { note })
    return data
  },

  markAttendance: async (id: string, status: 'completed' | 'no_show'): Promise<Reservation> => {
    const { data } = await api.patch<Reservation>(`/admin/reservations/${id}/attendance`, { status })
    return data
  },

  rescheduleReservation: async (id: string, params: {
    targetSlotId?: string
    date: string
    startTime: string
    endTime: string
    createSlotIfMissing?: boolean
  }): Promise<Reservation> => {
    const { data } = await api.patch<Reservation>(`/admin/reservations/${id}/reschedule`, params)
    return data
  },

  searchClients: async (query: string): Promise<User[]> => {
    const { data } = await api.get<User[]>(`/admin/clients/search?q=${encodeURIComponent(query)}`)
    return data
  },

  // Clients
  getClients: async (page = 0, size = 20): Promise<PageDTO<User>> => {
    const { data } = await api.get<PageDTO<User>>(`/admin/clients?page=${page}&size=${size}`)
    return data
  },

  getClient: async (id: string): Promise<User> => {
    const { data } = await api.get<User>(`/admin/clients/${id}`)
    return data
  },

  getClientReservations: async (id: string): Promise<Reservation[]> => {
    const { data } = await api.get<Reservation[]>(`/admin/clients/${id}/reservations`)
    return data
  },

  getClientWorkouts: async (id: string): Promise<WorkoutLog[]> => {
    const { data } = await api.get<WorkoutLog[]>(`/admin/clients/${id}/workouts`)
    return data
  },

  getClientTransactions: async (id: string): Promise<CreditTransaction[]> => {
    const { data } = await api.get<CreditTransaction[]>(`/admin/clients/${id}/transactions`)
    return data
  },

  getClientNotes: async (id: string): Promise<ClientNote[]> => {
    const { data } = await api.get<ClientNote[]>(`/admin/clients/${id}/notes`)
    return data
  },

  addClientNote: async (userId: string, content: string): Promise<ClientNote> => {
    // Backend expects `content` in CreateClientNoteRequest, not `note`.
    const { data } = await api.post<ClientNote>(`/admin/clients/${userId}/notes`, { content })
    return data
  },

  deleteClientNote: async (noteId: string): Promise<void> => {
    await api.delete(`/admin/notes/${noteId}`)
  },

  blockClient: async (id: string): Promise<User> => {
    const { data } = await api.post<User>(`/admin/clients/${id}/block`)
    return data
  },

  unblockClient: async (id: string): Promise<User> => {
    const { data } = await api.post<User>(`/admin/clients/${id}/unblock`)
    return data
  },

  adjustClientCredits: async (userId: string, amount: number, reason?: string): Promise<CreditBalance> => {
    const { data } = await api.post<CreditBalance>(`/admin/clients/${userId}/adjust-credits`, {
      amount,
      reason,
    })
    return data
  },

  // Plans
  getPlans: async (): Promise<TrainingPlan[]> => {
    const { data } = await api.get<TrainingPlan[]>('/admin/plans')
    return data
  },

  createPlan: async (params: {
    nameCs: string
    nameEn?: string
    descriptionCs?: string
    descriptionEn?: string
    durationWeeks?: number
    sessionsPerWeek?: number
    creditsCost?: number
    credits?: number
    isActive?: boolean
  }): Promise<TrainingPlan> => {
    const { data } = await api.post<TrainingPlan>('/admin/plans', params)
    return data
  },

  updatePlan: async (id: string, params: Partial<{
    nameCs: string
    nameEn: string
    descriptionCs: string
    descriptionEn: string
    durationWeeks: number
    sessionsPerWeek: number
    creditsCost: number
    credits: number
    isActive: boolean
  }>): Promise<TrainingPlan> => {
    const { data } = await api.patch<TrainingPlan>(`/admin/plans/${id}`, params)
    return data
  },

  deletePlan: async (id: string): Promise<void> => {
    await api.delete(`/admin/plans/${id}`)
  },

  uploadPlanPdf: async (id: string, file: File): Promise<void> => {
    const formData = new FormData()
    formData.append('file', file)
    await api.post(`/admin/plans/${id}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  uploadPlanPreview: async (id: string, file: File): Promise<void> => {
    const formData = new FormData()
    formData.append('file', file)
    await api.post(`/admin/plans/${id}/upload-preview`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  // Pricing
  getAllPricing: async (): Promise<PricingItem[]> => {
    const { data } = await api.get<PricingItem[]>('/admin/pricing')
    return data
  },

  createPricing: async (params: {
    nameCs: string
    nameEn?: string
    descriptionCs?: string
    descriptionEn?: string
    credits: number
    isActive?: boolean
    sortOrder?: number
  }): Promise<PricingItem> => {
    const { data } = await api.post<PricingItem>('/admin/pricing', params)
    return data
  },

  updatePricing: async (id: string, params: Partial<{
    nameCs: string
    nameEn: string
    descriptionCs: string
    descriptionEn: string
    credits: number
    isActive: boolean
    sortOrder: number
  }>): Promise<PricingItem> => {
    const { data } = await api.patch<PricingItem>(`/admin/pricing/${id}`, params)
    return data
  },

  deletePricing: async (id: string): Promise<void> => {
    await api.delete(`/admin/pricing/${id}`)
  },

  // Packages
  getAllPackages: async (): Promise<CreditPackage[]> => {
    const { data } = await api.get<CreditPackage[]>('/admin/packages')
    return data
  },

  createCreditPackage: async (params: {
    nameCs: string
    nameEn?: string
    description?: string
    credits: number
    priceCzk: number
    isActive?: boolean
    highlightType?: string
    isBasic?: boolean
  }): Promise<CreditPackage> => {
    const { data } = await api.post<CreditPackage>('/admin/packages', params)
    return data
  },

  updateCreditPackage: async (id: string, params: Partial<{
    nameCs: string
    nameEn: string
    description: string
    credits: number
    priceCzk: number
    isActive: boolean
    highlightType: string
    isBasic: boolean
    sortOrder: number
  }>): Promise<CreditPackage> => {
    const { data } = await api.patch<CreditPackage>(`/admin/packages/${id}`, params)
    return data
  },

  deleteCreditPackage: async (id: string): Promise<void> => {
    await api.delete(`/admin/packages/${id}`)
  },

  // Bulk slot operations
  bulkDeleteSlots: async (slotIds: string[]): Promise<{ deleted: number }> => {
    const { data } = await api.post<{ deleted: number }>('/admin/slots/bulk-delete', { slotIds })
    return data
  },

  bulkUpdateSlots: async (slotIds: string[], status: string): Promise<{ updated: number }> => {
    const { data } = await api.post<{ updated: number }>('/admin/slots/bulk-update', { slotIds, status })
    return data
  },

  // Auto-generate slots from templates
  autoGenerateSlots: async (weekStartDate: string): Promise<{ generated: number }> => {
    const { data } = await api.post<{ generated: number }>('/admin/slots/auto-generate', { weekStartDate })
    return data
  },

  // Measurements
  getClientMeasurements: async (clientId: string): Promise<BodyMeasurement[]> => {
    const { data } = await api.get(`/admin/clients/${clientId}/measurements`)
    return data
  },

  createClientMeasurement: async (clientId: string, measurement: { date: string; weight?: number; bodyFat?: number; chest?: number; waist?: number; hips?: number; bicep?: number; thigh?: number; notes?: string }): Promise<BodyMeasurement> => {
    const { data } = await api.post(`/admin/clients/${clientId}/measurements`, measurement)
    return data
  },

  // Workouts
  createWorkoutLog: async (reservationId: string, exercises: WorkoutExercise[], notes?: string): Promise<WorkoutLog> => {
    const { data } = await api.post(`/admin/reservations/${reservationId}/workout`, { exercises, notes })
    return data
  },

  getWorkoutLog: async (reservationId: string): Promise<WorkoutLog | null> => {
    const { data } = await api.get(`/admin/reservations/${reservationId}/workout`)
    return data
  },

  updateWorkoutLog: async (reservationId: string, exercises: WorkoutExercise[], notes?: string): Promise<WorkoutLog> => {
    const { data } = await api.put(`/admin/reservations/${reservationId}/workout`, { exercises, notes })
    return data
  },

  // Announcements
  createAnnouncement: async (subject: string, message: string): Promise<{ id: string; recipientsCount: number }> => {
    const { data } = await api.post('/admin/announcements', { subject, message })
    return data
  },

  getAnnouncements: async (): Promise<Array<{ id: string; subject: string; message: string; recipientsCount: number; createdAt: string }>> => {
    const { data } = await api.get('/admin/announcements')
    return data
  },

  // Feedback
  getFeedbackSummary: async (): Promise<{ averageRating: number | null; totalCount: number; distribution: Record<number, number> }> => {
    const { data } = await api.get('/admin/feedback/summary')
    return data
  },

  getAllFeedback: async (): Promise<Array<{ id: string; reservationId: string; userId: string; userName: string | null; rating: number; comment: string | null; date: string | null; createdAt: string }>> => {
    const { data } = await api.get('/admin/feedback')
    return data
  },

  // Statistics
  getStatistics: async (months?: number): Promise<{
    totalClients: number; totalReservations: number;
    completedCount: number; noShowCount: number;
    attendanceRate: number; noShowRate: number;
    averageRating: number; totalFeedback: number;
    ratingDistribution: Record<number, number>;
    creditsSold: number;
    monthlyStats: Array<{ month: string; reservations: number }>
  }> => {
    const { data } = await api.get('/admin/statistics', { params: { months } })
    return data
  },

  // Avatar
  uploadAvatar: async (file: File): Promise<User> => {
    const formData = new FormData()
    formData.append('file', file)
    const { data } = await api.post<User>('/auth/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return data
  },

  // Payments
  getPayments: async (): Promise<GopayPayment[]> => {
    const { data } = await api.get<GopayPayment[]>('/admin/payments')
    return data
  },

  exportCsv: async (kind: 'clients' | 'reservations' | 'payments', params?: { start?: string; end?: string }): Promise<Blob> => {
    const response = await api.get(`/admin/export/${kind}`, { params, responseType: 'blob' })
    return response.data
  },
}

// Calendar API (for both clients and admins)
export const calendarApi = {
  getSettings: async (): Promise<AdminSettings> => {
    const { data } = await api.get<AdminSettings>('/availability/calendar-settings')
    return data
  },
}

// Training Locations API — client trainer-scoped list + admin CRUD
export const locationsApi = {
  // Client: active locations for the authenticated user's trainer
  listActive: async (): Promise<TrainingLocation[]> => {
    const { data } = await api.get<TrainingLocation[]>('/locations')
    return data
  },

  // Admin: full list for current admin (includes inactive)
  listAdmin: async (): Promise<TrainingLocation[]> => {
    const { data } = await api.get<TrainingLocation[]>('/admin/locations')
    return data
  },

  create: async (params: TrainingLocationInput): Promise<TrainingLocation> => {
    const { data } = await api.post<TrainingLocation>('/admin/locations', params)
    return data
  },

  update: async (id: string, params: Partial<TrainingLocationInput>): Promise<TrainingLocation> => {
    const { data } = await api.patch<TrainingLocation>(`/admin/locations/${id}`, params)
    return data
  },

  remove: async (id: string): Promise<void> => {
    await api.delete(`/admin/locations/${id}`)
  },
}

export default api
