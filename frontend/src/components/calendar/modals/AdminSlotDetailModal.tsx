import { useTranslation } from 'react-i18next'
import { Lock, Unlock, UserPlus, UserMinus, X } from 'lucide-react'
import { Modal, Button, Badge } from '@/components/ui'
import type { Slot, User } from '@/types/api'

interface AdminSlotDetailModalProps {
  isOpen: boolean
  slot: Slot | null
  // Note editing
  noteText: string
  isEditingNote: boolean
  onNoteChange: (note: string) => void
  onStartEditNote: () => void
  onCancelEditNote: () => void
  onSaveNote: () => void
  isNoteSaving: boolean
  // User selection
  selectedUser: User | null
  deductCredits: boolean
  onDeductCreditsChange: (value: boolean) => void
  onClearUser: () => void
  onOpenUserSearch: () => void
  // Actions
  onUnlockSlot: () => void
  onLockSlot: () => void
  onDeleteSlot: () => void
  onCreateReservation: () => void
  onOpenCancelConfirm: (withRefund: boolean) => void
  // Loading states
  isUpdating: boolean
  isDeleting: boolean
  isCreatingReservation: boolean
  // Cancel confirmation
  showCancelConfirm: boolean
  cancelWithRefund: boolean
  onCloseCancelConfirm: () => void
  onConfirmCancel: () => void
  isCancelling: boolean
  // Modal close
  onClose: () => void
}

export function AdminSlotDetailModal({
  isOpen,
  slot,
  noteText,
  isEditingNote,
  onNoteChange,
  onStartEditNote,
  onCancelEditNote,
  onSaveNote,
  isNoteSaving,
  selectedUser,
  deductCredits,
  onDeductCreditsChange,
  onClearUser,
  onOpenUserSearch,
  onUnlockSlot,
  onLockSlot,
  onDeleteSlot,
  onCreateReservation,
  onOpenCancelConfirm,
  isUpdating,
  isDeleting,
  isCreatingReservation,
  showCancelConfirm,
  cancelWithRefund,
  onCloseCancelConfirm,
  onConfirmCancel,
  isCancelling,
  onClose,
}: AdminSlotDetailModalProps) {
  const { t, i18n } = useTranslation()

  const formatSlotTime = (time: string) => time.substring(0, 5)

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={t('calendar.slotDetail')} size="md">
      {slot && (
        <div className="space-y-4">
          {/* Date and time */}
          <div>
            <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('calendar.dateAndTime')}</p>
            <p className="font-medium text-neutral-900 dark:text-white">
              {new Date(slot.date).toLocaleDateString(i18n.language, {
                weekday: 'long',
                day: 'numeric',
                month: 'long',
                year: 'numeric',
              })}
            </p>
            <p className="font-mono text-neutral-900 dark:text-white">
              {formatSlotTime(slot.startTime)} - {formatSlotTime(slot.endTime)}
            </p>
          </div>

          {/* Status */}
          <div>
            <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('calendar.status')}</p>
            <Badge
              variant={
                slot.status === 'reserved'
                  ? 'primary'
                  : slot.status === 'cancelled'
                    ? 'danger'
                    : slot.status === 'locked'
                      ? 'default'
                      : 'success'
              }
            >
              {slot.status === 'reserved'
                ? t('calendar.reserved')
                : slot.status === 'cancelled'
                  ? t('calendar.cancelled')
                  : slot.status === 'locked'
                    ? t('calendar.locked')
                    : t('calendar.available')}
            </Badge>
          </div>

          {/* Reserved slot section */}
          {slot.status === 'reserved' && slot.reservationId && (
            <>
              <div className="p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20">
                <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">
                  {t('calendar.reservationInfo')}
                </p>
                <p className="font-medium text-neutral-900 dark:text-white">
                  {slot.assignedUserName || slot.assignedUserEmail || t('calendar.unknown')}
                </p>
                {slot.assignedUserName && slot.assignedUserEmail && (
                  <p className="text-sm text-neutral-600 dark:text-neutral-300">{slot.assignedUserEmail}</p>
                )}
              </div>

              {/* Note section */}
              <div>
                <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('calendar.note')}</p>
                {isEditingNote ? (
                  <div className="space-y-2">
                    <textarea
                      value={noteText}
                      onChange={(e) => onNoteChange(e.target.value)}
                      placeholder={t('calendar.writeNote')}
                      className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
                      rows={3}
                    />
                    <div className="flex gap-2">
                      <Button variant="secondary" size="sm" className="flex-1" onClick={onCancelEditNote}>
                        {t('common.cancel')}
                      </Button>
                      <Button size="sm" className="flex-1" onClick={onSaveNote} isLoading={isNoteSaving}>
                        {t('common.save')}
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div
                    onClick={onStartEditNote}
                    className="p-2 rounded-lg bg-neutral-50 dark:bg-dark-surface border border-neutral-200 dark:border-neutral-700 cursor-pointer hover:bg-neutral-100 dark:hover:bg-dark-surfaceHover min-h-[60px]"
                  >
                    {slot.note ? (
                      <p className="text-sm text-neutral-700 dark:text-neutral-300 whitespace-pre-wrap">
                        {slot.note}
                      </p>
                    ) : (
                      <p className="text-sm text-neutral-400 italic">{t('calendar.clickToAddNote')}</p>
                    )}
                  </div>
                )}
              </div>

              {/* Cancel actions */}
              <div className="pt-2 space-y-2">
                <Button className="w-full" variant="danger" onClick={() => onOpenCancelConfirm(true)}>
                  <UserMinus size={18} className="mr-2" />
                  {t('calendar.cancelWithRefund')}
                </Button>
                <Button className="w-full" variant="secondary" onClick={() => onOpenCancelConfirm(false)}>
                  {t('calendar.cancelWithoutRefund')}
                </Button>
              </div>
            </>
          )}

          {/* Cancelled slot section */}
          {slot.status === 'cancelled' && (
            <>
              <div className="p-3 rounded-lg bg-red-50 dark:bg-red-900/20">
                <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">
                  {t('calendar.cancelledReservation')}
                </p>
                <p className="font-medium text-neutral-900 dark:text-white">
                  {slot.assignedUserName || slot.assignedUserEmail || t('calendar.unknown')}
                </p>
                {slot.assignedUserName && slot.assignedUserEmail && (
                  <p className="text-sm text-neutral-600 dark:text-neutral-300">{slot.assignedUserEmail}</p>
                )}
                {slot.cancelledAt && (
                  <p className="text-xs text-red-600 dark:text-red-400 mt-1">
                    {t('calendar.cancelledAt')}: {new Date(slot.cancelledAt).toLocaleString(i18n.language)}
                  </p>
                )}
              </div>

              <div className="pt-2 space-y-2">
                <Button className="w-full" variant="primary" onClick={onOpenUserSearch}>
                  <UserPlus size={18} className="mr-2" />
                  {t('calendar.registerUser')}
                </Button>
                <Button className="w-full" variant="secondary" onClick={onLockSlot} isLoading={isUpdating}>
                  <Lock size={18} className="mr-2" />
                  {t('calendar.lockSlot')}
                </Button>
              </div>
            </>
          )}

          {/* Locked or Unlocked slot actions */}
          {(slot.status === 'locked' || slot.status === 'unlocked') && (
            <div className="pt-2 space-y-3">
              {selectedUser ? (
                <>
                  <div className="p-3 rounded-lg bg-green-50 dark:bg-green-900/20 flex items-center justify-between">
                    <div>
                      <p className="font-medium text-neutral-900 dark:text-white">
                        {selectedUser.firstName} {selectedUser.lastName}
                      </p>
                      <p className="text-sm text-neutral-600 dark:text-neutral-300">{selectedUser.email}</p>
                      <p className="text-xs text-neutral-500">{t('nav.credits')}: {selectedUser.credits}</p>
                    </div>
                    <button
                      onClick={onClearUser}
                      className="p-1 hover:bg-neutral-200 dark:hover:bg-neutral-700 rounded"
                    >
                      <X size={18} />
                    </button>
                  </div>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={deductCredits}
                      onChange={(e) => onDeductCreditsChange(e.target.checked)}
                      className="w-4 h-4 rounded border-neutral-300 text-primary-500 focus:ring-primary-500"
                    />
                    <span className="text-sm text-neutral-700 dark:text-neutral-300">
                      {t('calendar.deductCredit', { available: selectedUser.credits })}
                    </span>
                  </label>
                  <div className="flex gap-2">
                    <Button variant="secondary" className="flex-1" onClick={onClearUser}>
                      {t('calendar.change')}
                    </Button>
                    <Button className="flex-1" onClick={onCreateReservation} isLoading={isCreatingReservation}>
                      {t('calendar.register')}
                    </Button>
                  </div>
                </>
              ) : (
                <>
                  <Button className="w-full" variant="primary" onClick={onOpenUserSearch}>
                    <UserPlus size={18} className="mr-2" />
                    {t('calendar.registerUser')}
                  </Button>
                  {slot.status === 'locked' ? (
                    <>
                      <Button
                        className="w-full"
                        variant="secondary"
                        onClick={onUnlockSlot}
                        isLoading={isUpdating}
                      >
                        <Unlock size={18} className="mr-2" />
                        {t('calendar.unlockSlot')}
                      </Button>
                      <Button
                        className="w-full"
                        variant="danger"
                        onClick={onDeleteSlot}
                        isLoading={isDeleting}
                      >
                        {t('calendar.deleteSlot')}
                      </Button>
                    </>
                  ) : (
                    <Button className="w-full" variant="secondary" onClick={onLockSlot} isLoading={isUpdating}>
                      <Lock size={18} className="mr-2" />
                      {t('calendar.lockSlot')}
                    </Button>
                  )}
                </>
              )}
            </div>
          )}

          {/* Cancel confirmation overlay */}
          {showCancelConfirm && slot.reservationId && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
              <div className="bg-white dark:bg-dark-surface rounded-lg p-6 max-w-sm mx-4 shadow-xl">
                <h3 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
                  {t('calendar.confirmCancel')}
                </h3>
                <p className="text-neutral-600 dark:text-neutral-300 mb-4">
                  {t('calendar.confirmCancelText')}{' '}
                  <strong>{slot.assignedUserName || slot.assignedUserEmail}</strong>?
                  {cancelWithRefund && ' ' + t('calendar.creditsWillBeRefunded')}
                  {!cancelWithRefund && ' ' + t('calendar.creditsWontBeRefunded')}
                </p>
                <div className="flex gap-3">
                  <Button variant="secondary" className="flex-1" onClick={onCloseCancelConfirm}>
                    {t('calendar.no')}
                  </Button>
                  <Button variant="danger" className="flex-1" onClick={onConfirmCancel} isLoading={isCancelling}>
                    {t('calendar.yesCancel')}
                  </Button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </Modal>
  )
}
