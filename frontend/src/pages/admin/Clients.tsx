import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Search, User, CreditCard, ChevronRight, ChevronDown } from 'lucide-react'
import { Card, Input, Button } from '@/components/ui'
import { ClientSkeleton } from '@/components/ui/Skeleton'
import EmptyState from '@/components/ui/EmptyState'
import { adminApi } from '@/services/api'
import type { User as UserType } from '@/types/api'

export default function Clients() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [searchQuery, setSearchQuery] = useState('')
  const [page, setPage] = useState(0)
  const [allClients, setAllClients] = useState<UserType[]>([])

  // Paginated query for initial load
  const { data: clientsPage, isLoading, isFetching } = useQuery({
    queryKey: ['admin', 'clients', page],
    queryFn: () => adminApi.getClients(page, 20),
    enabled: !searchQuery,
  })

  // Search query
  const { data: searchResults, isLoading: isSearching } = useQuery({
    queryKey: ['admin', 'clients', 'search', searchQuery],
    queryFn: () => adminApi.searchClients(searchQuery),
    enabled: searchQuery.length >= 2,
  })

  // Accumulate clients for infinite scroll
  useEffect(() => {
    if (clientsPage && !searchQuery) {
      if (page === 0) {
        setAllClients(clientsPage.content)
      } else {
        setAllClients(prev => [...prev, ...clientsPage.content])
      }
    }
  }, [clientsPage, page, searchQuery])

  // Reset when search changes
  useEffect(() => {
    if (searchQuery) {
      setAllClients([])
      setPage(0)
    }
  }, [searchQuery])

  const displayedClients = searchQuery.length >= 2 ? searchResults : allClients
  const hasMore = !searchQuery && clientsPage?.hasNext
  const showLoading = isLoading || isSearching

  const loadMore = () => {
    if (hasMore && !isFetching) {
      setPage(prev => prev + 1)
    }
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
        {t('admin.clients')}
      </h1>

      {/* Search */}
      <Input
        placeholder={t('admin.searchClients')}
        leftIcon={<Search size={18} />}
        value={searchQuery}
        onChange={(e) => setSearchQuery(e.target.value)}
      />

      {/* Clients list */}
      {showLoading && allClients.length === 0 ? (
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => (
            <ClientSkeleton key={i} />
          ))}
        </div>
      ) : displayedClients && displayedClients.length > 0 ? (
        <div className="space-y-3">
          {displayedClients.map((client) => (
            <Card
              key={client.id}
              variant="bordered"
              className="cursor-pointer hover:border-primary-500 transition-colors"
              onClick={() => navigate(`/admin/clients/${client.id}`)}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
                    <User size={20} className="text-primary-500" />
                  </div>
                  <div>
                    <p className="font-medium text-neutral-900 dark:text-white">
                      {client.firstName} {client.lastName}
                    </p>
                    <p className="text-sm text-neutral-500 dark:text-neutral-400">
                      {client.email}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-4">
                  <div className="hidden sm:flex items-center gap-4 text-sm">
                    <div className="flex items-center gap-1 text-neutral-500 dark:text-neutral-400">
                      <CreditCard size={14} />
                      <span>{client.credits} kr.</span>
                    </div>
                  </div>
                  <ChevronRight size={20} className="text-neutral-400" />
                </div>
              </div>
            </Card>
          ))}

          {/* Load more button */}
          {hasMore && (
            <div className="flex justify-center pt-4">
              <Button
                variant="secondary"
                onClick={loadMore}
                isLoading={isFetching}
                leftIcon={<ChevronDown size={18} />}
              >
                {t('admin.loadMore')}
              </Button>
            </div>
          )}
        </div>
      ) : (
        <EmptyState
          icon={User}
          title={searchQuery ? t('admin.noResults') : t('admin.noClients')}
          description={searchQuery ? t('admin.tryDifferentTerm') : undefined}
        />
      )}

      {/* Total count */}
      {clientsPage && !searchQuery && (
        <p className="text-sm text-neutral-500 dark:text-neutral-400 text-center">
          {t('admin.showingOfClients', { shown: allClients.length, total: clientsPage.totalElements })}
        </p>
      )}
    </div>
  )
}
