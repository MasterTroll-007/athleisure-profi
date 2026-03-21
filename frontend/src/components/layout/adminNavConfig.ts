import { Users, LayoutTemplate, Dumbbell, Tag, DollarSign, Settings, BarChart3, Megaphone, Star } from 'lucide-react'

export const adminMenuItems = [
  { path: '/admin/clients', icon: Users, labelKey: 'admin.clientsTitle' },
  { path: '/admin/templates', icon: LayoutTemplate, labelKey: 'admin.templatesTitle' },
  { path: '/admin/plans', icon: Dumbbell, labelKey: 'admin.plansTitle' },
  { path: '/admin/training-pricing', icon: Dumbbell, labelKey: 'admin.trainingPricing.title' },
  { path: '/admin/pricing', icon: Tag, labelKey: 'admin.pricingTitle' },
  { path: '/admin/payments', icon: DollarSign, labelKey: 'admin.paymentsTitle' },
  { path: '/admin/announcements', icon: Megaphone, labelKey: 'admin.announcementsTitle' },
  { path: '/admin/feedback', icon: Star, labelKey: 'admin.feedbackTitle' },
  { path: '/admin/statistics', icon: BarChart3, labelKey: 'admin.statistics' },
  { path: '/admin/settings', icon: Settings, labelKey: 'admin.settings.title' },
] as const
