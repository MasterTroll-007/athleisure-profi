import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Plus, Edit2, Trash2, Dumbbell, GripVertical } from 'lucide-react'
import { Card, Button, Input, Modal, Badge, Spinner } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi } from '@/services/api'
import type { PricingItem } from '@/types/api'

const pricingSchema = z.object({
  nameCs: z.string().min(1),
  nameEn: z.string().optional(),
  descriptionCs: z.string().optional(),
  descriptionEn: z.string().optional(),
  credits: z.coerce.number().min(1),
  durationMinutes: z.coerce.number().min(1).optional(),
  isActive: z.boolean().default(true),
})

type PricingForm = z.infer<typeof pricingSchema>

export default function TrainingPricing() {
  const { t, i18n } = useTranslation()
  const { showToast } = useToast()
  const queryClient = useQueryClient()

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<PricingItem | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [draggedId, setDraggedId] = useState<string | null>(null)
  const [dragOverId, setDragOverId] = useState<string | null>(null)

  const { data: items, isLoading } = useQuery({
    queryKey: ['adminPricingItems'],
    queryFn: adminApi.getAllPricing,
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PricingForm>({
    resolver: zodResolver(pricingSchema),
    defaultValues: {
      credits: 1,
      durationMinutes: 60,
      isActive: true,
    },
  })

  const createMutation = useMutation({
    mutationFn: adminApi.createPricing,
    onSuccess: () => {
      showToast('success', t('admin.trainingPricing.created'))
      queryClient.invalidateQueries({ queryKey: ['adminPricingItems'] })
      closeModal()
    },
    onError: () => showToast('error', t('errors.somethingWrong')),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<PricingForm & { sortOrder?: number }> }) =>
      adminApi.updatePricing(id, data),
    onSuccess: () => {
      showToast('success', t('admin.trainingPricing.updated'))
      queryClient.invalidateQueries({ queryKey: ['adminPricingItems'] })
      closeModal()
    },
    onError: () => showToast('error', t('errors.somethingWrong')),
  })

  const deleteMutation = useMutation({
    mutationFn: adminApi.deletePricing,
    onSuccess: () => {
      showToast('success', t('admin.trainingPricing.deleted'))
      queryClient.invalidateQueries({ queryKey: ['adminPricingItems'] })
      setDeletingId(null)
    },
    onError: () => showToast('error', t('errors.somethingWrong')),
  })

  const reorderMutation = useMutation({
    mutationFn: async ({ draggedItemId, targetItemId }: { draggedItemId: string; targetItemId: string }) => {
      if (!items) return
      const draggedIndex = items.findIndex(i => i.id === draggedItemId)
      const targetIndex = items.findIndex(i => i.id === targetItemId)
      if (draggedIndex === -1 || targetIndex === -1) return
      const newItems = [...items]
      const [dragged] = newItems.splice(draggedIndex, 1)
      newItems.splice(targetIndex, 0, dragged)
      await Promise.all(newItems.map((item, index) =>
        adminApi.updatePricing(item.id, { sortOrder: index })
      ))
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['adminPricingItems'] }),
    onError: () => showToast('error', t('errors.somethingWrong')),
  })

  const openCreateModal = () => {
    setEditingItem(null)
    reset({ nameCs: '', nameEn: '', descriptionCs: '', descriptionEn: '', credits: 1, durationMinutes: 60, isActive: true })
    setIsModalOpen(true)
  }

  const openEditModal = (item: PricingItem) => {
    setEditingItem(item)
    reset({
      nameCs: item.nameCs,
      nameEn: item.nameEn || '',
      descriptionCs: item.descriptionCs || '',
      descriptionEn: item.descriptionEn || '',
      credits: item.credits,
      durationMinutes: (item as any).durationMinutes || 60,
      isActive: item.isActive,
    })
    setIsModalOpen(true)
  }

  const closeModal = () => {
    setIsModalOpen(false)
    setEditingItem(null)
    reset()
  }

  const onSubmit = (data: PricingForm) => {
    if (editingItem) {
      updateMutation.mutate({ id: editingItem.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  const handleDragStart = (e: React.DragEvent, id: string) => {
    setDraggedId(id)
    e.dataTransfer.effectAllowed = 'move'
  }
  const handleDragOver = (e: React.DragEvent, id: string) => {
    e.preventDefault()
    if (draggedId && draggedId !== id) setDragOverId(id)
  }
  const handleDrop = (e: React.DragEvent, targetId: string) => {
    e.preventDefault()
    if (draggedId && draggedId !== targetId) {
      reorderMutation.mutate({ draggedItemId: draggedId, targetItemId: targetId })
    }
    setDraggedId(null)
    setDragOverId(null)
  }
  const handleDragEnd = () => { setDraggedId(null); setDragOverId(null) }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('admin.trainingPricing.title')}
        </h1>
        <Button leftIcon={<Plus size={18} />} onClick={openCreateModal}>
          {t('admin.trainingPricing.new')}
        </Button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Spinner size="lg" /></div>
      ) : items && items.length > 0 ? (
        <div className="space-y-3">
          {items.map((item) => {
            const name = i18n.language === 'cs' ? item.nameCs : (item.nameEn || item.nameCs)
            const desc = i18n.language === 'cs' ? item.descriptionCs : (item.descriptionEn || item.descriptionCs)
            const isDragging = draggedId === item.id
            const isDragOver = dragOverId === item.id

            return (
              <Card
                key={item.id}
                variant="bordered"
                draggable
                onDragStart={(e) => handleDragStart(e, item.id)}
                onDragOver={(e) => handleDragOver(e, item.id)}
                onDragLeave={() => setDragOverId(null)}
                onDrop={(e) => handleDrop(e, item.id)}
                onDragEnd={handleDragEnd}
                className={`
                  ${!item.isActive ? 'opacity-60' : ''}
                  ${isDragging ? 'opacity-50 scale-95' : ''}
                  ${isDragOver ? 'ring-2 ring-primary-500 ring-offset-2' : ''}
                  cursor-grab active:cursor-grabbing transition-all duration-200
                `}
              >
                <div className="flex items-center gap-4">
                  <GripVertical size={16} className="text-neutral-400 dark:text-neutral-500 flex-shrink-0" />

                  <div className="w-12 h-12 bg-primary-100 dark:bg-primary-900/30 rounded-full flex items-center justify-center flex-shrink-0">
                    <Dumbbell size={20} className="text-primary-500" />
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="font-medium text-neutral-900 dark:text-white truncate">{name}</p>
                      {!item.isActive && <Badge variant="warning">{t('admin.pricing.inactive')}</Badge>}
                    </div>
                    {desc && (
                      <p className="text-sm text-neutral-500 dark:text-neutral-400 truncate">{desc}</p>
                    )}
                  </div>

                  <div className="text-right flex-shrink-0">
                    <p className="text-lg font-bold text-primary-600 dark:text-primary-400">
                      {item.credits} {item.credits === 1 ? t('admin.trainingPricing.credit') : t('admin.trainingPricing.credits')}
                    </p>
                    {(item as any).durationMinutes && (
                      <p className="text-sm text-neutral-500 dark:text-neutral-400">
                        {(item as any).durationMinutes} min
                      </p>
                    )}
                  </div>

                  <div className="flex items-center gap-1 flex-shrink-0">
                    <Button variant="ghost" size="sm" onClick={() => openEditModal(item)}>
                      <Edit2 size={14} />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setDeletingId(item.id)}
                      className="text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20"
                    >
                      <Trash2 size={14} />
                    </Button>
                  </div>
                </div>
              </Card>
            )
          })}
        </div>
      ) : (
        <Card variant="bordered">
          <div className="text-center py-12">
            <Dumbbell className="mx-auto mb-4 text-neutral-300 dark:text-neutral-600" size={48} />
            <p className="text-neutral-500 dark:text-neutral-400 mb-4">{t('admin.trainingPricing.empty')}</p>
            <Button onClick={openCreateModal}>{t('admin.trainingPricing.new')}</Button>
          </div>
        </Card>
      )}

      {/* Create/Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={editingItem ? t('admin.trainingPricing.edit') : t('admin.trainingPricing.new')}
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

          <div className="grid gap-4 sm:grid-cols-2">
            <Input label={t('admin.pricing.description') + ' (CZ)'} {...register('descriptionCs')} />
            <Input label={t('admin.pricing.description') + ' (EN)'} {...register('descriptionEn')} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label={t('admin.trainingPricing.creditsCost')}
              type="number"
              {...register('credits')}
              error={errors.credits?.message}
            />
            <Input
              label={t('admin.trainingPricing.duration')}
              type="number"
              {...register('durationMinutes')}
              error={errors.durationMinutes?.message}
            />
          </div>

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
        title={t('admin.trainingPricing.delete')}
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-neutral-600 dark:text-neutral-300">
            {t('admin.trainingPricing.deleteConfirm')}
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
