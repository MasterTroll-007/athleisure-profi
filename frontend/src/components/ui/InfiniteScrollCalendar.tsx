import { useRef, useEffect, useState, useCallback, useMemo, forwardRef, useImperativeHandle } from 'react'
import { useTranslation } from 'react-i18next'
import { slotVisualStyle } from './slotVisualStyle'
import { useDragDrop } from '@/components/calendar/desktop/useDragDrop'

export interface CalendarSlot {
  id: string
  date: string
  startTime: string
  endTime: string
  title: string
  backgroundColor: string
  borderColor: string
  textColor: string
  pattern?: 'stripes' | null
  opacity?: number
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
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
  onVisibleDateRangeChange?: (start: string, end: string) => void
  isAdmin?: boolean
  isLoading?: boolean
  // When set (and user is admin), enables long-press + drag on slots to
  // move them. Called on release with the snapped target date/time.
  onSlotDrop?: (slot: CalendarSlot, newDate: string, newStartTime: string) => void
}

export interface InfiniteScrollCalendarRef {
  scrollToToday: () => void
}

const TIME_COLUMN_WIDTH = 40 // px
const HOUR_HEIGHT = 48 // px for user view
const ADMIN_HOUR_HEIGHT = 64 // px for admin view (15min slots)
const DAYS_BACK_INITIAL = 7 // Initial days into the past
const DAYS_FORWARD_INITIAL = 14 // Initial days into the future (2 weeks ahead)
const LOAD_MORE_THRESHOLD = 3 // Load more when within 3 days of edge
const LOAD_MORE_DAYS = 7 // Load 7 more days at a time

export const InfiniteScrollCalendar = forwardRef<InfiniteScrollCalendarRef, InfiniteScrollCalendarProps>(({
  slots,
  initialDate,
  viewDays,
  startHour,
  endHour,
  onSlotClick,
  onDateClick,
  onDateRangeChange,
  onVisibleDateRangeChange,
  isAdmin = false,
  isLoading = false,
  onSlotDrop,
}, ref) => {
  const { i18n } = useTranslation()
  const containerRef = useRef<HTMLDivElement>(null)
  const scrollContainerRef = useRef<HTMLDivElement>(null)
  const headerScrollRef = useRef<HTMLDivElement>(null)
  const timeColumnRef = useRef<HTMLDivElement>(null)
  // Inner flex grid (the wide row of day columns inside the horizontal scroller).
  // Used as the reference frame for drag-drop drop targets.
  const innerGridRef = useRef<HTMLDivElement>(null)
  const [loadedDays, setLoadedDays] = useState<Date[]>([])
  const [containerWidth, setContainerWidth] = useState(0)
  const lastScrollLeft = useRef(0)
  const isInitialScroll = useRef(true)
  // Track the fetched date range to avoid refetching
  const fetchedRangeRef = useRef<{ start: string; end: string } | null>(null)
  const visibleRangeRef = useRef<{ start: string; end: string } | null>(null)
  const isInitialFetchDone = useRef(false)
  // Track if we're currently loading more days to prevent duplicate requests
  const isLoadingPast = useRef(false)
  const isLoadingFuture = useRef(false)

  const hourHeight = isAdmin ? ADMIN_HOUR_HEIGHT : HOUR_HEIGHT
  const totalHours = endHour - startHour

  // Store initial date in ref to avoid re-initialization on prop changes
  const initialDateRef = useRef<Date>(initialDate || new Date())
  const isInitialized = useRef(false)

  // Calculate dynamic column width based on container and viewDays
  const dayColumnWidth = useMemo(() => {
    if (containerWidth === 0) return 100
    const availableWidth = containerWidth - TIME_COLUMN_WIDTH
    return Math.floor(availableWidth / viewDays)
  }, [containerWidth, viewDays])

  // Expose scrollToToday method via ref
  useImperativeHandle(ref, () => ({
    scrollToToday: () => {
      if (!scrollContainerRef.current || loadedDays.length === 0 || dayColumnWidth === 0) return

      // Find today's index in loadedDays
      const today = new Date()
      const todayStr = today.toDateString()
      const todayIndex = loadedDays.findIndex(d => d.toDateString() === todayStr)

      if (todayIndex >= 0) {
        const scrollLeft = todayIndex * dayColumnWidth
        // Use smooth scrolling for nice transition effect
        scrollContainerRef.current.scrollTo({
          left: scrollLeft,
          behavior: 'smooth'
        })
        if (headerScrollRef.current) {
          headerScrollRef.current.scrollTo({
            left: scrollLeft,
            behavior: 'smooth'
          })
        }
      }
    }
  }), [loadedDays, dayColumnWidth])

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

  // Format date to ISO string (YYYY-MM-DD) using local timezone
  const formatDateISO = useCallback((date: Date): string => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }, [])

  const reportVisibleDateRange = useCallback((scrollLeft: number) => {
    if (!onVisibleDateRangeChange || loadedDays.length === 0 || dayColumnWidth === 0) return

    const visibleStartIndex = Math.min(
      Math.max(Math.round(scrollLeft / dayColumnWidth), 0),
      loadedDays.length - 1,
    )
    const visibleEndIndex = Math.min(visibleStartIndex + viewDays - 1, loadedDays.length - 1)
    const start = formatDateISO(loadedDays[visibleStartIndex])
    const end = formatDateISO(loadedDays[visibleEndIndex])
    const key = `${start}_${end}`

    if (visibleRangeRef.current && `${visibleRangeRef.current.start}_${visibleRangeRef.current.end}` === key) {
      return
    }

    visibleRangeRef.current = { start, end }
    onVisibleDateRangeChange(start, end)
  }, [dayColumnWidth, formatDateISO, loadedDays, onVisibleDateRangeChange, viewDays])

  // Initialize days: 7 days back, 14 days forward from initialDate (like mobile app)
  // Only run once on mount - subsequent date range changes are handled by scroll
  useEffect(() => {
    if (isInitialized.current) return
    isInitialized.current = true

    const baseDate = initialDateRef.current
    const days: Date[] = []

    // Generate days from DAYS_BACK_INITIAL in the past to DAYS_FORWARD_INITIAL in the future
    for (let i = -DAYS_BACK_INITIAL; i <= DAYS_FORWARD_INITIAL; i++) {
      const date = new Date(baseDate)
      date.setDate(date.getDate() + i)
      days.push(date)
    }
    setLoadedDays(days)

    // Trigger initial data fetch for the full range
    const startDate = formatDateISO(days[0])
    const endDate = formatDateISO(days[days.length - 1])
    fetchedRangeRef.current = { start: startDate, end: endDate }
    isInitialFetchDone.current = true
    onDateRangeChange(startDate, endDate)
  }, [formatDateISO, onDateRangeChange])

  // Scroll to today on mount (today is at index DAYS_BACK_INITIAL)
  useEffect(() => {
    if (scrollContainerRef.current && loadedDays.length > 0 && dayColumnWidth > 0 && isInitialScroll.current) {
      const todayIndex = DAYS_BACK_INITIAL
      const scrollLeft = todayIndex * dayColumnWidth
      scrollContainerRef.current.scrollLeft = scrollLeft
      if (headerScrollRef.current) {
        headerScrollRef.current.scrollLeft = scrollLeft
      }
      reportVisibleDateRange(scrollLeft)
      isInitialScroll.current = false
    }
  }, [loadedDays.length, dayColumnWidth, reportVisibleDateRange])

  useEffect(() => {
    if (!scrollContainerRef.current || loadedDays.length === 0 || dayColumnWidth === 0) return
    reportVisibleDateRange(scrollContainerRef.current.scrollLeft)
  }, [loadedDays.length, dayColumnWidth, viewDays, reportVisibleDateRange])

  // Load more days in the past
  const loadMorePast = useCallback(() => {
    if (isLoadingPast.current || loadedDays.length === 0) return
    isLoadingPast.current = true

    const firstDay = loadedDays[0]
    const newDays: Date[] = []

    // Add LOAD_MORE_DAYS days before the first loaded day
    for (let i = LOAD_MORE_DAYS; i >= 1; i--) {
      const newDate = new Date(firstDay)
      newDate.setDate(newDate.getDate() - i)
      newDays.push(newDate)
    }

    // Update loaded days and get the new full array
    const updatedDays = [...newDays, ...loadedDays]
    setLoadedDays(updatedDays)

    // Calculate the full range (all loaded days)
    const fullStart = formatDateISO(updatedDays[0])
    const fullEnd = formatDateISO(updatedDays[updatedDays.length - 1])

    // Update fetched range
    fetchedRangeRef.current = { start: fullStart, end: fullEnd }

    // Adjust scroll position to maintain view
    setTimeout(() => {
      if (scrollContainerRef.current) {
        scrollContainerRef.current.scrollLeft += LOAD_MORE_DAYS * dayColumnWidth
      }
      if (headerScrollRef.current) {
        headerScrollRef.current.scrollLeft += LOAD_MORE_DAYS * dayColumnWidth
      }
      isLoadingPast.current = false
    }, 0)

    // Fetch data for the FULL range (React Query will use cache for existing data)
    onDateRangeChange(fullStart, fullEnd)
  }, [loadedDays, formatDateISO, dayColumnWidth, onDateRangeChange])

  // Load more days in the future
  const loadMoreFuture = useCallback(() => {
    if (isLoadingFuture.current || loadedDays.length === 0) return
    isLoadingFuture.current = true

    const lastDay = loadedDays[loadedDays.length - 1]
    const newDays: Date[] = []

    // Add LOAD_MORE_DAYS days after the last loaded day
    for (let i = 1; i <= LOAD_MORE_DAYS; i++) {
      const newDate = new Date(lastDay)
      newDate.setDate(newDate.getDate() + i)
      newDays.push(newDate)
    }

    // Update loaded days and get the new full array
    const updatedDays = [...loadedDays, ...newDays]
    setLoadedDays(updatedDays)

    // Calculate the full range (all loaded days)
    const fullStart = formatDateISO(updatedDays[0])
    const fullEnd = formatDateISO(updatedDays[updatedDays.length - 1])

    // Update fetched range
    fetchedRangeRef.current = { start: fullStart, end: fullEnd }

    isLoadingFuture.current = false

    // Fetch data for the FULL range (React Query will use cache for existing data)
    onDateRangeChange(fullStart, fullEnd)
  }, [loadedDays, formatDateISO, onDateRangeChange])

  // Sync header and time column scroll with content scroll
  // Load more days when approaching edges (within LOAD_MORE_THRESHOLD days)
  const handleScroll = useCallback(() => {
    if (!scrollContainerRef.current || dayColumnWidth === 0 || loadedDays.length === 0) return

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

    // Calculate which day index is currently visible
    const visibleStartIndex = Math.floor(scrollLeft / dayColumnWidth)
    const visibleEndIndex = visibleStartIndex + viewDays
    reportVisibleDateRange(scrollLeft)

    // Load more past days when within threshold of the start
    if (visibleStartIndex <= LOAD_MORE_THRESHOLD && !isLoadingPast.current) {
      loadMorePast()
    }

    // Load more future days when within threshold of the end
    if (visibleEndIndex >= loadedDays.length - LOAD_MORE_THRESHOLD && !isLoadingFuture.current) {
      loadMoreFuture()
    }

    lastScrollLeft.current = scrollLeft
  }, [dayColumnWidth, loadedDays.length, viewDays, loadMorePast, loadMoreFuture, reportVisibleDateRange])

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
    // Add small gap (2px) between consecutive slots for visual separation
    const gap = 2

    const visual = slotVisualStyle(slot)
    return {
      position: 'absolute',
      top: `${top + gap}px`,
      height: `${Math.max(height - gap * 2, isAdmin ? 24 : 18)}px`,
      left: '3px',
      right: '3px',
      ...visual,
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

  // Drag-drop — only active when admin with an onSlotDrop handler.
  // Long-press gate prevents interfering with normal scroll.
  const dragDrop = useDragDrop({
    enabled: !!(isAdmin && onSlotDrop),
    days: loadedDays,
    startHour,
    hourHeight,
    containerRef,
    bodyRef: scrollContainerRef,
    gridRef: innerGridRef,
    onDrop: onSlotDrop || (() => {}),
    longPressMs: 400,
  })

  return (
    <div
      ref={containerRef}
      className="flex flex-col h-full bg-white dark:bg-dark-surface rounded-lg overflow-hidden relative"
      onPointerMove={dragDrop.onPointerMove}
      onPointerUp={dragDrop.onPointerUp}
      onPointerCancel={dragDrop.onPointerCancel}
    >
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

        {/* Scrollable day columns with snap. Snap is disabled while the user
            is actively dragging a slot — otherwise every tiny programmatic
            scrollBy lurches a whole column. */}
        <div
          ref={scrollContainerRef}
          className="flex-1 overflow-x-auto overflow-y-auto"
          style={{
            scrollSnapType: dragDrop.dragState.isDragging ? 'none' : 'x mandatory',
            scrollbarWidth: 'none',
            WebkitOverflowScrolling: 'touch',
          }}
          onScroll={handleScroll}
        >
          <div
            ref={innerGridRef}
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
                    className="absolute w-full border-b border-neutral-100 dark:border-neutral-800 cursor-pointer transition-colors hover:bg-primary-50/70 dark:hover:bg-primary-900/20"
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
                {getSlotsForDate(date).map((slot) => {
                  const isDragged = dragDrop.dragState.isDragging && dragDrop.dragState.slot?.id === slot.id
                  const baseStyle = getSlotStyle(slot)
                  const realSlotId = slot.data?.slot?.slotId ?? slot.data?.adminSlot?.id ?? slot.data?.reservation?.slotId ?? slot.id
                  return (
                    <div
                      key={slot.id}
                      data-testid="reservation-slot"
                      data-slot-id={realSlotId}
                      data-slot-date={slot.date}
                      style={{
                        ...baseStyle,
                        opacity: isDragged ? 0 : baseStyle.opacity,
                        // Admin slots opt out of native touch gestures entirely
                        // so long-press → drag isn't pre-empted by the browser
                        // committing the touch to a scroll. The user still
                        // scrolls by swiping empty grid area around the slots.
                        touchAction: isAdmin && onSlotDrop ? 'none' : undefined,
                      }}
                      onClick={(e) => {
                        e.stopPropagation()
                        onSlotClick(slot)
                      }}
                      onPointerDown={isAdmin && onSlotDrop ? (e) => dragDrop.onPointerDown(e, slot) : undefined}
                    >
                      {slot.title.split('\n').map((line, idx) => (
                        <div key={idx} className={idx === 0 ? 'font-medium truncate' : 'truncate'}>
                          {line}
                        </div>
                      ))}
                    </div>
                  )
                })}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Live drag preview — just the slot following the finger. No ghost
          left behind, no snap outline: grab it, place it. */}
      {dragDrop.dragState.isDragging && dragDrop.dragState.slot && (() => {
        const slot = dragDrop.dragState.slot
        const snap = dragDrop.dragState.snap
        const { pointerX, pointerY, grabOffsetX, grabOffsetY } = dragDrop.dragState
        const [sh, sm] = slot.startTime.split(':').map(Number)
        const [eh, em] = slot.endTime.split(':').map(Number)
        const durationMin = Math.max(15, (eh * 60 + em) - (sh * 60 + sm))
        const heightPx = (durationMin / 60) * hourHeight - 4
        const widthPx = dayColumnWidth - 6
        const visual = slotVisualStyle(slot)
        return (
          <div
            className="fixed pointer-events-none z-50 rounded shadow-2xl ring-2 ring-white/40 dark:ring-black/40"
            style={{
              left: pointerX - grabOffsetX,
              top: pointerY - grabOffsetY,
              width: widthPx,
              height: heightPx,
              ...visual,
              padding: '2px 4px',
              fontSize: 11,
              overflow: 'hidden',
            }}
          >
            {slot.title.split('\n').map((line, idx) => (
              <div key={idx} className={idx === 0 ? 'font-medium truncate' : 'truncate'}>
                {line}
              </div>
            ))}
            {snap && (
              <div className="mt-0.5 font-mono text-[10px] opacity-80">{snap.time}</div>
            )}
          </div>
        )
      })()}
    </div>
  )
})

InfiniteScrollCalendar.displayName = 'InfiniteScrollCalendar'

export default InfiniteScrollCalendar
