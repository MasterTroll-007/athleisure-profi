import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  ArrowLeft,
  User,
  Mail,
  Phone,
  CreditCard,
  Calendar,
  Plus,
  Trash2,
  UserCog,
} from 'lucide-react'
import { Card, Button, Input, Modal, Badge, Spinner } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi } from '@/services/api'
import { formatDate, formatTime } from '@/utils/formatters'
import type { ClientNote, Reservation, Trainer } from '@/types/api'

const noteSchema = z.object({
  note: z.string().min(1),
})

const creditSchema = z.object({
  amount: z.coerce.number().min(-100).max(100),
  reason: z.string().min(1),
})

type NoteForm = z.infer<typeof noteSchema>
type CreditForm = z.infer<typeof creditSchema>

export default function ClientDetail() {
  const { id } = useParams<{ id: string }>()
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const queryClient = useQueryClient()

  const [isNoteModalOpen, setIsNoteModalOpen] = useState(false)
  const [isCreditModalOpen, setIsCreditModalOpen] = useState(false)
  const [isTrainerModalOpen, setIsTrainerModalOpen] = useState(false)
  const [selectedTrainerId, setSelectedTrainerId] = useState<string | null>(null)

  const { data: client, isLoading } = useQuery({
    queryKey: ['admin', 'client', id],
    queryFn: () => adminApi.getClient(id!),
    enabled: !!id,
  })

  const { data: notes } = useQuery({
    queryKey: ['admin', 'client', id, 'notes'],
    queryFn: () => adminApi.getClientNotes(id!),
    enabled: !!id,
  })

  const { data: reservations } = useQuery({
    queryKey: ['admin', 'client', id, 'reservations'],
    queryFn: () => adminApi.getClientReservations(id!),
    enabled: !!id,
  })

  const { data: trainers } = useQuery({
    queryKey: ['admin', 'trainers'],
    queryFn: () => adminApi.getTrainers(),
  })

  const {
    register: registerNote,
    handleSubmit: handleNoteSubmit,
    reset: resetNote,
    formState: { errors: noteErrors },
  } = useForm<NoteForm>({
    resolver: zodResolver(noteSchema),
  })

  const {
    register: registerCredit,
    handleSubmit: handleCreditSubmit,
    reset: resetCredit,
    formState: { errors: creditErrors },
  } = useForm<CreditForm>({
    resolver: zodResolver(creditSchema),
  })

  const addNoteMutation = useMutation({
    mutationFn: (data: NoteForm) => adminApi.addClientNote(id!, data.note),
    onSuccess: () => {
      showToast('success', 'Poznámka přidána')
      queryClient.invalidateQueries({ queryKey: ['admin', 'client', id] })
      setIsNoteModalOpen(false)
      resetNote()
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const deleteNoteMutation = useMutation({
    mutationFn: (noteId: string) => adminApi.deleteClientNote(noteId),
    onSuccess: () => {
      showToast('success', 'Poznámka smazána')
      queryClient.invalidateQueries({ queryKey: ['admin', 'client', id] })
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const adjustCreditsMutation = useMutation({
    mutationFn: (data: CreditForm) =>
      adminApi.adjustClientCredits(id!, data.amount, data.reason),
    onSuccess: () => {
      showToast('success', 'Kredity upraveny')
      queryClient.invalidateQueries({ queryKey: ['admin', 'client', id] })
      setIsCreditModalOpen(false)
      resetCredit()
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const assignTrainerMutation = useMutation({
    mutationFn: (trainerId: string) => adminApi.assignTrainer(id!, trainerId),
    onSuccess: () => {
      showToast('success', t('admin.trainerAssigned'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'client', id] })
      setIsTrainerModalOpen(false)
      setSelectedTrainerId(null)
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const handleOpenTrainerModal = () => {
    setSelectedTrainerId(client?.trainerId || null)
    setIsTrainerModalOpen(true)
  }

  const handleAssignTrainer = () => {
    if (selectedTrainerId) {
      assignTrainerMutation.mutate(selectedTrainerId)
    }
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!client) {
    return (
      <div className="text-center py-12">
        <p className="text-neutral-500 dark:text-neutral-400">Klientka nenalezena</p>
      </div>
    )
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/admin/clients')}
          className="p-2 -ml-2 text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-white"
        >
          <ArrowLeft size={24} />
        </button>
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {client.firstName} {client.lastName}
        </h1>
      </div>

      {/* Client info */}
      <Card variant="bordered">
        <div className="flex items-center gap-4 mb-6">
          <div className="w-16 h-16 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
            <User size={32} className="text-primary-500" />
          </div>
          <div className="flex-1">
            <p className="font-medium text-neutral-900 dark:text-white text-lg">
              {client.firstName} {client.lastName}
            </p>
            <div className="flex items-center gap-1 text-neutral-500 dark:text-neutral-400 mt-1">
              <Mail size={14} />
              <span className="text-sm">{client.email}</span>
            </div>
            {client.phone && (
              <div className="flex items-center gap-1 text-neutral-500 dark:text-neutral-400 mt-1">
                <Phone size={14} />
                <span className="text-sm">{client.phone}</span>
              </div>
            )}
            <div className="flex items-center gap-1 text-neutral-500 dark:text-neutral-400 mt-1">
              <UserCog size={14} />
              <span className="text-sm">
                {client.trainerName ? `${t('admin.trainer')}: ${client.trainerName}` : t('admin.noTrainer')}
              </span>
            </div>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-2 gap-4">
          <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
            <div className="flex items-center gap-2 mb-1">
              <CreditCard size={16} className="text-primary-500" />
              <span className="text-sm text-neutral-500 dark:text-neutral-400">Kredity</span>
            </div>
            <p className="text-2xl font-bold text-neutral-900 dark:text-white">
              {client.credits}
            </p>
          </div>
          <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
            <div className="flex items-center gap-2 mb-1">
              <Calendar size={16} className="text-primary-500" />
              <span className="text-sm text-neutral-500 dark:text-neutral-400">Rezervací</span>
            </div>
            <p className="text-2xl font-bold text-neutral-900 dark:text-white">
              {reservations?.length || 0}
            </p>
          </div>
        </div>

        <div className="flex gap-3 mt-4 pt-4 border-t border-neutral-100 dark:border-dark-border">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setIsCreditModalOpen(true)}
          >
            {t('admin.adjustCredits')}
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={handleOpenTrainerModal}
          >
            <UserCog size={16} className="mr-1" />
            {t('admin.assignTrainer')}
          </Button>
        </div>
      </Card>

      {/* Notes */}
      <Card variant="bordered">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white">
            Poznámky
          </h2>
          <Button
            variant="ghost"
            size="sm"
            leftIcon={<Plus size={16} />}
            onClick={() => setIsNoteModalOpen(true)}
          >
            {t('admin.addNote')}
          </Button>
        </div>

        {notes && notes.length > 0 ? (
          <div className="space-y-3">
            {notes.map((note: ClientNote) => (
              <div
                key={note.id}
                className="p-3 bg-neutral-50 dark:bg-dark-surface rounded-lg"
              >
                <div className="flex items-start justify-between">
                  <p className="text-neutral-900 dark:text-white whitespace-pre-wrap">
                    {note.note}
                  </p>
                  <button
                    onClick={() => deleteNoteMutation.mutate(note.id)}
                    className="p-1 text-neutral-400 hover:text-red-500"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
                <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-2">
                  {new Date(note.createdAt).toLocaleDateString(i18n.language)}
                </p>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-neutral-500 dark:text-neutral-400">Žádné poznámky</p>
        )}
      </Card>

      {/* Recent reservations */}
      <Card variant="bordered">
        <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
          Poslední rezervace
        </h2>

        {reservations && reservations.length > 0 ? (
          <div className="space-y-2">
            {reservations.slice(0, 10).map((reservation: Reservation) => (
              <div
                key={reservation.id}
                className="flex items-center justify-between p-3 bg-neutral-50 dark:bg-dark-surface rounded-lg"
              >
                <div>
                  <p className="font-medium text-neutral-900 dark:text-white">
                    {formatDate(reservation.date)}
                  </p>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">
                    {formatTime(reservation.startTime)} - {formatTime(reservation.endTime)}
                  </p>
                </div>
                <Badge
                  variant={
                    reservation.status === 'confirmed'
                      ? 'success'
                      : reservation.status === 'cancelled'
                        ? 'danger'
                        : 'default'
                  }
                >
                  {reservation.status}
                </Badge>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-neutral-500 dark:text-neutral-400">Žádné rezervace</p>
        )}
      </Card>

      {/* Add note modal */}
      <Modal
        isOpen={isNoteModalOpen}
        onClose={() => {
          setIsNoteModalOpen(false)
          resetNote()
        }}
        title={t('admin.addNote')}
      >
        <form onSubmit={handleNoteSubmit((data) => addNoteMutation.mutate(data))} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              Poznámka
            </label>
            <textarea
              {...registerNote('note')}
              rows={4}
              className="w-full px-3 py-2 rounded-lg border border-neutral-200 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
              placeholder="Napište poznámku..."
            />
            {noteErrors.note && (
              <p className="text-sm text-red-500 mt-1">{t('errors.required')}</p>
            )}
          </div>

          <div className="flex gap-3">
            <Button
              type="button"
              variant="secondary"
              className="flex-1"
              onClick={() => {
                setIsNoteModalOpen(false)
                resetNote()
              }}
            >
              {t('common.cancel')}
            </Button>
            <Button type="submit" className="flex-1" isLoading={addNoteMutation.isPending}>
              {t('common.save')}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Adjust credits modal */}
      <Modal
        isOpen={isCreditModalOpen}
        onClose={() => {
          setIsCreditModalOpen(false)
          resetCredit()
        }}
        title={t('admin.adjustCredits')}
      >
        <form onSubmit={handleCreditSubmit((data) => adjustCreditsMutation.mutate(data))} className="space-y-4">
          <p className="text-sm text-neutral-500 dark:text-neutral-400">
            Aktuální stav: <strong>{client.credits}</strong> kreditů
          </p>

          <Input
            label="Počet kreditů (+/-)"
            type="number"
            {...registerCredit('amount')}
            error={creditErrors.amount?.message}
          />

          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              Důvod
            </label>
            <textarea
              {...registerCredit('reason')}
              rows={2}
              className="w-full px-3 py-2 rounded-lg border border-neutral-200 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
              placeholder="Důvod úpravy..."
            />
            {creditErrors.reason && (
              <p className="text-sm text-red-500 mt-1">{t('errors.required')}</p>
            )}
          </div>

          <div className="flex gap-3">
            <Button
              type="button"
              variant="secondary"
              className="flex-1"
              onClick={() => {
                setIsCreditModalOpen(false)
                resetCredit()
              }}
            >
              {t('common.cancel')}
            </Button>
            <Button type="submit" className="flex-1" isLoading={adjustCreditsMutation.isPending}>
              {t('common.save')}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Assign trainer modal */}
      <Modal
        isOpen={isTrainerModalOpen}
        onClose={() => {
          setIsTrainerModalOpen(false)
          setSelectedTrainerId(null)
        }}
        title={t('admin.assignTrainer')}
      >
        <div className="space-y-4">
          <p className="text-sm text-neutral-500 dark:text-neutral-400">
            {t('admin.selectTrainerForClient')}
          </p>

          {trainers && trainers.length > 0 ? (
            <div className="space-y-2 max-h-64 overflow-y-auto">
              {trainers.map((trainer: Trainer) => (
                <button
                  key={trainer.id}
                  onClick={() => setSelectedTrainerId(trainer.id)}
                  className={`w-full p-3 text-left rounded-lg border transition-colors ${
                    selectedTrainerId === trainer.id
                      ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                      : 'border-neutral-200 dark:border-neutral-700 hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover'
                  }`}
                >
                  <p className="font-medium text-neutral-900 dark:text-white">
                    {trainer.firstName && trainer.lastName
                      ? `${trainer.firstName} ${trainer.lastName}`
                      : trainer.email}
                  </p>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">
                    {trainer.email}
                  </p>
                </button>
              ))}
            </div>
          ) : (
            <p className="text-center py-4 text-neutral-500 dark:text-neutral-400">
              {t('admin.noTrainersAvailable')}
            </p>
          )}

          <div className="flex gap-3">
            <Button
              type="button"
              variant="secondary"
              className="flex-1"
              onClick={() => {
                setIsTrainerModalOpen(false)
                setSelectedTrainerId(null)
              }}
            >
              {t('common.cancel')}
            </Button>
            <Button
              className="flex-1"
              onClick={handleAssignTrainer}
              disabled={!selectedTrainerId}
              isLoading={assignTrainerMutation.isPending}
            >
              {t('common.save')}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
