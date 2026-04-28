import { ChevronLeft, ChevronRight } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import Button from './Button'

interface PaginationProps {
  page: number
  totalPages: number
  totalElements: number
  size: number
  onPageChange: (page: number) => void
  isLoading?: boolean
}

export default function Pagination({
  page,
  totalPages,
  totalElements,
  size,
  onPageChange,
  isLoading = false,
}: PaginationProps) {
  const { t } = useTranslation()
  const safeTotalPages = Math.max(totalPages, 1)
  const from = totalElements === 0 ? 0 : page * size + 1
  const to = Math.min((page + 1) * size, totalElements)

  if (totalPages <= 1 && totalElements <= size) return null

  return (
    <div className="flex flex-col items-center justify-between gap-3 pt-4 text-sm text-neutral-500 dark:text-neutral-400 sm:flex-row">
      <span>
        {t('pagination.range', { from, to, total: totalElements })}
      </span>
      <div className="flex items-center gap-3">
        <Button
          type="button"
          variant="secondary"
          size="sm"
          leftIcon={<ChevronLeft size={16} />}
          disabled={page <= 0 || isLoading}
          onClick={() => onPageChange(Math.max(page - 1, 0))}
        >
          {t('common.previous')}
        </Button>
        <span className="min-w-[5.5rem] text-center">
          {t('pagination.page', { page: page + 1, total: safeTotalPages })}
        </span>
        <Button
          type="button"
          variant="secondary"
          size="sm"
          rightIcon={<ChevronRight size={16} />}
          disabled={page >= totalPages - 1 || isLoading}
          onClick={() => onPageChange(page + 1)}
        >
          {t('common.next')}
        </Button>
      </div>
    </div>
  )
}
