import { useMemo, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { Check, ChevronDown } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/utils/cn'
import { formatCredits } from '@/utils/formatters'

export interface TrainingTypeOption {
  id: string
  nameCs: string
  nameEn: string | null
  credits: number
}

interface TrainingTypeAccordionProps<T extends TrainingTypeOption> {
  items: T[]
  selectedIds: string[]
  onSelectedIdsChange?: (ids: string[]) => void
  selectionMode?: 'single' | 'multiple'
  readOnly?: boolean
  defaultOpen?: boolean
  className?: string
}

export function TrainingTypeAccordion<T extends TrainingTypeOption>({
  items,
  selectedIds,
  onSelectedIdsChange,
  selectionMode = 'multiple',
  readOnly = false,
  defaultOpen = false,
  className,
}: TrainingTypeAccordionProps<T>) {
  const { t, i18n } = useTranslation()
  const [isOpen, setIsOpen] = useState(defaultOpen)

  const selectedItems = useMemo(
    () => items.filter((item) => selectedIds.includes(item.id)),
    [items, selectedIds]
  )

  const getName = (item: TrainingTypeOption) =>
    i18n.language === 'cs' ? item.nameCs : (item.nameEn || item.nameCs)

  const summary = (() => {
    if (selectedItems.length === 0) return t('calendar.noTrainingTypeSelected')
    if (selectedItems.length === 1) {
      const item = selectedItems[0]
      return `${getName(item)} · ${formatCredits(item.credits, i18n.language)}`
    }
    return t('calendar.trainingTypesSelected', { count: selectedItems.length })
  })()

  const handleSelect = (id: string) => {
    if (readOnly || !onSelectedIdsChange) return

    if (selectionMode === 'single') {
      onSelectedIdsChange([id])
      setIsOpen(false)
      return
    }

    onSelectedIdsChange(
      selectedIds.includes(id)
        ? selectedIds.filter((selectedId) => selectedId !== id)
        : [...selectedIds, id]
    )
  }

  if (items.length === 0) return null

  return (
    <div
      className={cn(
        'w-full min-w-0 overflow-hidden rounded-xl border border-white/10 bg-white/[0.04] dark:bg-white/[0.04]',
        className
      )}
      data-testid="training-type-accordion"
    >
      <button
        type="button"
        className="flex w-full min-w-0 items-center gap-3 px-3 py-2.5 text-left transition-colors hover:bg-white/[0.06] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary-300"
        onClick={() => setIsOpen((open) => !open)}
        aria-expanded={isOpen}
      >
        <div className="min-w-0 flex-1">
          <p className="text-xs font-medium text-neutral-500 dark:text-neutral-400">
            {t('calendar.selectTrainingType')}
          </p>
          <p className="mt-0.5 truncate text-sm font-medium text-neutral-900 dark:text-white">
            {summary}
          </p>
        </div>
        <ChevronDown
          size={18}
          className={cn(
            'flex-shrink-0 text-neutral-500 transition-transform dark:text-neutral-400',
            isOpen && 'rotate-180'
          )}
        />
      </button>

      <AnimatePresence initial={false}>
        {isOpen && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.18, ease: 'easeOut' }}
            className="min-w-0 overflow-hidden border-t border-white/10"
          >
            <div className="max-h-44 min-w-0 overflow-y-auto overflow-x-hidden p-1.5">
              <div className="grid min-w-0 gap-1.5">
                {items.map((item) => {
                  const isSelected = selectedIds.includes(item.id)
                  const optionName = getName(item)

                  return (
                    <button
                      key={item.id}
                      type="button"
                      disabled={readOnly}
                      onClick={() => handleSelect(item.id)}
                      title={`${optionName} · ${formatCredits(item.credits, i18n.language)}`}
                      className={cn(
                        'grid w-full min-w-0 grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-2 rounded-lg border px-2.5 py-2 text-left transition-colors',
                        isSelected
                          ? 'border-primary-500/70 bg-primary-500/12'
                          : 'border-white/10 bg-black/[0.03] hover:border-white/20 hover:bg-white/[0.06] dark:bg-white/[0.03]',
                        readOnly && 'cursor-default hover:border-white/10 hover:bg-black/[0.03] dark:hover:bg-white/[0.03]'
                      )}
                    >
                      <span
                        className={cn(
                          'flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-md border',
                          selectionMode === 'single' && 'rounded-full',
                          isSelected
                            ? 'border-primary-500 bg-primary-500 text-white'
                            : 'border-neutral-300 bg-white/70 dark:border-neutral-600 dark:bg-neutral-800'
                        )}
                        aria-hidden="true"
                      >
                        {isSelected && <Check size={13} strokeWidth={3} />}
                      </span>
                      <span
                        className={cn(
                          'min-w-0 truncate text-sm',
                          isSelected
                            ? 'font-medium text-primary-700 dark:text-primary-300'
                            : 'text-neutral-700 dark:text-neutral-300'
                        )}
                      >
                        {optionName}
                      </span>
                      <span
                        className={cn(
                          'min-w-0 max-w-[7.5rem] flex-shrink-0 truncate rounded-full px-2 py-0.5 text-xs font-medium',
                          isSelected
                            ? 'bg-primary-100 text-primary-700 dark:bg-primary-800/50 dark:text-primary-300'
                            : 'bg-neutral-100 text-neutral-500 dark:bg-neutral-700 dark:text-neutral-300'
                        )}
                      >
                        {formatCredits(item.credits, i18n.language)}
                      </span>
                    </button>
                  )
                })}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
