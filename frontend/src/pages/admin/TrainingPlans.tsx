import { useState, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Plus, Dumbbell, Edit2, Trash2, Eye, EyeOff, Upload, FileText } from 'lucide-react'
import { Card, Button, Input, Modal, Badge, Spinner } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi } from '@/services/api'
import type { TrainingPlan } from '@/types/api'

const planSchema = z.object({
  nameCs: z.string().min(1),
  nameEn: z.string().optional(),
  descriptionCs: z.string().optional(),
  descriptionEn: z.string().optional(),
  credits: z.coerce.number().min(1),
  isActive: z.boolean().default(true),
})

type PlanForm = z.infer<typeof planSchema>

export default function TrainingPlans() {
  const { t, i18n } = useTranslation()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingPlan, setEditingPlan] = useState<TrainingPlan | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [uploadingPlanId, setUploadingPlanId] = useState<string | null>(null)

  const { data: plans, isLoading } = useQuery({
    queryKey: ['admin', 'plans'],
    queryFn: adminApi.getPlans,
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PlanForm>({
    resolver: zodResolver(planSchema),
    defaultValues: {
      credits: 5,
      isActive: true,
    },
  })

  const createMutation = useMutation({
    mutationFn: adminApi.createPlan,
    onSuccess: () => {
      showToast('success', t('admin.planCreated'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'plans'] })
      closeModal()
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: PlanForm }) =>
      adminApi.updatePlan(id, data),
    onSuccess: () => {
      showToast('success', t('admin.planUpdated'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'plans'] })
      closeModal()
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const deleteMutation = useMutation({
    mutationFn: adminApi.deletePlan,
    onSuccess: () => {
      showToast('success', t('admin.planDeleted'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'plans'] })
      setDeletingId(null)
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const toggleActiveMutation = useMutation({
    mutationFn: ({ id, isActive }: { id: string; isActive: boolean }) =>
      adminApi.updatePlan(id, { isActive }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'plans'] })
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const uploadMutation = useMutation({
    mutationFn: ({ id, file }: { id: string; file: File }) =>
      adminApi.uploadPlanPdf(id, file),
    onSuccess: () => {
      showToast('success', t('admin.pdfUploaded'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'plans'] })
      setUploadingPlanId(null)
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const openCreateModal = () => {
    setEditingPlan(null)
    reset({
      nameCs: '',
      nameEn: '',
      descriptionCs: '',
      descriptionEn: '',
      credits: 5,
      isActive: true,
    })
    setIsModalOpen(true)
  }

  const openEditModal = (plan: TrainingPlan) => {
    setEditingPlan(plan)
    reset({
      nameCs: plan.nameCs,
      nameEn: plan.nameEn || '',
      descriptionCs: plan.descriptionCs || '',
      descriptionEn: plan.descriptionEn || '',
      credits: plan.credits,
      isActive: plan.isActive,
    })
    setIsModalOpen(true)
  }

  const closeModal = () => {
    setIsModalOpen(false)
    setEditingPlan(null)
    reset()
  }

  const onSubmit = (data: PlanForm) => {
    if (editingPlan) {
      updateMutation.mutate({ id: editingPlan.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  const handleFileSelect = (planId: string) => {
    setUploadingPlanId(planId)
    fileInputRef.current?.click()
  }

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (file && uploadingPlanId) {
      uploadMutation.mutate({ id: uploadingPlanId, file })
    }
    event.target.value = ''
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('admin.plans')}
        </h1>
        <Button leftIcon={<Plus size={18} />} onClick={openCreateModal}>
          {t('admin.createPlan')}
        </Button>
      </div>

      {/* Hidden file input for PDF upload */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept=".pdf"
        className="hidden"
      />

      {isLoading ? (
        <div className="flex justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : plans && plans.length > 0 ? (
        <div className="space-y-4">
          {plans.map((plan) => (
            <Card key={plan.id} variant="bordered">
              <div className="flex items-start gap-4">
                <div className="flex-shrink-0 w-12 h-12 bg-primary-100 dark:bg-primary-900/30 rounded-lg flex items-center justify-center">
                  <Dumbbell size={24} className="text-primary-500" />
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="font-semibold text-neutral-900 dark:text-white">
                          {i18n.language === 'cs' ? plan.nameCs : plan.nameEn || plan.nameCs}
                        </h3>
                        {!plan.isActive && <Badge variant="warning">{t('admin.inactive')}</Badge>}
                        {plan.hasFile && (
                          <Badge variant="success">
                            <FileText size={12} className="mr-1" />
                            PDF
                          </Badge>
                        )}
                      </div>
                      <p className="text-sm text-neutral-500 dark:text-neutral-400 mt-1 line-clamp-2">
                        {i18n.language === 'cs'
                          ? plan.descriptionCs
                          : plan.descriptionEn || plan.descriptionCs}
                      </p>
                    </div>
                    <Badge variant="primary">{plan.credits} kr.</Badge>
                  </div>

                  <div className="flex items-center gap-2 mt-3 pt-3 border-t border-neutral-100 dark:border-dark-border">
                    <Button variant="ghost" size="sm" onClick={() => openEditModal(plan)}>
                      <Edit2 size={16} />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleFileSelect(plan.id)}
                      title={t('admin.uploadPdf')}
                    >
                      <Upload size={16} />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() =>
                        toggleActiveMutation.mutate({ id: plan.id, isActive: !plan.isActive })
                      }
                    >
                      {plan.isActive ? <EyeOff size={16} /> : <Eye size={16} />}
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setDeletingId(plan.id)}
                      className="text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20"
                    >
                      <Trash2 size={16} />
                    </Button>
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <Card variant="bordered">
          <div className="text-center py-12">
            <Dumbbell className="mx-auto mb-4 text-neutral-300 dark:text-neutral-600" size={48} />
            <p className="text-neutral-500 dark:text-neutral-400 mb-4">{t('admin.noPlans')}</p>
            <Button onClick={openCreateModal}>{t('admin.createPlan')}</Button>
          </div>
        </Card>
      )}

      {/* Create/Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={editingPlan ? t('admin.editPlan') : t('admin.createPlan')}
        size="lg"
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label={t('admin.nameCz')}
              {...register('nameCs')}
              error={errors.nameCs?.message && t('errors.required')}
            />
            <Input label={t('admin.nameEn')} {...register('nameEn')} />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              {t('admin.descriptionCz')}
            </label>
            <textarea
              {...register('descriptionCs')}
              rows={3}
              className="w-full px-3 py-2 rounded-lg border border-neutral-200 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              {t('admin.descriptionEn')}
            </label>
            <textarea
              {...register('descriptionEn')}
              rows={3}
              className="w-full px-3 py-2 rounded-lg border border-neutral-200 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
            />
          </div>

          <Input
            label={t('admin.priceCredits')}
            type="number"
            {...register('credits')}
            error={errors.credits?.message}
          />

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="isActive"
              {...register('isActive')}
              className="w-4 h-4 rounded border-neutral-300 text-primary-500 focus:ring-primary-500"
            />
            <label
              htmlFor="isActive"
              className="text-sm text-neutral-700 dark:text-neutral-300"
            >
              {t('admin.activeVisibleToClients')}
            </label>
          </div>

          <div className="flex gap-3 pt-2">
            <Button type="button" variant="secondary" className="flex-1" onClick={closeModal}>
              {t('common.cancel')}
            </Button>
            <Button
              type="submit"
              className="flex-1"
              isLoading={createMutation.isPending || updateMutation.isPending}
            >
              {t('common.save')}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete confirmation */}
      <Modal
        isOpen={!!deletingId}
        onClose={() => setDeletingId(null)}
        title={t('admin.deletePlan')}
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-neutral-600 dark:text-neutral-300">
            {t('admin.confirmDeletePlan')}
          </p>
          <div className="flex gap-3">
            <Button variant="secondary" className="flex-1" onClick={() => setDeletingId(null)}>
              {t('common.cancel')}
            </Button>
            <Button
              variant="danger"
              className="flex-1"
              onClick={() => deletingId && deleteMutation.mutate(deletingId)}
              isLoading={deleteMutation.isPending}
            >
              {t('common.delete')}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
