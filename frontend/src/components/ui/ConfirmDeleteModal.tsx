import { useTranslation } from 'react-i18next'
import Modal from './Modal'
import Button from './Button'

interface ConfirmDeleteModalProps {
  isOpen: boolean
  title: string
  message: string
  isLoading: boolean
  onConfirm: () => void
  onClose: () => void
}

export default function ConfirmDeleteModal({
  isOpen,
  title,
  message,
  isLoading,
  onConfirm,
  onClose,
}: ConfirmDeleteModalProps) {
  const { t } = useTranslation()

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} size="sm">
      <div className="space-y-4">
        <p className="text-neutral-600 dark:text-neutral-300">{message}</p>
        <div className="flex gap-3">
          <Button variant="secondary" className="flex-1" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button
            variant="danger"
            className="flex-1"
            onClick={onConfirm}
            isLoading={isLoading}
          >
            {t('common.delete')}
          </Button>
        </div>
      </div>
    </Modal>
  )
}
