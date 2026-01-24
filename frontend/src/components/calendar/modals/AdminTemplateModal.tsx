import { useTranslation } from 'react-i18next'
import { Modal, Button } from '@/components/ui'
import type { SlotTemplate } from '@/types/api'

interface AdminTemplateModalProps {
  isOpen: boolean
  templates: SlotTemplate[] | undefined
  selectedTemplateId: string | null
  isLoading: boolean
  onSelectTemplate: (templateId: string) => void
  onApply: () => void
  onClose: () => void
}

export function AdminTemplateModal({
  isOpen,
  templates,
  selectedTemplateId,
  isLoading,
  onSelectTemplate,
  onApply,
  onClose,
}: AdminTemplateModalProps) {
  const { t } = useTranslation()

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={t('calendar.applyTemplate')} size="md">
      <div className="space-y-4">
        <p className="text-sm text-neutral-600 dark:text-neutral-400">
          {t('calendar.selectTemplateDescription')}
        </p>
        {templates && templates.length > 0 ? (
          <div className="space-y-2">
            {templates
              .filter((tmpl) => tmpl.isActive)
              .map((template) => (
                <button
                  key={template.id}
                  onClick={() => onSelectTemplate(template.id)}
                  className={`w-full p-3 text-left rounded-lg border transition-colors ${
                    selectedTemplateId === template.id
                      ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                      : 'border-neutral-200 dark:border-neutral-700 hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover'
                  }`}
                >
                  <p className="font-medium text-neutral-900 dark:text-white">{template.name}</p>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">
                    {template.slots.length} {t('calendar.slots')}
                  </p>
                </button>
              ))}
          </div>
        ) : (
          <p className="text-center text-neutral-500 dark:text-neutral-400 py-4">
            {t('calendar.noTemplates')}
          </p>
        )}
        <Button
          className="w-full"
          onClick={onApply}
          disabled={!selectedTemplateId}
          isLoading={isLoading}
        >
          {t('calendar.applyTemplate')}
        </Button>
      </div>
    </Modal>
  )
}
