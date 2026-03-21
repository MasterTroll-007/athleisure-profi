import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { adminApi } from '@/services/api'
import Card from '@/components/ui/Card'
import Spinner from '@/components/ui/Spinner'
import { Star } from 'lucide-react'

function StarRating({ rating }: { rating: number }) {
  return (
    <div className="flex gap-0.5">
      {[1, 2, 3, 4, 5].map((i) => (
        <Star
          key={i}
          className={`w-4 h-4 ${i <= rating ? 'text-yellow-400 fill-yellow-400' : 'text-neutral-300'}`}
        />
      ))}
    </div>
  )
}

export default function Feedback() {
  const { t } = useTranslation()

  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ['admin', 'feedback', 'summary'],
    queryFn: () => adminApi.getFeedbackSummary(),
  })

  const { data: feedback, isLoading: feedbackLoading } = useQuery({
    queryKey: ['admin', 'feedback', 'all'],
    queryFn: () => adminApi.getAllFeedback(),
  })

  if (summaryLoading || feedbackLoading) return <div className="flex justify-center py-12"><Spinner /></div>

  return (
    <div className="space-y-6 animate-fade-in">
      <h1 className="text-2xl font-heading font-bold">{t('admin.feedbackTitle')}</h1>

      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card variant="bordered" className="p-6 text-center">
            <div className="text-3xl font-bold text-primary-600">
              {summary.averageRating ? summary.averageRating.toFixed(1) : '-'}
            </div>
            <div className="flex justify-center mt-1">
              {summary.averageRating ? <StarRating rating={Math.round(summary.averageRating)} /> : null}
            </div>
            <div className="text-sm text-neutral-500 mt-1">{t('admin.averageRating')}</div>
          </Card>
          <Card variant="bordered" className="p-6 text-center">
            <div className="text-3xl font-bold text-primary-600">{summary.totalCount}</div>
            <div className="text-sm text-neutral-500">Celkem hodnocení</div>
          </Card>
          <Card variant="bordered" className="p-6">
            <div className="text-sm font-semibold mb-2">Distribuce</div>
            {[5, 4, 3, 2, 1].map((star) => {
              const count = summary.distribution[star] || 0
              const pct = summary.totalCount > 0 ? (count / summary.totalCount) * 100 : 0
              return (
                <div key={star} className="flex items-center gap-2 mb-1">
                  <span className="text-xs w-3">{star}</span>
                  <Star className="w-3 h-3 text-yellow-400 fill-yellow-400" />
                  <div className="flex-1 h-2 bg-neutral-200 dark:bg-neutral-700 rounded-full overflow-hidden">
                    <div className="h-full bg-yellow-400 rounded-full" style={{ width: `${pct}%` }} />
                  </div>
                  <span className="text-xs text-neutral-500 w-8">{count}</span>
                </div>
              )
            })}
          </Card>
        </div>
      )}

      <Card variant="bordered" className="p-6">
        <h2 className="text-lg font-semibold mb-4">Všechna hodnocení</h2>
        {!feedback?.length ? (
          <p className="text-neutral-500">Žádná hodnocení</p>
        ) : (
          <div className="space-y-3">
            {feedback.map((f) => (
              <div key={f.id} className="flex items-start gap-3 border-b border-neutral-200 dark:border-neutral-700 pb-3 last:border-0">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-medium">{f.userName || 'Anonym'}</span>
                    <StarRating rating={f.rating} />
                    {f.date && <span className="text-xs text-neutral-500">{new Date(f.date).toLocaleDateString('cs')}</span>}
                  </div>
                  {f.comment && <p className="text-sm text-neutral-600 dark:text-neutral-400">{f.comment}</p>}
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}
