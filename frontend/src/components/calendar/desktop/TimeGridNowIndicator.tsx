import { useState, useEffect } from 'react'

interface TimeGridNowIndicatorProps {
  startHour: number
  endHour: number
  hourHeight: number
}

export function TimeGridNowIndicator({ startHour, endHour, hourHeight }: TimeGridNowIndicatorProps) {
  const [now, setNow] = useState(new Date())

  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 60_000)
    return () => clearInterval(timer)
  }, [])

  const hours = now.getHours()
  const minutes = now.getMinutes()

  if (hours < startHour || hours >= endHour) return null

  const top = ((hours - startHour) * 60 + minutes) / 60 * hourHeight

  return (
    <div
      className="absolute left-0 right-0 z-20 pointer-events-none"
      style={{ top: `${top}px` }}
    >
      <div className="relative">
        <div className="absolute -left-1.5 -top-1.5 w-3 h-3 rounded-full bg-red-500" />
        <div className="h-0.5 bg-red-500 w-full" />
      </div>
    </div>
  )
}
