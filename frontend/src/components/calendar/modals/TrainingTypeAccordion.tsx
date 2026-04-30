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
        'app-dropdown-panel w-full min-w-0',
        className
      )}
      data-testid="training-type-accordion"
    >
      <button
        type="button"
        className="group flex w-full min-w-0 items-center gap-3 px-3 py-2.5 text-left transition-colors hover:bg-white/[0.06] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary-300"
        onClick={() => setIsOpen((open) => !open)}
        aria-expanded={isOpen}
      >
        <div className="min-w-0 flex-1">
          <p className="text-xs font-medium text-white/46">
            {t('calendar.selectTrainingType')}
          </p>
          <p className="mt-0.5 truncate text-sm font-medium text-white">
            {summary}
          </p>
        </div>
        <ChevronDown
          size={18}
          className={cn(
            'flex-shrink-0 text-primary-100 transition-transform',
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
              <div className="grid min-w-0 gap-1">
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
                        'app-dropdown-item',
                        isSelected && 'app-dropdown-item-selected',
                        readOnly && 'cursor-default'
                      )}
                    >
                      <span
                        className={cn(
                          'flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-md border',
                          selectionMode === 'single' && 'rounded-full',
                          isSelected
                            ? 'border-primary-300 bg-primary-300 text-neutral-950'
                            : 'border-white/18 bg-black/20'
                        )}
                        aria-hidden="true"
                      >
                        {isSelected && <Check size={13} strokeWidth={3} />}
                      </span>
                      <span
                        className={cn(
                          'min-w-0 truncate text-sm',
                          isSelected
                            ? 'font-medium text-primary-100'
                            : 'text-white/78'
                        )}
                      >
                        {optionName}
                      </span>
                      <span
                        className={cn(
                          'min-w-0 max-w-[7.5rem] flex-shrink-0 truncate rounded-full px-2 py-0.5 text-xs font-medium',
                          isSelected
                            ? 'bg-primary-200/20 text-primary-100'
                            : 'bg-white/8 text-white/58'
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
