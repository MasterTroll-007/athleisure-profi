import { useRef, useEffect, useState, useCallback, useMemo } from 'react'
import { useTranslation } from 'react-i18next'

export interface CalendarSlot {
  id: string
  date: string
  startTime: string
  endTime: string
  title: string
  backgroundColor: string
  borderColor: string
  textColor: string
  data: any // Original slot/event data
}

interface InfiniteScrollCalendarProps {
  slots: CalendarSlot[]
  initialDate: Date
  viewDays: number // 1, 3, 5, or 7
  startHour: number
  endHour: number
  onSlotClick: (slot: CalendarSlot) => void
  onDateClick?: (date: string, time: string) => void
  onDateRangeChange: (start: string, end: string) => void
  isAdmin?: boolean
  isLoading?: boolean
}

const TIME_COLUMN_WIDTH = 40 // px
const HOUR_HEIGHT = 48 // px for user view
const ADMIN_HOUR_HEIGHT = 64 // px for admin view (15min slots)
const BUFFER_DAYS = 14 // Days to load on each side

export function InfiniteScrollCalendar({
  slots,
  initialDate,
  viewDays,
  startHour,
  endHour,
  onSlotClick,
  onDateClick,
  onDateRangeChange,
  isAdmin = false,
  isLoading = false,
}: InfiniteScrollCalendarProps) {
  const { i18n } = useTranslation()
  const containerRef = useRef<HTMLDivElement>(null)
  const scrollContainerRef = useRef<HTMLDivElement>(null)
  const headerScrollRef = useRef<HTMLDivElement>(null)
  const timeColumnRef = useRef<HTMLDivElement>(null)
  const [loadedDays, setLoadedDays] = useState<Date[]>([])
  const [containerWidth, setContainerWidth] = useState(0)
  const lastScrollLeft = useRef(0)
  const isInitialScroll = useRef(true)

  const hourHeight = isAdmin ? ADMIN_HOUR_HEIGHT : HOUR_HEIGHT
  const totalHours = endHour - startHour

  // Calculate dynamic column width based on container and viewDays
  const dayColumnWidth = useMemo(() => {
    if (containerWidth === 0) return 100
    const availableWidth = containerWidth - TIME_COLUMN_WIDTH
    return Math.floor(availableWidth / viewDays)
  }, [containerWidth, viewDays])

  // Measure container width
  useEffect(() => {
    const updateWidth = () => {
      if (containerRef.current) {
        setContainerWidth(containerRef.current.offsetWidth)
      }
    }
    updateWidth()
    window.addEventListener('resize', updateWidth)
    return () => window.removeEventListener('resize', updateWidth)
  }, [])

  // Initialize days centered on initialDate
  useEffect(() => {
    const days: Date[] = []
    for (let i = -BUFFER_DAYS; i <= BUFFER_DAYS; i++) {
      const date = new Date(initialDate)
      date.setDate(date.getDate() + i)
      days.push(date)
    }
    setLoadedDays(days)
  }, [initialDate])

  // Scroll to center (initialDate) on mount
  useEffect(() => {
    if (scrollContainerRef.current && loadedDays.length > 0 && dayColumnWidth > 0 && isInitialScroll.current) {
      const centerIndex = BUFFER_DAYS
      const scrollLeft = centerIndex * dayColumnWidth
      scrollContainerRef.current.scrollLeft = scrollLeft
      if (headerScrollRef.current) {
        headerScrollRef.current.scrollLeft = scrollLeft
      }
      isInitialScroll.current = false
    }
  }, [loadedDays.length, dayColumnWidth])

  // Sync header and time column scroll with content scroll
  const handleScroll = useCallback(() => {
    if (!scrollContainerRef.current || dayColumnWidth === 0) return

    const scrollLeft = scrollContainerRef.current.scrollLeft
    const scrollTop = scrollContainerRef.current.scrollTop

    // Sync header (horizontal)
    if (headerScrollRef.current) {
      headerScrollRef.current.scrollLeft = scrollLeft
    }

    // Sync time column (vertical)
    if (timeColumnRef.current) {
      timeColumnRef.current.scrollTop = scrollTop
    }

    // Check if we need to load more days
    const maxScroll = scrollContainerRef.current.scrollWidth - scrollContainerRef.current.clientWidth

    // Load more future days
    if (scrollLeft > maxScroll - dayColumnWidth * 3) {
      setLoadedDays(prev => {
        const lastDay = prev[prev.length - 1]
        const newDays = [...prev]
        for (let i = 1; i <= 7; i++) {
          const newDate = new Date(lastDay)
          newDate.setDate(newDate.getDate() + i)
          newDays.push(newDate)
        }
        return newDays
      })
    }

    // Load more past days
    if (scrollLeft < dayColumnWidth * 3) {
      setLoadedDays(prev => {
        const firstDay = prev[0]
        const newDays: Date[] = []
        for (let i = 7; i >= 1; i--) {
          const newDate = new Date(firstDay)
          newDate.setDate(newDate.getDate() - i)
          newDays.push(newDate)
        }
        // Adjust scroll position to maintain view
        setTimeout(() => {
          if (scrollContainerRef.current) {
            scrollContainerRef.current.scrollLeft += 7 * dayColumnWidth
          }
          if (headerScrollRef.current) {
            headerScrollRef.current.scrollLeft += 7 * dayColumnWidth
          }
        }, 0)
        return [...newDays, ...prev]
      })
    }

    // Update date range for data fetching
    const visibleStartIndex = Math.floor(scrollLeft / dayColumnWidth)
    const visibleEndIndex = visibleStartIndex + viewDays + 2

    if (loadedDays[visibleStartIndex] && loadedDays[visibleEndIndex]) {
      const startDate = loadedDays[Math.max(0, visibleStartIndex - 7)]
      const endDate = loadedDays[Math.min(loadedDays.length - 1, visibleEndIndex + 7)]
      if (startDate && endDate) {
        onDateRangeChange(
          formatDateISO(startDate),
          formatDateISO(endDate)
        )
      }
    }

    lastScrollLeft.current = scrollLeft
  }, [loadedDays, viewDays, onDateRangeChange, dayColumnWidth])

  // Format date to ISO string (YYYY-MM-DD)
  const formatDateISO = (date: Date): string => {
    return date.toISOString().split('T')[0]
  }

  // Format date for header display
  const formatDateHeader = (date: Date): string => {
    const dayNames = i18n.language === 'cs'
      ? ['ne', 'po', 'út', 'st', 'čt', 'pá', 'so']
      : ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
    const dayName = dayNames[date.getDay()]
    const day = date.getDate()
    const month = date.getMonth() + 1
    return `${dayName} ${day}.${month}.`
  }

  // Check if date is today
  const isToday = (date: Date): boolean => {
    const today = new Date()
    return date.toDateString() === today.toDateString()
  }

  // Get slots for a specific date
  const getSlotsForDate = (date: Date): CalendarSlot[] => {
    const dateStr = formatDateISO(date)
    return slots.filter(slot => slot.date === dateStr)
  }

  // Calculate slot position and height
  const getSlotStyle = (slot: CalendarSlot): React.CSSProperties => {
    const [startH, startM] = slot.startTime.split(':').map(Number)
    const [endH, endM] = slot.endTime.split(':').map(Number)

    const startMinutes = (startH - startHour) * 60 + startM
    const endMinutes = (endH - startHour) * 60 + endM
    const durationMinutes = endMinutes - startMinutes

    const top = (startMinutes / 60) * hourHeight
    const height = (durationMinutes / 60) * hourHeight

    return {
      position: 'absolute',
      top: `${top}px`,
      height: `${Math.max(height, 20)}px`,
      left: '2px',
      right: '2px',
      backgroundColor: slot.backgroundColor,
      borderLeft: `3px solid ${slot.borderColor}`,
      color: slot.textColor,
      borderRadius: '4px',
      padding: '2px 4px',
      fontSize: '11px',
      overflow: 'hidden',
      cursor: 'pointer',
      zIndex: 10,
    }
  }

  // Handle click on empty time slot
  const handleTimeClick = (date: Date, hour: number, minutes: number = 0) => {
    if (onDateClick) {
      const dateStr = formatDateISO(date)
      const timeStr = `${hour.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`
      onDateClick(dateStr, timeStr)
    }
  }

  // Generate time labels
  const timeLabels: number[] = []
  for (let h = startHour; h < endHour; h++) {
    timeLabels.push(h)
  }

  return (
    <div ref={containerRef} className="flex flex-col h-full bg-white dark:bg-dark-surface rounded-lg overflow-hidden relative">
      {/* Loading overlay */}
      {isLoading && (
        <div className="absolute inset-0 bg-white/50 dark:bg-dark-bg/50 z-20 flex items-center justify-center">
          <div className="w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin" />
        </div>
      )}

      {/* Header with day names - synchronized scroll */}
      <div className="flex border-b border-neutral-200 dark:border-neutral-700">
        {/* Empty corner for time column */}
        <div
          className="flex-shrink-0 border-r border-neutral-200 dark:border-neutral-700"
          style={{ width: TIME_COLUMN_WIDTH }}
        />
        {/* Scrollable day headers */}
        <div
          ref={headerScrollRef}
          className="flex-1 overflow-hidden"
          style={{ scrollbarWidth: 'none' }}
        >
          <div className="flex" style={{ width: loadedDays.length * dayColumnWidth }}>
            {loadedDays.map((date, index) => (
              <div
                key={index}
                className={`flex-shrink-0 py-2 text-center text-xs font-medium border-r border-neutral-200 dark:border-neutral-700 ${
                  isToday(date)
                    ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-400'
                    : 'text-neutral-600 dark:text-neutral-400'
                }`}
                style={{ width: dayColumnWidth }}
              >
                {formatDateHeader(date)}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Main content area */}
      <div className="flex flex-1 overflow-hidden">
        {/* Time column - synced with content vertical scroll */}
        <div
          ref={timeColumnRef}
          className="flex-shrink-0 overflow-hidden border-r border-neutral-200 dark:border-neutral-700"
          style={{ width: TIME_COLUMN_WIDTH }}
        >
          {timeLabels.map(hour => (
            <div
              key={hour}
              className="text-[10px] text-neutral-500 dark:text-neutral-400 text-right pr-2 border-b border-neutral-100 dark:border-neutral-800"
              style={{ height: hourHeight }}
            >
              {hour}:00
            </div>
          ))}
        </div>

        {/* Scrollable day columns with snap */}
        <div
          ref={scrollContainerRef}
          className="flex-1 overflow-x-auto overflow-y-auto"
          style={{
            scrollSnapType: 'x mandatory',
            scrollbarWidth: 'none',
            WebkitOverflowScrolling: 'touch',
          }}
          onScroll={handleScroll}
        >
          <div
            className="flex relative"
            style={{
              width: loadedDays.length * dayColumnWidth,
              height: totalHours * hourHeight,
            }}
          >
            {loadedDays.map((date, dayIndex) => (
              <div
                key={dayIndex}
                className={`flex-shrink-0 relative border-r border-neutral-200 dark:border-neutral-700 ${
                  isToday(date) ? 'bg-primary-50/50 dark:bg-primary-900/10' : ''
                }`}
                style={{
                  width: dayColumnWidth,
                  height: totalHours * hourHeight,
                  scrollSnapAlign: 'start',
                  scrollSnapStop: 'always',
                }}
              >
                {/* Hour grid lines */}
                {timeLabels.map((hour, hourIndex) => (
                  <div
                    key={hourIndex}
                    className="absolute w-full border-b border-neutral-100 dark:border-neutral-800 cursor-pointer hover:bg-neutral-50 dark:hover:bg-neutral-800/50"
                    style={{
                      top: hourIndex * hourHeight,
                      height: hourHeight,
                    }}
                    onClick={() => handleTimeClick(date, hour)}
                  >
                    {/* 30-minute line for admin */}
                    {isAdmin && (
                      <>
                        <div
                          className="absolute w-full border-b border-neutral-50 dark:border-neutral-800/50"
                          style={{ top: hourHeight / 4 }}
                          onClick={(e) => { e.stopPropagation(); handleTimeClick(date, hour, 15); }}
                        />
                        <div
                          className="absolute w-full border-b border-neutral-100 dark:border-neutral-800"
                          style={{ top: hourHeight / 2 }}
                          onClick={(e) => { e.stopPropagation(); handleTimeClick(date, hour, 30); }}
                        />
                        <div
                          className="absolute w-full border-b border-neutral-50 dark:border-neutral-800/50"
                          style={{ top: (hourHeight / 4) * 3 }}
                          onClick={(e) => { e.stopPropagation(); handleTimeClick(date, hour, 45); }}
                        />
                      </>
                    )}
                  </div>
                ))}

                {/* Slots/Events */}
                {getSlotsForDate(date).map((slot) => (
                  <div
                    key={slot.id}
                    style={getSlotStyle(slot)}
                    onClick={(e) => {
                      e.stopPropagation()
                      onSlotClick(slot)
                    }}
                  >
                    <div className="font-medium truncate">{slot.title}</div>
                  </div>
                ))}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

export default InfiniteScrollCalendar
