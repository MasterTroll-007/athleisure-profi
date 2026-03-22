import { useCallback, useMemo } from 'react'
import type { CalendarSlot } from '@/components/ui'
import { HOUR_HEIGHT, ADMIN_HOUR_HEIGHT, EVENT_GAP, MIN_EVENT_HEIGHT } from './constants'

export interface SlotWithPosition extends CalendarSlot {
  top: number
  height: number
  column: number
  totalColumns: number
}

export function useTimeGrid(startHour: number, endHour: number, isAdmin: boolean) {
  const hourHeight = isAdmin ? ADMIN_HOUR_HEIGHT : HOUR_HEIGHT
  const totalHours = endHour - startHour
  const totalHeight = totalHours * hourHeight

  const getSlotPosition = useCallback((slot: CalendarSlot): { top: number; height: number } => {
    const [startH, startM] = slot.startTime.split(':').map(Number)
    const [endH, endM] = slot.endTime.split(':').map(Number)

    const startMinutes = (startH - startHour) * 60 + startM
    const endMinutes = (endH - startHour) * 60 + endM
    const durationMinutes = endMinutes - startMinutes

    const top = (startMinutes / 60) * hourHeight + EVENT_GAP
    const height = Math.max((durationMinutes / 60) * hourHeight - EVENT_GAP * 2, MIN_EVENT_HEIGHT)

    return { top, height }
  }, [startHour, hourHeight])

  const pixelToTime = useCallback((y: number, snapMinutes: number = 15): { hours: number; minutes: number } => {
    const totalMinutes = (y / hourHeight) * 60 + startHour * 60
    const snapped = Math.round(totalMinutes / snapMinutes) * snapMinutes
    const hours = Math.max(startHour, Math.min(endHour - 1, Math.floor(snapped / 60)))
    const minutes = snapped % 60
    return { hours, minutes }
  }, [startHour, endHour, hourHeight])

  const resolveOverlaps = useCallback((slots: CalendarSlot[]): SlotWithPosition[] => {
    if (slots.length === 0) return []

    const sorted = [...slots].sort((a, b) => {
      const cmp = a.startTime.localeCompare(b.startTime)
      if (cmp !== 0) return cmp
      return b.endTime.localeCompare(a.endTime) // longer first
    })

    // Group overlapping events
    const groups: number[][] = []
    let currentGroup: number[] = []
    let groupEnd = ''

    for (let i = 0; i < sorted.length; i++) {
      if (currentGroup.length === 0 || sorted[i].startTime < groupEnd) {
        currentGroup.push(i)
        if (sorted[i].endTime > groupEnd) groupEnd = sorted[i].endTime
      } else {
        groups.push(currentGroup)
        currentGroup = [i]
        groupEnd = sorted[i].endTime
      }
    }
    if (currentGroup.length > 0) groups.push(currentGroup)

    const positions: SlotWithPosition[] = new Array(sorted.length)

    for (const group of groups) {
      const columnEnds: string[] = []

      for (const idx of group) {
        const slot = sorted[idx]
        const pos = getSlotPosition(slot)

        let col = 0
        for (; col < columnEnds.length; col++) {
          if (columnEnds[col] <= slot.startTime) break
        }
        if (col === columnEnds.length) columnEnds.push('')
        columnEnds[col] = slot.endTime

        positions[idx] = { ...slot, ...pos, column: col, totalColumns: 0 }
      }

      for (const idx of group) {
        positions[idx].totalColumns = columnEnds.length
      }
    }

    return positions
  }, [getSlotPosition])

  const timeLabels = useMemo(() => {
    const labels: number[] = []
    for (let h = startHour; h < endHour; h++) labels.push(h)
    return labels
  }, [startHour, endHour])

  return { hourHeight, totalHours, totalHeight, getSlotPosition, pixelToTime, resolveOverlaps, timeLabels }
}
