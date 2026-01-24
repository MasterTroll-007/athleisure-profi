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

    // Skip auth handling for public endpoints
    const publicPaths = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/verify-email', '/auth/resend-verification']
    const isPublicEndpoint = publicPaths.some(path => originalRequest.url?.includes(path))

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
  }): Promise<User> => {
    const { data } = await api.patch<User>('/auth/me', params)
    return data
  },

  changePassword: async (currentPassword: string, newPassword: string): Promise<void> => {
    await api.post('/auth/change-password', { currentPassword, newPassword })
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

  purchase: async (id: string): Promise<PurchasedPlan> => {
    const { data } = await api.post<PurchasedPlan>(`/plans/${id}/purchase`)
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
  }): Promise<Slot> => {
    const { data } = await api.patch<Slot>(`/admin/slots/${id}`, params)
    return data
  },

  deleteSlot: async (id: string): Promise<void> => {
    await api.delete(`/admin/slots/${id}`)
  },

  unlockWeek: async (weekStartDate: string): Promise<{ unlockedCount: number }> => {
    const { data } = await api.post<{ unlockedCount: number }>('/admin/slots/unlock-week', { weekStartDate })
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
  }): Promise<SlotTemplate> => {
    const { data } = await api.post<SlotTemplate>('/admin/templates', params)
    return data
  },

  updateTemplate: async (id: string, params: {
    name?: string
    slots?: TemplateSlot[]
    isActive?: boolean
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

  getClientTransactions: async (id: string): Promise<CreditTransaction[]> => {
    const { data } = await api.get<CreditTransaction[]>(`/admin/clients/${id}/transactions`)
    return data
  },

  getClientNotes: async (id: string): Promise<ClientNote[]> => {
    const { data } = await api.get<ClientNote[]>(`/admin/clients/${id}/notes`)
    return data
  },

  addClientNote: async (userId: string, note: string): Promise<ClientNote> => {
    const { data } = await api.post<ClientNote>(`/admin/clients/${userId}/notes`, { note })
    return data
  },

  deleteClientNote: async (noteId: string): Promise<void> => {
    await api.delete(`/admin/clients/notes/${noteId}`)
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
    await api.post(`/admin/plans/${id}/upload-pdf`, formData, {
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

  // Payments
  getPayments: async (): Promise<GopayPayment[]> => {
    const { data } = await api.get<GopayPayment[]>('/admin/payments')
    return data
  },
}

// Calendar API (for both clients and admins)
export const calendarApi = {
  getSettings: async (): Promise<AdminSettings> => {
    const { data } = await api.get<AdminSettings>('/availability/calendar-settings')
    return data
  },
}

export default api
