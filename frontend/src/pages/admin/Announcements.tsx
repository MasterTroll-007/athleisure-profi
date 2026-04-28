import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { adminApi } from '@/services/api'
import Card from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Input from '@/components/ui/Input'
import Spinner from '@/components/ui/Spinner'
import Pagination from '@/components/ui/Pagination'
import { useToast } from '@/components/ui/Toast'
import { Send, Clock, Users } from 'lucide-react'

export default function Announcements() {
  const { t, i18n } = useTranslation()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const [subject, setSubject] = useState('')
  const [message, setMessage] = useState('')
  const [page, setPage] = useState(0)

  const { data: announcements, isLoading, isFetching } = useQuery({
    queryKey: ['admin', 'announcements', page],
    queryFn: () => adminApi.getAnnouncements(page, 10),
  })

  const sendMutation = useMutation({
    mutationFn: () => adminApi.createAnnouncement(subject, message),
    onSuccess: (data) => {
      showToast('success', t('announcements.sentTo', { count: data.recipientsCount }))
      setSubject('')
      setMessage('')
      queryClient.invalidateQueries({ queryKey: ['admin', 'announcements'] })
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  if (isLoading) return <div className="flex justify-center py-12"><Spinner /></div>

  return (
    <div className="space-y-6 animate-fade-in">
      <h1 className="text-2xl font-heading font-bold dark:text-white">{t('announcements.title')}</h1>

      <Card variant="bordered" className="p-6">
        <h2 className="text-lg font-semibold mb-4">{t('announcements.send')}</h2>
        <div className="space-y-4">
          <Input
            placeholder={t('announcements.subject')}
            value={subject}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSubject(e.target.value)}
          />
          <textarea
            className="w-full p-3 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-neutral-100 resize-y min-h-[120px]"
            placeholder={t('announcements.message')}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            rows={5}
          />
          <Button
            onClick={() => sendMutation.mutate()}
            leftIcon={<Send className="h-4 w-4" />}
            disabled={!subject.trim() || !message.trim() || sendMutation.isPending}
            isLoading={sendMutation.isPending}
          >
            {t('announcements.send')}
          </Button>
        </div>
      </Card>

      <Card variant="bordered" className="p-6">
        <h2 className="text-lg font-semibold mb-4">{t('announcements.history')}</h2>
        {!announcements?.content.length ? (
          <p className="text-neutral-500">{t('announcements.noAnnouncements')}</p>
        ) : (
          <div className="space-y-4">
            {announcements.content.map((a) => (
              <div key={a.id} className="border-b border-neutral-200 dark:border-neutral-700 pb-4 last:border-0">
                <div className="flex items-center justify-between mb-1">
                  <h3 className="font-semibold">{a.subject}</h3>
                  <div className="flex items-center gap-3 text-sm text-neutral-500">
                    <span className="flex items-center gap-1"><Users className="w-3 h-3" />{a.recipientsCount}</span>
                    <span className="flex items-center gap-1"><Clock className="w-3 h-3" />{new Date(a.createdAt).toLocaleDateString(i18n.language)}</span>
                  </div>
                </div>
                <p className="text-sm text-neutral-600 dark:text-neutral-400 whitespace-pre-line">{a.message}</p>
              </div>
            ))}
            <Pagination
              page={announcements.page}
              totalPages={announcements.totalPages}
              totalElements={announcements.totalElements}
              size={announcements.size}
              onPageChange={setPage}
              isLoading={isFetching}
            />
          </div>
        )}
      </Card>
    </div>
  )
}
