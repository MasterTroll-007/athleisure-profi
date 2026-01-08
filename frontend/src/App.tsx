import { Routes, Route, Navigate } from 'react-router-dom'
import { useEffect } from 'react'
import { useAuthStore } from '@/stores/authStore'
import { useThemeStore } from '@/stores/themeStore'

// Layout
import Layout from '@/components/layout/Layout'

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
import AdminTemplates from '@/pages/admin/Templates'
import AdminClients from '@/pages/admin/Clients'
import AdminClientDetail from '@/pages/admin/ClientDetail'
import AdminPlans from '@/pages/admin/TrainingPlans'
import AdminPricing from '@/pages/admin/Pricing'
import AdminPayments from '@/pages/admin/Payments'
import AdminSettings from '@/pages/admin/Settings'

// Components
import { Toaster } from '@/components/ui/Toast'
import { ErrorBoundary } from '@/components/ui/ErrorBoundary'
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
    <ErrorBoundary>
      <OfflineIndicator />
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<PublicRoute><Login /></PublicRoute>} />
        <Route path="/register/:trainerCode" element={<PublicRoute><Register /></PublicRoute>} />
        <Route path="/verify-email" element={<VerifyEmail />} />

        {/* Protected routes - all under single Layout */}
        <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          <Route index element={<Home />} />
          <Route path="calendar" element={<NewReservation />} />
          <Route path="reservations" element={<MyReservations />} />
          <Route path="plans" element={<PlansList />} />
          <Route path="plans/my" element={<MyPlans />} />
          <Route path="profile" element={<Profile />} />
          <Route path="credits" element={<BuyCredits />} />

          {/* Admin routes - same Layout, admin check in AdminRoute wrapper */}
          <Route path="admin/templates" element={<AdminRoute><AdminTemplates /></AdminRoute>} />
          <Route path="admin/clients" element={<AdminRoute><AdminClients /></AdminRoute>} />
          <Route path="admin/clients/:id" element={<AdminRoute><AdminClientDetail /></AdminRoute>} />
          <Route path="admin/plans" element={<AdminRoute><AdminPlans /></AdminRoute>} />
          <Route path="admin/pricing" element={<AdminRoute><AdminPricing /></AdminRoute>} />
          <Route path="admin/payments" element={<AdminRoute><AdminPayments /></AdminRoute>} />
          <Route path="admin/settings" element={<AdminRoute><AdminSettings /></AdminRoute>} />
        </Route>

        {/* Redirect /admin to first admin page */}
        <Route path="/admin" element={<Navigate to="/admin/clients" replace />} />

        {/* Catch all */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <Toaster />
    </ErrorBoundary>
  )
}
