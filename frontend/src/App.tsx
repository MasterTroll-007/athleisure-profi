import { Routes, Route, Navigate } from 'react-router-dom'
import { useEffect } from 'react'
import { useAuthStore } from '@/stores/authStore'
import { useThemeStore } from '@/stores/themeStore'

// Layout
import Layout from '@/components/layout/Layout'
import AdminLayout from '@/pages/admin/AdminLayout'

// Pages
import Home from '@/pages/Home'
import Login from '@/pages/Login'
import Register from '@/pages/Register'
import VerifyEmail from '@/pages/VerifyEmail'
import NewReservation from '@/pages/reservations/NewReservation'
import MyReservations from '@/pages/reservations/MyReservations'
import PlansList from '@/pages/plans/PlansList'
import MyPlans from '@/pages/plans/MyPlans'
import Profile from '@/pages/profile/Profile'
import BuyCredits from '@/pages/credits/BuyCredits'

// Admin Pages
import AdminDashboard from '@/pages/admin/Dashboard'
import AdminCalendar from '@/pages/admin/Calendar'
import AdminTemplates from '@/pages/admin/Templates'
import AdminClients from '@/pages/admin/Clients'
import AdminClientDetail from '@/pages/admin/ClientDetail'
import AdminPlans from '@/pages/admin/TrainingPlans'
import AdminPricing from '@/pages/admin/Pricing'
import AdminPayments from '@/pages/admin/Payments'

// Components
import { Toaster } from '@/components/ui/Toast'
import OfflineIndicator from '@/components/ui/OfflineIndicator'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuthStore()

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500" />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}

function AdminRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, user, isLoading } = useAuthStore()

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500" />
      </div>
    )
  }

  if (!isAuthenticated || user?.role !== 'admin') {
    return <Navigate to="/" replace />
  }

  return <>{children}</>
}

function PublicRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuthStore()

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500" />
      </div>
    )
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return <>{children}</>
}

export default function App() {
  const { initAuth } = useAuthStore()
  const { initTheme } = useThemeStore()

  useEffect(() => {
    initTheme()
    initAuth()
  }, [initAuth, initTheme])

  return (
    <>
      <OfflineIndicator />
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<PublicRoute><Login /></PublicRoute>} />
        <Route path="/register" element={<PublicRoute><Register /></PublicRoute>} />
        <Route path="/verify-email" element={<VerifyEmail />} />

        {/* Protected routes */}
        <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          <Route index element={<Home />} />
          <Route path="reservations/new" element={<NewReservation />} />
          <Route path="reservations" element={<MyReservations />} />
          <Route path="plans" element={<PlansList />} />
          <Route path="plans/my" element={<MyPlans />} />
          <Route path="profile" element={<Profile />} />
          <Route path="credits" element={<BuyCredits />} />
        </Route>

        {/* Admin routes */}
        <Route path="/admin" element={<AdminRoute><AdminLayout /></AdminRoute>}>
          <Route index element={<AdminDashboard />} />
          <Route path="calendar" element={<AdminCalendar />} />
          <Route path="templates" element={<AdminTemplates />} />
          <Route path="clients" element={<AdminClients />} />
          <Route path="clients/:id" element={<AdminClientDetail />} />
          <Route path="plans" element={<AdminPlans />} />
          <Route path="pricing" element={<AdminPricing />} />
          <Route path="payments" element={<AdminPayments />} />
        </Route>

        {/* Catch all */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <Toaster />
    </>
  )
}
