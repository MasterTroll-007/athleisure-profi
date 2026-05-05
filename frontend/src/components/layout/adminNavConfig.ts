import { Users, LayoutTemplate, Dumbbell, Tag, DollarSign, Settings, BarChart3, Megaphone, Star, MapPin, ClipboardList, FileText } from 'lucide-react'

export const adminMenuItems = [
  { path: '/admin/clients', icon: Users, labelKey: 'admin.clientsTitle' },
  { path: '/admin/templates', icon: LayoutTemplate, labelKey: 'admin.templatesTitle' },
  { path: '/admin/training-pricing', icon: Dumbbell, labelKey: 'admin.trainingPricing.title' },
  { path: '/admin/pricing', icon: Tag, labelKey: 'admin.pricingTitle' },
  { path: '/admin/locations', icon: MapPin, labelKey: 'admin.locations.title' },
  { path: '/admin/payments', icon: DollarSign, labelKey: 'admin.paymentsTitle' },
  { path: '/admin/accounting', icon: FileText, labelKey: 'admin.accountingExportsTitle' },
  { path: '/admin/announcements', icon: Megaphone, labelKey: 'admin.announcementsTitle' },
  { path: '/admin/feedback', icon: Star, labelKey: 'admin.feedbackTitle' },
  { path: '/admin/audit', icon: ClipboardList, labelKey: 'admin.auditTitle' },
  { path: '/admin/statistics', icon: BarChart3, labelKey: 'admin.statistics' },
  { path: '/admin/settings', icon: Settings, labelKey: 'admin.settings.title' },
] as const
