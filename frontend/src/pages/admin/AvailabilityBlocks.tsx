import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Plus, Clock, Trash2, Edit2 } from 'lucide-react'
import { Card, Button, Input, Modal, Spinner } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi } from '@/services/api'
import type { AvailabilityBlock } from '@/types/api'

const blockSchema = z.object({
  name: z.string().optional(),
  daysOfWeek: z.array(z.number().min(1).max(7)).min(1),
  startTime: z.string().regex(/^\d{2}:\d{2}$/),
  endTime: z.string().regex(/^\d{2}:\d{2}$/),
  slotDurationMinutes: z.coerce.number().min(15).max(120),
})

type BlockForm = z.infer<typeof blockSchema>

const DAYS = [
  { value: 1, label: 'Pondělí' },
  { value: 2, label: 'Úterý' },
  { value: 3, label: 'Středa' },
  { value: 4, label: 'Čtvrtek' },
  { value: 5, label: 'Pátek' },
  { value: 6, label: 'Sobota' },
  { value: 7, label: 'Neděle' },
]

export default function AvailabilityBlocks() {
  const { t } = useTranslation()
  const { showToast } = useToast()
  const queryClient = useQueryClient()

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingBlock, setEditingBlock] = useState<AvailabilityBlock | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const { data: blocks, isLoading } = useQuery({
    queryKey: ['admin', 'availability'],
    queryFn: adminApi.getBlocks,
  })

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<BlockForm>({
    resolver: zodResolver(blockSchema),
    defaultValues: {
      daysOfWeek: [1],
      startTime: '09:00',
      endTime: '17:00',
      slotDurationMinutes: 60,
    },
  })

  const selectedDays = watch('daysOfWeek')

  const createMutation = useMutation({
    mutationFn: adminApi.createBlock,
    onSuccess: () => {
      showToast('success', 'Blok byl vytvořen')
      queryClient.invalidateQueries({ queryKey: ['admin', 'availability'] })
      closeModal()
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<BlockForm> }) =>
      adminApi.updateBlock(id, data),
    onSuccess: () => {
      showToast('success', 'Blok byl upraven')
      queryClient.invalidateQueries({ queryKey: ['admin', 'availability'] })
      closeModal()
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const deleteMutation = useMutation({
    mutationFn: adminApi.deleteBlock,
    onSuccess: () => {
      showToast('success', 'Blok byl smazán')
      queryClient.invalidateQueries({ queryKey: ['admin', 'availability'] })
      setDeletingId(null)
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const openCreateModal = () => {
    setEditingBlock(null)
    reset({
      name: '',
      daysOfWeek: [1],
      startTime: '09:00',
      endTime: '17:00',
      slotDurationMinutes: 60,
    })
    setIsModalOpen(true)
  }

  const openEditModal = (block: AvailabilityBlock) => {
    setEditingBlock(block)
    reset({
      name: block.name || '',
      daysOfWeek: block.daysOfWeek,
      startTime: block.startTime.substring(0, 5),
      endTime: block.endTime.substring(0, 5),
      slotDurationMinutes: block.slotDurationMinutes,
    })
    setIsModalOpen(true)
  }

  const closeModal = () => {
    setIsModalOpen(false)
    setEditingBlock(null)
    reset()
  }

  const toggleDay = (day: number) => {
    const current = selectedDays || []
    if (current.includes(day)) {
      setValue('daysOfWeek', current.filter((d) => d !== day))
    } else {
      setValue('daysOfWeek', [...current, day].sort())
    }
  }

  const onSubmit = (data: BlockForm) => {
    if (editingBlock) {
      updateMutation.mutate({ id: editingBlock.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  const getDayLabels = (days: number[]) => {
    return days
      .sort()
      .map((d) => DAYS.find((day) => day.value === d)?.label.substring(0, 2))
      .join(', ')
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('admin.availability')}
        </h1>
        <Button leftIcon={<Plus size={18} />} onClick={openCreateModal}>
          {t('admin.createBlock')}
        </Button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : blocks && blocks.length > 0 ? (
        <div className="space-y-4">
          {blocks.map((block) => (
            <Card key={block.id} variant="bordered">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Clock size={18} className="text-primary-500" />
                  <div>
                    <p className="font-medium text-neutral-900 dark:text-white">
                      {block.name || 'Blok dostupnosti'}
                    </p>
                    <p className="font-mono text-sm text-neutral-700 dark:text-neutral-300">
                      {block.startTime.substring(0, 5)} - {block.endTime.substring(0, 5)}
                    </p>
                    <p className="text-sm text-neutral-500 dark:text-neutral-400">
                      {getDayLabels(block.daysOfWeek)} · {block.slotDurationMinutes} min
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <Button variant="ghost" size="sm" onClick={() => openEditModal(block)}>
                    <Edit2 size={16} />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setDeletingId(block.id)}
                    className="text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20"
                  >
                    <Trash2 size={16} />
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <Card variant="bordered">
          <div className="text-center py-12">
            <Clock className="mx-auto mb-4 text-neutral-300 dark:text-neutral-600" size={48} />
            <p className="text-neutral-500 dark:text-neutral-400 mb-4">
              Žádné bloky dostupnosti
            </p>
            <Button onClick={openCreateModal}>{t('admin.createBlock')}</Button>
          </div>
        </Card>
      )}

      {/* Create/Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={editingBlock ? t('admin.editBlock') : t('admin.createBlock')}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input label="Název (volitelně)" {...register('name')} />

          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">
              Dny v týdnu
            </label>
            <div className="flex flex-wrap gap-2">
              {DAYS.map((day) => (
                <button
                  key={day.value}
                  type="button"
                  onClick={() => toggleDay(day.value)}
                  className={`px-3 py-1.5 text-sm rounded-lg border transition-colors ${
                    selectedDays?.includes(day.value)
                      ? 'bg-primary-500 border-primary-500 text-white'
                      : 'border-neutral-200 dark:border-dark-border text-neutral-700 dark:text-neutral-300 hover:border-primary-300'
                  }`}
                >
                  {day.label}
                </button>
              ))}
            </div>
            {errors.daysOfWeek && (
              <p className="text-sm text-red-500 mt-1">Vyberte alespoň jeden den</p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Od"
              type="time"
              {...register('startTime')}
              error={errors.startTime?.message}
            />
            <Input
              label="Do"
              type="time"
              {...register('endTime')}
              error={errors.endTime?.message}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              Délka slotu
            </label>
            <select
              {...register('slotDurationMinutes')}
              className="w-full px-3 py-2 rounded-lg border border-neutral-200 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value={30}>30 minut</option>
              <option value={45}>45 minut</option>
              <option value={60}>60 minut</option>
              <option value={90}>90 minut</option>
            </select>
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
        title="Smazat blok?"
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-neutral-600 dark:text-neutral-300">
            Opravdu chcete smazat tento blok dostupnosti?
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
