import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Search, User, CreditCard, ChevronRight } from 'lucide-react'
import { Card, Input, Spinner } from '@/components/ui'
import { adminApi } from '@/services/api'

export default function Clients() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [searchQuery, setSearchQuery] = useState('')

  const { data: clients, isLoading } = useQuery({
    queryKey: ['admin', 'clients'],
    queryFn: () => adminApi.getClients(),
  })

  const filteredClients = clients?.filter(
    (client) =>
      client.firstName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      client.lastName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      client.email.toLowerCase().includes(searchQuery.toLowerCase())
  )

  return (
    <div className="space-y-6 animate-fade-in">
      <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
        {t('admin.clients')}
      </h1>

      {/* Search */}
      <Input
        placeholder="Hledat klientku..."
        leftIcon={<Search size={18} />}
        value={searchQuery}
        onChange={(e) => setSearchQuery(e.target.value)}
      />

      {/* Clients list */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : filteredClients && filteredClients.length > 0 ? (
        <div className="space-y-3">
          {filteredClients.map((client) => (
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
        </div>
      ) : (
        <Card variant="bordered">
          <div className="text-center py-12">
            <User className="mx-auto mb-4 text-neutral-300 dark:text-neutral-600" size={48} />
            <p className="text-neutral-500 dark:text-neutral-400">
              {searchQuery ? 'Žádné výsledky' : 'Žádné klientky'}
            </p>
          </div>
        </Card>
      )}
    </div>
  )
}
