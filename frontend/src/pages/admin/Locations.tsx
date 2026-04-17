import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Plus, Edit2, Trash2, MapPin } from 'lucide-react'
import { Card, Button, Input, Modal, Badge, Spinner } from '@/components/ui'
import ColorPicker, { PRESET_COLORS } from '@/components/ui/ColorPicker'
import { useToast } from '@/components/ui/Toast'
import { locationsApi } from '@/services/api'
import type { TrainingLocation } from '@/types/api'

const HEX_PATTERN = /^#[0-9A-Fa-f]{6}$/

const locationSchema = z.object({
  nameCs: z.string().trim().min(1).max(100),
  nameEn: z.string().trim().max(100).optional(),
  addressCs: z.string().trim().max(500).optional(),
  addressEn: z.string().trim().max(500).optional(),
  color: z.string().regex(HEX_PATTERN, 'Barva musí být ve formátu #RRGGBB'),
  isActive: z.boolean().default(true),
})

type LocationForm = z.infer<typeof locationSchema>

export default function Locations() {
  const { t } = useTranslation()
  const { showToast } = useToast()
  const queryClient = useQueryClient()

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<TrainingLocation | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const { data: locations, isLoading } = useQuery({
    queryKey: ['admin', 'locations'],
    queryFn: locationsApi.listAdmin,
  })

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<LocationForm>({
    resolver: zodResolver(locationSchema),
    defaultValues: {
      nameCs: '',
      nameEn: '',
      addressCs: '',
      addressEn: '',
      color: PRESET_COLORS[0],
      isActive: true,
    },
  })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['admin', 'locations'] })
    queryClient.invalidateQueries({ queryKey: ['locations'] })
    queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
    queryClient.invalidateQueries({ queryKey: ['admin', 'templates'] })
    queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
  }

  const createMutation = useMutation({
    mutationFn: locationsApi.create,
    onSuccess: () => {
      showToast('success', t('admin.locations.created'))
      invalidate()
      closeModal()
    },
    onError: () => showToast('error', t('errors.somethingWrong')),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<LocationForm> }) =>
      locationsApi.update(id, data),
    onSuccess: () => {
      showToast('success', t('admin.locations.updated'))
      invalidate()
      closeModal()
    },
    onError: () => showToast('error', t('errors.somethingWrong')),
  })

  const deleteMutation = useMutation({
    mutationFn: locationsApi.remove,
    onSuccess: () => {
      showToast('success', t('admin.locations.deleted'))
      invalidate()
      setDeletingId(null)
    },
    onError: () => showToast('error', t('errors.somethingWrong')),
  })

  const openCreateModal = () => {
    setEditingItem(null)
    reset({
      nameCs: '',
      nameEn: '',
      addressCs: '',
      addressEn: '',
      color: PRESET_COLORS[0],
      isActive: true,
    })
    setIsModalOpen(true)
  }

  const openEditModal = (item: TrainingLocation) => {
    setEditingItem(item)
    reset({
      nameCs: item.nameCs,
      nameEn: item.nameEn || '',
      addressCs: item.addressCs || '',
      addressEn: item.addressEn || '',
      color: item.color,
      isActive: item.isActive,
    })
    setIsModalOpen(true)
  }

  const closeModal = () => {
    setIsModalOpen(false)
    setEditingItem(null)
    reset()
  }

  const onSubmit = (data: LocationForm) => {
    if (editingItem) {
      updateMutation.mutate({ id: editingItem.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('admin.locations.title')}
        </h1>
        <Button leftIcon={<Plus size={18} />} onClick={openCreateModal}>
          {t('admin.locations.new')}
        </Button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Spinner size="lg" /></div>
      ) : locations && locations.length > 0 ? (
        <div className="space-y-3">
          {locations.map((loc) => (
            <Card
              key={loc.id}
              variant="bordered"
              className={!loc.isActive ? 'opacity-60' : ''}
            >
              <div className="flex items-center gap-4">
                <div
                  className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0"
                  style={{ backgroundColor: loc.color }}
                >
                  <MapPin size={20} className="text-white" />
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-medium text-neutral-900 dark:text-white truncate">
                      {loc.nameCs}
                    </p>
                    {!loc.isActive && <Badge variant="warning">{t('admin.pricing.inactive')}</Badge>}
                  </div>
                  {loc.addressCs && (
                    <p className="text-sm text-neutral-500 dark:text-neutral-400 truncate">
                      {loc.addressCs}
                    </p>
                  )}
                </div>

                <div className="flex items-center gap-1 flex-shrink-0">
                  <Button variant="ghost" size="sm" onClick={() => openEditModal(loc)}>
                    <Edit2 size={14} />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setDeletingId(loc.id)}
                    className="text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20"
                  >
                    <Trash2 size={14} />
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <Card variant="bordered">
          <div className="text-center py-12">
            <MapPin className="mx-auto mb-4 text-neutral-300 dark:text-neutral-600" size={48} />
            <p className="text-neutral-500 dark:text-neutral-400 mb-4">
              {t('admin.locations.empty')}
            </p>
            <Button onClick={openCreateModal}>{t('admin.locations.new')}</Button>
          </div>
        </Card>
      )}

      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={editingItem ? t('admin.locations.edit') : t('admin.locations.new')}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label={t('admin.locations.nameCs')}
              {...register('nameCs')}
              error={errors.nameCs?.message && t('errors.required')}
            />
            <Input label={t('admin.locations.nameEn')} {...register('nameEn')} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Input label={t('admin.locations.addressCs')} {...register('addressCs')} />
            <Input label={t('admin.locations.addressEn')} {...register('addressEn')} />
          </div>

          <Controller
            control={control}
            name="color"
            render={({ field, fieldState }) => (
              <ColorPicker
                label={t('admin.locations.color')}
                value={field.value}
                onChange={field.onChange}
                error={fieldState.error?.message}
              />
            )}
          />

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

      <Modal
        isOpen={!!deletingId}
        onClose={() => setDeletingId(null)}
        title={t('admin.locations.delete')}
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-neutral-600 dark:text-neutral-300">
            {t('admin.locations.deleteConfirm')}
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
