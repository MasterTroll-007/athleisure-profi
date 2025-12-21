import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Plus, CreditCard, Edit2, Trash2, Gift } from 'lucide-react'
import { Card, Button, Input, Modal, Badge, Spinner } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi, creditApi } from '@/services/api'
import { formatCurrency } from '@/utils/formatters'
import type { CreditPackage } from '@/types/api'

const packageSchema = z.object({
  nameCs: z.string().min(1),
  nameEn: z.string().optional(),
  credits: z.coerce.number().min(1),
  bonusCredits: z.coerce.number().min(0).default(0),
  priceCzk: z.coerce.number().min(1),
  isActive: z.boolean().default(true),
})

type PackageForm = z.infer<typeof packageSchema>

export default function Pricing() {
  const { t, i18n } = useTranslation()
  const { showToast } = useToast()
  const queryClient = useQueryClient()

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingPackage, setEditingPackage] = useState<CreditPackage | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const { data: packages, isLoading } = useQuery({
    queryKey: ['creditPackages'],
    queryFn: creditApi.getPackages,
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PackageForm>({
    resolver: zodResolver(packageSchema),
    defaultValues: {
      credits: 1,
      bonusCredits: 0,
      priceCzk: 500,
      isActive: true,
    },
  })

  const createMutation = useMutation({
    mutationFn: adminApi.createCreditPackage,
    onSuccess: () => {
      showToast('success', 'Balíček byl vytvořen')
      queryClient.invalidateQueries({ queryKey: ['creditPackages'] })
      closeModal()
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: PackageForm }) =>
      adminApi.updateCreditPackage(id, data),
    onSuccess: () => {
      showToast('success', 'Balíček byl upraven')
      queryClient.invalidateQueries({ queryKey: ['creditPackages'] })
      closeModal()
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const deleteMutation = useMutation({
    mutationFn: adminApi.deleteCreditPackage,
    onSuccess: () => {
      showToast('success', 'Balíček byl smazán')
      queryClient.invalidateQueries({ queryKey: ['creditPackages'] })
      setDeletingId(null)
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const openCreateModal = () => {
    setEditingPackage(null)
    reset({
      nameCs: '',
      nameEn: '',
      credits: 1,
      bonusCredits: 0,
      priceCzk: 500,
      isActive: true,
    })
    setIsModalOpen(true)
  }

  const openEditModal = (pkg: CreditPackage) => {
    setEditingPackage(pkg)
    reset({
      nameCs: pkg.nameCs,
      nameEn: pkg.nameEn || '',
      credits: pkg.credits,
      bonusCredits: pkg.bonusCredits,
      priceCzk: pkg.priceCzk,
      isActive: pkg.isActive,
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

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('admin.pricing')}
        </h1>
        <Button leftIcon={<Plus size={18} />} onClick={openCreateModal}>
          Nový balíček
        </Button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : packages && packages.length > 0 ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {packages.map((pkg) => {
            const totalCredits = pkg.credits + pkg.bonusCredits
            const pricePerCredit = pkg.priceCzk / totalCredits

            return (
              <Card key={pkg.id} variant="bordered" className={!pkg.isActive ? 'opacity-60' : ''}>
                <div className="text-center">
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-2">
                      {!pkg.isActive && <Badge variant="warning">Neaktivní</Badge>}
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

                  <div className="w-16 h-16 mx-auto mb-4 bg-primary-100 dark:bg-primary-900/30 rounded-full flex items-center justify-center">
                    <CreditCard size={28} className="text-primary-500" />
                  </div>

                  <p className="text-3xl font-heading font-bold text-neutral-900 dark:text-white">
                    {pkg.credits}
                  </p>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">
                    {i18n.language === 'cs' ? pkg.nameCs : pkg.nameEn || pkg.nameCs}
                  </p>

                  {pkg.bonusCredits > 0 && (
                    <div className="flex items-center justify-center gap-1 mt-2">
                      <Gift size={14} className="text-green-500" />
                      <span className="text-sm font-medium text-green-600 dark:text-green-400">
                        +{pkg.bonusCredits} bonus
                      </span>
                    </div>
                  )}

                  <div className="mt-4 pt-4 border-t border-neutral-100 dark:border-dark-border">
                    <p className="text-2xl font-bold text-neutral-900 dark:text-white">
                      {formatCurrency(pkg.priceCzk)}
                    </p>
                    <p className="text-sm text-neutral-500 dark:text-neutral-400">
                      {formatCurrency(Math.round(pricePerCredit))}/kredit
                    </p>
                  </div>
                </div>
              </Card>
            )
          })}
        </div>
      ) : (
        <Card variant="bordered">
          <div className="text-center py-12">
            <CreditCard className="mx-auto mb-4 text-neutral-300 dark:text-neutral-600" size={48} />
            <p className="text-neutral-500 dark:text-neutral-400 mb-4">Žádné balíčky</p>
            <Button onClick={openCreateModal}>Vytvořit balíček</Button>
          </div>
        </Card>
      )}

      {/* Create/Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={editingPackage ? 'Upravit balíček' : 'Nový balíček'}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label="Název (CZ)"
              {...register('nameCs')}
              error={errors.nameCs?.message && t('errors.required')}
            />
            <Input label="Název (EN)" {...register('nameEn')} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label="Kredity"
              type="number"
              {...register('credits')}
              error={errors.credits?.message}
            />
            <Input
              label="Bonus kredity"
              type="number"
              {...register('bonusCredits')}
              error={errors.bonusCredits?.message}
            />
          </div>

          <Input
            label="Cena (Kč)"
            type="number"
            {...register('priceCzk')}
            error={errors.priceCzk?.message}
          />

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="isActive"
              {...register('isActive')}
              className="w-4 h-4 rounded border-neutral-300 text-primary-500 focus:ring-primary-500"
            />
            <label htmlFor="isActive" className="text-sm text-neutral-700 dark:text-neutral-300">
              Aktivní (viditelný pro klientky)
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
        title="Smazat balíček?"
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-neutral-600 dark:text-neutral-300">
            Opravdu chcete smazat tento kreditový balíček?
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
