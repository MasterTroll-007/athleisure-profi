import { cn } from '@/utils/cn'

interface SkeletonProps {
  className?: string
}

export function Skeleton({ className }: SkeletonProps) {
  return (
    <div
      className={cn(
        'animate-pulse rounded-md bg-neutral-200 dark:bg-neutral-700',
        className
      )}
    />
  )
}

export function CardSkeleton() {
  return (
    <div className="rounded-xl border border-white/10 bg-white/50 dark:bg-neutral-800/50 p-4 space-y-3">
      <div className="flex items-center gap-3">
        <Skeleton className="h-10 w-10 rounded-full" />
        <div className="space-y-2 flex-1">
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-3 w-1/2" />
        </div>
      </div>
      <Skeleton className="h-4 w-full" />
      <Skeleton className="h-4 w-2/3" />
    </div>
  )
}

export function ReservationSkeleton() {
  return (
    <div className="rounded-xl border border-white/10 bg-white/50 dark:bg-neutral-800/50 p-4">
      <div className="flex items-center gap-4">
        <div className="flex flex-col items-center justify-center w-14 h-14 rounded-lg bg-neutral-100 dark:bg-neutral-700">
          <Skeleton className="h-3 w-8 mb-1" />
          <Skeleton className="h-5 w-6" />
        </div>
        <div className="flex-1 space-y-2">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-3 w-32" />
        </div>
        <Skeleton className="h-6 w-16 rounded-full" />
      </div>
    </div>
  )
}

export function ClientSkeleton() {
  return (
    <div className="rounded-xl border border-white/10 bg-white/50 dark:bg-neutral-800/50 p-4">
      <div className="flex items-center gap-3">
        <Skeleton className="h-12 w-12 rounded-full" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-4 w-32" />
          <Skeleton className="h-3 w-48" />
        </div>
        <Skeleton className="h-8 w-20 rounded-lg" />
      </div>
    </div>
  )
}

export function CreditPackageSkeleton() {
  return (
    <div className="rounded-xl border border-white/10 bg-white/50 dark:bg-neutral-800/50 p-4 space-y-3">
      <div className="flex justify-between items-start">
        <Skeleton className="h-5 w-24" />
        <Skeleton className="h-6 w-16 rounded-full" />
      </div>
      <Skeleton className="h-8 w-20" />
      <Skeleton className="h-10 w-full rounded-lg" />
    </div>
  )
}
