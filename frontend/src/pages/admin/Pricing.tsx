import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Plus, CreditCard, Edit2, Trash2, Star, TrendingUp, Percent, GripVertical } from 'lucide-react'
import { Card, Button, Input, Modal, Badge, Spinner } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi } from '@/services/api'
import { formatCurrency } from '@/utils/formatters'
import type { CreditPackage, PackageHighlight } from '@/types/api'

const packageSchema = z.object({
  nameCs: z.string().min(1),
  nameEn: z.string().optional(),
  description: z.string().optional(),
  credits: z.coerce.number().min(1),
  priceCzk: z.coerce.number().min(1),
  isActive: z.boolean().default(true),
  highlightType: z.enum(['NONE', 'BEST_SELLER', 'BEST_VALUE']).default('NONE'),
  isBasic: z.boolean().default(false),
})

type PackageForm = z.infer<typeof packageSchema>

const highlightKeys: Record<PackageHighlight, string> = {
  NONE: 'highlightNone',
  BEST_SELLER: 'highlightBestSeller',
  BEST_VALUE: 'highlightBestValue',
}

const highlightColors: Record<PackageHighlight, string> = {
  NONE: '',
  BEST_SELLER: 'ring-2 ring-amber-400 dark:ring-amber-500',
  BEST_VALUE: 'ring-2 ring-green-400 dark:ring-green-500',
}

export default function Pricing() {
  const { t, i18n } = useTranslation()
  const { showToast } = useToast()
  const queryClient = useQueryClient()

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingPackage, setEditingPackage] = useState<CreditPackage | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [draggedId, setDraggedId] = useState<string | null>(null)
  const [dragOverId, setDragOverId] = useState<string | null>(null)

  const { data: packages, isLoading } = useQuery({
    queryKey: ['adminCreditPackages'],
    queryFn: adminApi.getAllPackages,
  })

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<PackageForm>({
    resolver: zodResolver(packageSchema),
    defaultValues: {
      credits: 1,
      priceCzk: 500,
      isActive: true,
      highlightType: 'NONE',
      isBasic: false,
    },
  })

  const createMutation = useMutation({
    mutationFn: adminApi.createCreditPackage,
    onSuccess: () => {
      showToast('success', t('admin.pricing.packageCreated'))
      queryClient.invalidateQueries({ queryKey: ['adminCreditPackages'] })
      closeModal()
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<PackageForm & { sortOrder?: number }> }) =>
      adminApi.updateCreditPackage(id, data),
    onSuccess: () => {
      showToast('success', t('admin.pricing.packageUpdated'))
      queryClient.invalidateQueries({ queryKey: ['adminCreditPackages'] })
      closeModal()
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const deleteMutation = useMutation({
    mutationFn: adminApi.deleteCreditPackage,
    onSuccess: () => {
      showToast('success', t('admin.pricing.packageDeleted'))
      queryClient.invalidateQueries({ queryKey: ['adminCreditPackages'] })
      setDeletingId(null)
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const setBasicMutation = useMutation({
    mutationFn: (id: string) => adminApi.updateCreditPackage(id, { isBasic: true }),
    onSuccess: () => {
      showToast('success', t('admin.pricing.basicPackageSet'))
      queryClient.invalidateQueries({ queryKey: ['adminCreditPackages'] })
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const reorderMutation = useMutation({
    mutationFn: async ({ draggedPkgId, targetPkgId }: { draggedPkgId: string; targetPkgId: string }) => {
      if (!packages) return

      const draggedIndex = packages.findIndex(p => p.id === draggedPkgId)
      const targetIndex = packages.findIndex(p => p.id === targetPkgId)

      if (draggedIndex === -1 || targetIndex === -1) return

      // Create new order
      const newPackages = [...packages]
      const [draggedPkg] = newPackages.splice(draggedIndex, 1)
      newPackages.splice(targetIndex, 0, draggedPkg)

      // Update sortOrder for all affected packages
      const updates = newPackages.map((pkg, index) =>
        adminApi.updateCreditPackage(pkg.id, { sortOrder: index })
      )

      await Promise.all(updates)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminCreditPackages'] })
    },
    onError: () => {
      showToast('error', t('admin.pricing.reorderError'))
    },
  })

  const openCreateModal = () => {
    setEditingPackage(null)
    reset({
      nameCs: '',
      nameEn: '',
      description: '',
      credits: 1,
      priceCzk: 500,
      isActive: true,
      highlightType: 'NONE',
      isBasic: false,
    })
    setIsModalOpen(true)
  }

  const openEditModal = (pkg: CreditPackage) => {
    setEditingPackage(pkg)
    reset({
      nameCs: pkg.nameCs,
      nameEn: pkg.nameEn || '',
      description: pkg.description || '',
      credits: pkg.credits,
      priceCzk: pkg.priceCzk,
      isActive: pkg.isActive,
      highlightType: pkg.highlightType,
      isBasic: pkg.isBasic,
    })
    setIsModalOpen(true)
  }

  const closeModal = () => {
    setIsModalOpen(false)
    setEditingPackage(null)
    reset()
  }

  const onSubmit = (data: PackageForm) => {
    if (editingPackage) {
      updateMutation.mutate({ id: editingPackage.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  const handleSetBasic = (pkgId: string) => {
    setBasicMutation.mutate(pkgId)
  }

  // Drag and drop handlers
  const handleDragStart = (e: React.DragEvent, pkgId: string) => {
    setDraggedId(pkgId)
    e.dataTransfer.effectAllowed = 'move'
  }

  const handleDragOver = (e: React.DragEvent, pkgId: string) => {
    e.preventDefault()
    if (draggedId && draggedId !== pkgId) {
      setDragOverId(pkgId)
    }
  }

  const handleDragLeave = () => {
    setDragOverId(null)
  }

  const handleDrop = (e: React.DragEvent, targetPkgId: string) => {
    e.preventDefault()
    if (draggedId && draggedId !== targetPkgId) {
      reorderMutation.mutate({ draggedPkgId: draggedId, targetPkgId })
    }
    setDraggedId(null)
    setDragOverId(null)
  }

  const handleDragEnd = () => {
    setDraggedId(null)
    setDragOverId(null)
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('admin.pricing')}
        </h1>
        <Button leftIcon={<Plus size={18} />} onClick={openCreateModal}>
          {t('admin.pricing.newPackage')}
        </Button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : packages && packages.length > 0 ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {packages.map((pkg) => {
            const pricePerCredit = pkg.credits > 0 ? pkg.priceCzk / pkg.credits : 0
            const isDragging = draggedId === pkg.id
            const isDragOver = dragOverId === pkg.id

            return (
              <Card
                key={pkg.id}
                variant="bordered"
                draggable
                onDragStart={(e) => handleDragStart(e, pkg.id)}
                onDragOver={(e) => handleDragOver(e, pkg.id)}
                onDragLeave={handleDragLeave}
                onDrop={(e) => handleDrop(e, pkg.id)}
                onDragEnd={handleDragEnd}
                className={`
                  ${!pkg.isActive ? 'opacity-60' : ''}
                  ${highlightColors[pkg.highlightType]}
                  relative cursor-grab active:cursor-grabbing
                  transition-all duration-200
                  ${isDragging ? 'opacity-50 scale-95' : ''}
                  ${isDragOver ? 'ring-2 ring-primary-500 ring-offset-2' : ''}
                `}
              >
                <div className="text-center">
                  {/* Drag handle + actions row */}
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <GripVertical size={16} className="text-neutral-400 dark:text-neutral-500" />
                      {!pkg.isActive && <Badge variant="warning">{t('admin.pricing.inactive', 'Inactive')}</Badge>}
                      {pkg.isBasic && <Badge variant="info">{t('admin.pricing.basic')}</Badge>}
                    </div>
                    <div className="flex items-center gap-1">
                      <Button variant="ghost" size="sm" onClick={() => openEditModal(pkg)}>
                        <Edit2 size={14} />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setDeletingId(pkg.id)}
                        className="text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20"
                      >
                        <Trash2 size={14} />
                      </Button>
                    </div>
                  </div>

                  {/* Highlight badge - inside card */}
                  {pkg.highlightType !== 'NONE' && (
                    <div className="mb-3">
                      <Badge
                        variant={pkg.highlightType === 'BEST_SELLER' ? 'warning' : 'success'}
                        className="whitespace-nowrap"
                      >
                        {pkg.highlightType === 'BEST_SELLER' ? (
                          <span className="flex items-center gap-1">
                            <Star size={12} /> {t('admin.pricing.highlightBestSeller')}
                          </span>
                        ) : (
                          <span className="flex items-center gap-1">
                            <TrendingUp size={12} /> {t('admin.pricing.highlightBestValue')}
                          </span>
                        )}
                      </Badge>
                    </div>
                  )}

                  <div className="w-16 h-16 mx-auto mb-4 bg-primary-100 dark:bg-primary-900/30 rounded-full flex items-center justify-center">
                    <CreditCard size={28} className="text-primary-500" />
                  </div>

                  <p className="text-3xl font-heading font-bold text-neutral-900 dark:text-white">
                    {pkg.credits}
                  </p>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">
                    {i18n.language === 'cs' ? pkg.nameCs : pkg.nameEn || pkg.nameCs}
                  </p>

                  {/* Discount percentage */}
                  {pkg.discountPercent && pkg.discountPercent > 0 && (
                    <div className="flex items-center justify-center gap-1 mt-2">
                      <Percent size={14} className="text-green-500" />
                      <span className="text-sm font-medium text-green-600 dark:text-green-400">
                        -{pkg.discountPercent}% {t('admin.pricing.vsBasic')}
                      </span>
                    </div>
                  )}

                  <div className="mt-4 pt-4 border-t border-neutral-100 dark:border-dark-border">
                    <p className="text-2xl font-bold text-neutral-900 dark:text-white">
                      {formatCurrency(pkg.priceCzk)}
                    </p>
                    <p className="text-sm text-neutral-500 dark:text-neutral-400">
                      {formatCurrency(Math.round(pricePerCredit))}{t('admin.pricing.perTraining')}
                    </p>
                  </div>

                  {/* Set as basic button */}
                  {!pkg.isBasic && (
                    <Button
                      variant="ghost"
                      size="sm"
                      className="mt-3 text-xs"
                      onClick={() => handleSetBasic(pkg.id)}
                      disabled={setBasicMutation.isPending}
                    >
                      {t('admin.pricing.setAsBasic')}
                    </Button>
                  )}
                </div>
              </Card>
            )
          })}
        </div>
      ) : (
        <Card variant="bordered">
          <div className="text-center py-12">
            <CreditCard className="mx-auto mb-4 text-neutral-300 dark:text-neutral-600" size={48} />
            <p className="text-neutral-500 dark:text-neutral-400 mb-4">{t('admin.pricing.noPackages')}</p>
            <Button onClick={openCreateModal}>{t('admin.pricing.createPackage')}</Button>
          </div>
        </Card>
      )}

      {/* Create/Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={editingPackage ? t('admin.pricing.editPackage') : t('admin.pricing.newPackage')}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label={t('admin.pricing.nameCz')}
              {...register('nameCs')}
              error={errors.nameCs?.message && t('errors.required')}
            />
            <Input label={t('admin.pricing.nameEn')} {...register('nameEn')} />
          </div>

          <Input
            label={t('admin.pricing.description')}
            {...register('description')}
          />

          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label={t('admin.pricing.credits')}
              type="number"
              {...register('credits')}
              error={errors.credits?.message}
            />
            <Input
              label={t('admin.pricing.priceCzk')}
              type="number"
              {...register('priceCzk')}
              error={errors.priceCzk?.message}
            />
          </div>

          {/* Highlight Type */}
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">
              {t('admin.pricing.highlight')}
            </label>
            <Controller
              name="highlightType"
              control={control}
              render={({ field }) => (
                <div className="flex flex-wrap gap-2">
                  {(['NONE', 'BEST_SELLER', 'BEST_VALUE'] as PackageHighlight[]).map((type) => (
                    <button
                      key={type}
                      type="button"
                      onClick={() => field.onChange(type)}
                      className={`px-3 py-2 text-sm rounded-lg border transition-colors ${
                        field.value === type
                          ? type === 'BEST_SELLER'
                            ? 'bg-amber-100 border-amber-400 text-amber-800 dark:bg-amber-900/30 dark:border-amber-500 dark:text-amber-300'
                            : type === 'BEST_VALUE'
                              ? 'bg-green-100 border-green-400 text-green-800 dark:bg-green-900/30 dark:border-green-500 dark:text-green-300'
                              : 'bg-neutral-100 border-neutral-400 text-neutral-800 dark:bg-neutral-800 dark:border-neutral-500 dark:text-neutral-300'
                          : 'bg-white border-neutral-200 text-neutral-600 hover:bg-neutral-50 dark:bg-dark-card dark:border-dark-border dark:text-neutral-400 dark:hover:bg-dark-hover'
                      }`}
                    >
                      {t(`admin.pricing.${highlightKeys[type]}`)}
                    </button>
                  ))}
                </div>
              )}
            />
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="isActive"
                {...register('isActive')}
                className="w-4 h-4 rounded border-neutral-300 text-primary-500 focus:ring-primary-500"
              />
              <label htmlFor="isActive" className="text-sm text-neutral-700 dark:text-neutral-300">
                {t('admin.pricing.activeVisible')}
              </label>
            </div>

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="isBasic"
                {...register('isBasic')}
                className="w-4 h-4 rounded border-neutral-300 text-primary-500 focus:ring-primary-500"
              />
              <label htmlFor="isBasic" className="text-sm text-neutral-700 dark:text-neutral-300">
                {t('admin.pricing.basicPackageForDiscount')}
              </label>
            </div>
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
        title={t('admin.pricing.deletePackage')}
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-neutral-600 dark:text-neutral-300">
            {t('admin.pricing.deletePackageConfirm')}
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
