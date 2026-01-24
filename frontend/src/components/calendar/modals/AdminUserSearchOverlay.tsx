import { RefObject } from 'react'
import { useTranslation } from 'react-i18next'
import { Search, X } from 'lucide-react'
import { Spinner } from '@/components/ui'
import type { User } from '@/types/api'

interface AdminUserSearchOverlayProps {
  isOpen: boolean
  searchQuery: string
  searchResults: User[]
  isSearching: boolean
  searchInputRef: RefObject<HTMLInputElement>
  onSearchChange: (query: string) => void
  onSelectUser: (user: User) => void
  onClose: () => void
}

export function AdminUserSearchOverlay({
  isOpen,
  searchQuery,
  searchResults,
  isSearching,
  searchInputRef,
  onSearchChange,
  onSelectUser,
  onClose,
}: AdminUserSearchOverlayProps) {
  const { t } = useTranslation()

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-[60] bg-white dark:bg-dark-bg flex flex-col">
      <div className="flex items-center gap-3 p-4 border-b border-neutral-200 dark:border-neutral-700">
        <button
          onClick={onClose}
          className="p-2 -ml-2 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-lg"
        >
          <X size={24} className="text-neutral-600 dark:text-neutral-300" />
        </button>
        <h2 className="text-lg font-semibold text-neutral-900 dark:text-white">
          {t('calendar.selectUser')}
        </h2>
      </div>
      <div className="p-4 border-b border-neutral-200 dark:border-neutral-700">
        <div className="relative">
          <Search size={20} className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" />
          <input
            ref={searchInputRef}
            type="text"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder={t('calendar.searchPlaceholder')}
            className="w-full pl-10 pr-4 py-3 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 text-base"
            autoComplete="off"
          />
          {searchQuery && (
            <button
              onClick={() => onSearchChange('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 p-1 hover:bg-neutral-100 dark:hover:bg-neutral-700 rounded"
            >
              <X size={18} className="text-neutral-400" />
            </button>
          )}
        </div>
      </div>
      <div className="flex-1 overflow-y-auto">
        {isSearching && (
          <div className="flex justify-center py-8">
            <Spinner size="md" />
          </div>
        )}
        {!isSearching && searchQuery.length >= 2 && searchResults.length === 0 && (
          <div className="text-center py-8 text-neutral-500 dark:text-neutral-400">
            {t('calendar.noUsersFound')}
          </div>
        )}
        {!isSearching && searchQuery.length < 2 && (
          <div className="text-center py-8 text-neutral-500 dark:text-neutral-400">
            {t('calendar.searchMinChars')}
          </div>
        )}
        {!isSearching && searchResults.length > 0 && (
          <div className="divide-y divide-neutral-200 dark:divide-neutral-700">
            {searchResults.map((user) => (
              <button
                key={user.id}
                onClick={() => onSelectUser(user)}
                className="w-full p-4 text-left hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center flex-shrink-0">
                    <span className="text-primary-600 dark:text-primary-400 font-medium">
                      {(user.firstName?.[0] || user.email[0]).toUpperCase()}
                    </span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-neutral-900 dark:text-white truncate">
                      {user.firstName && user.lastName
                        ? `${user.firstName} ${user.lastName}`
                        : user.email}
                    </p>
                    {user.firstName && user.lastName && (
                      <p className="text-sm text-neutral-500 dark:text-neutral-400 truncate">
                        {user.email}
                      </p>
                    )}
                    <p className="text-xs text-neutral-400">{t('nav.credits')}: {user.credits}</p>
                  </div>
                </div>
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
