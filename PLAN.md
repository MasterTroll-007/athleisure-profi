# Admin Dashboard Analytics Implementation Plan

## Overview
Implement comprehensive analytics for the admin dashboard including revenue tracking, reservation trends, cancellation rates, and client metrics. All data must be trainer-scoped (filtered by authenticated admin's ID).

## Current State Analysis
- **AdminSettingsController.kt:86-98**: Basic dashboard with only `totalClients`, `todayReservations`, `weekReservations` - all global counts (NOT trainer-scoped)
- **Frontend Dashboard.tsx**: Simple stat cards and today's training list, no charts
- **No charting library** installed in frontend
- Repositories exist but lack trainer-scoped analytics queries

## Implementation Steps

### Phase 1: Backend - DTOs

**1.1 Create AnalyticsDTOs.kt** (`backend/src/main/kotlin/com/fitness/dto/AnalyticsDTOs.kt`)
```kotlin
data class DashboardAnalyticsDTO(
    val overview: OverviewStatsDTO,
    val revenueChart: List<ChartDataPointDTO>,
    val reservationTrends: List<ChartDataPointDTO>,
    val popularTimeSlots: List<TimeSlotPopularityDTO>,
    val packagePopularity: List<PackagePopularityDTO>,
    val clientMetrics: ClientMetricsDTO
)

data class OverviewStatsDTO(
    val totalClients: Long,
    val activeClients: Long,
    val todayReservations: Long,
    val weekReservations: Long,
    val monthReservations: Long,
    val cancellationRate: Double,
    val totalRevenue: Long,
    val monthRevenue: Long
)

data class ChartDataPointDTO(val label: String, val value: Long)
data class TimeSlotPopularityDTO(val hour: Int, val count: Long)
data class PackagePopularityDTO(val packageId: String, val packageName: String, val purchaseCount: Long, val revenue: Long)
data class ClientMetricsDTO(val newClientsThisMonth: Long, val retentionRate: Double, val averageCreditsPerClient: Double)
```

### Phase 2: Backend - Repository Queries

**2.1 UserRepository.kt** - Add trainer-scoped queries:
```kotlin
@Query("SELECT COUNT(u) FROM User u WHERE u.trainerId = :trainerId AND u.role = 'client'")
fun countClientsByTrainerId(trainerId: UUID): Long

@Query("""SELECT COUNT(DISTINCT r.userId) FROM Reservation r JOIN User u ON r.userId = u.id
    WHERE u.trainerId = :trainerId AND r.date >= :since AND r.status = 'confirmed'""")
fun countActiveClientsByTrainerId(trainerId: UUID, since: LocalDate): Long

@Query("SELECT COUNT(u) FROM User u WHERE u.trainerId = :trainerId AND u.role = 'client' AND u.createdAt >= :since")
fun countNewClientsByTrainerId(trainerId: UUID, since: Instant): Long
```

**2.2 ReservationRepository.kt** - Add analytics queries:
```kotlin
@Query("""SELECT COUNT(r) FROM Reservation r JOIN User u ON r.userId = u.id
    WHERE u.trainerId = :trainerId AND r.date BETWEEN :startDate AND :endDate AND r.status = 'confirmed'""")
fun countByTrainerAndDateRange(trainerId: UUID, startDate: LocalDate, endDate: LocalDate): Long

@Query("""SELECT COUNT(r) FROM Reservation r JOIN User u ON r.userId = u.id
    WHERE u.trainerId = :trainerId AND r.date BETWEEN :startDate AND :endDate AND r.status = 'cancelled'""")
fun countCancelledByTrainerAndDateRange(trainerId: UUID, startDate: LocalDate, endDate: LocalDate): Long

// Grouped by date for chart
@Query(nativeQuery = true, value = """
    SELECT r.date as date, COUNT(r.id) as count FROM reservations r
    JOIN users u ON r.user_id = u.id
    WHERE u.trainer_id = :trainerId AND r.date BETWEEN :startDate AND :endDate AND r.status = 'confirmed'
    GROUP BY r.date ORDER BY r.date""")
fun countByTrainerGroupedByDate(trainerId: UUID, startDate: LocalDate, endDate: LocalDate): List<Array<Any>>

// Popular time slots
@Query(nativeQuery = true, value = """
    SELECT EXTRACT(HOUR FROM r.start_time) as hour, COUNT(r.id) as count
    FROM reservations r JOIN users u ON r.user_id = u.id
    WHERE u.trainer_id = :trainerId AND r.status = 'confirmed'
    GROUP BY EXTRACT(HOUR FROM r.start_time) ORDER BY count DESC LIMIT 10""")
fun countPopularTimeSlotsByTrainer(trainerId: UUID): List<Array<Any>>
```

**2.3 CreditTransactionRepository.kt / StripePaymentRepository.kt** - Add revenue queries:
```kotlin
@Query(nativeQuery = true, value = """
    SELECT COALESCE(SUM(sp.amount), 0) FROM stripe_payments sp
    JOIN users u ON sp.user_id = u.id
    WHERE u.trainer_id = :trainerId AND sp.status = 'completed' AND sp.created_at >= :since""")
fun sumRevenueByTrainerSince(trainerId: UUID, since: Instant): Long

@Query(nativeQuery = true, value = """
    SELECT DATE(sp.created_at) as date, SUM(sp.amount) as total FROM stripe_payments sp
    JOIN users u ON sp.user_id = u.id
    WHERE u.trainer_id = :trainerId AND sp.status = 'completed' AND sp.created_at BETWEEN :startDate AND :endDate
    GROUP BY DATE(sp.created_at) ORDER BY date""")
fun sumRevenueByTrainerGroupedByDate(trainerId: UUID, startDate: Instant, endDate: Instant): List<Array<Any>>
```

### Phase 3: Backend - Service Layer

**3.1 Create AdminAnalyticsService.kt** (`backend/src/main/kotlin/com/fitness/service/AdminAnalyticsService.kt`)
- Methods:
  - `getAnalytics(trainerId: UUID, period: String): DashboardAnalyticsDTO`
  - `getOverviewStats(trainerId, startDate, endDate): OverviewStatsDTO`
  - `getRevenueChart(trainerId, startDate, endDate): List<ChartDataPointDTO>`
  - `getReservationTrends(trainerId, startDate, endDate): List<ChartDataPointDTO>`
  - `getPopularTimeSlots(trainerId): List<TimeSlotPopularityDTO>`
  - `getPackagePopularity(trainerId, startDate, endDate): List<PackagePopularityDTO>`
  - `getClientMetrics(trainerId): ClientMetricsDTO`
  - `exportToCsv(trainerId, period): String`

### Phase 4: Backend - Controller Endpoint

**4.1 Update AdminSettingsController.kt** - Add new endpoints:
```kotlin
@GetMapping("/analytics")
fun getAnalytics(
    @AuthenticationPrincipal principal: UserPrincipal,
    @RequestParam(defaultValue = "month") period: String
): ResponseEntity<DashboardAnalyticsDTO>

@GetMapping("/analytics/export")
fun exportAnalytics(
    @AuthenticationPrincipal principal: UserPrincipal,
    @RequestParam(defaultValue = "month") period: String
): ResponseEntity<ByteArray>  // CSV download
```

### Phase 5: Frontend - Dependencies and Types

**5.1 Install Recharts**
```bash
cd frontend && npm install recharts @types/recharts
```

**5.2 Add TypeScript types** (`frontend/src/types/analytics.ts`)
```typescript
export interface DashboardAnalytics {
  overview: OverviewStats
  revenueChart: ChartDataPoint[]
  reservationTrends: ChartDataPoint[]
  popularTimeSlots: TimeSlotPopularity[]
  packagePopularity: PackagePopularity[]
  clientMetrics: ClientMetrics
}
// ... rest of interfaces
```

**5.3 Add API methods** (`frontend/src/services/api.ts`)
```typescript
getAnalytics: (period: string) => Promise<DashboardAnalytics>
exportAnalytics: (period: string) => Promise<Blob>
```

### Phase 6: Frontend - Components

**6.1 Create chart components** (`frontend/src/components/charts/`)
- `RevenueChart.tsx` - Line chart for revenue over time
- `ReservationTrendsChart.tsx` - Bar chart for reservation counts
- `PopularTimeSlotsChart.tsx` - Bar chart for popular hours
- `PackagePopularityChart.tsx` - Pie/bar chart for packages

**6.2 Create Analytics page** (`frontend/src/pages/admin/Analytics.tsx`)
- Period selector (week/month/year)
- Overview stat cards
- Revenue chart
- Reservation trends chart
- Popular time slots
- Package popularity
- Export CSV button

**6.3 Update routing** (`frontend/src/App.tsx`)
- Add `/admin/analytics` route

**6.4 Update Dashboard.tsx**
- Add navigation link to Analytics page
- Keep existing quick stats

### Phase 7: i18n Updates

**7.1 Add translations** to `en.json` and `cs.json`:
```json
{
  "admin": {
    "analytics": "Analytics",
    "revenue": "Revenue",
    "totalRevenue": "Total Revenue",
    "monthlyRevenue": "Monthly Revenue",
    "reservationTrends": "Reservation Trends",
    "popularTimeSlots": "Popular Time Slots",
    "cancellationRate": "Cancellation Rate",
    "activeClients": "Active Clients",
    "newClients": "New Clients",
    "retentionRate": "Retention Rate",
    "exportCsv": "Export CSV",
    "period": "Period",
    "week": "Week",
    "month": "Month",
    "year": "Year"
  }
}
```

## Files Summary

### New Files (8):
1. `backend/src/main/kotlin/com/fitness/dto/AnalyticsDTOs.kt`
2. `backend/src/main/kotlin/com/fitness/service/AdminAnalyticsService.kt`
3. `frontend/src/types/analytics.ts`
4. `frontend/src/pages/admin/Analytics.tsx`
5. `frontend/src/components/charts/RevenueChart.tsx`
6. `frontend/src/components/charts/ReservationTrendsChart.tsx`
7. `frontend/src/components/charts/PopularTimeSlotsChart.tsx`
8. `frontend/src/components/charts/PackagePopularityChart.tsx`

### Modified Files (10):
1. `backend/src/main/kotlin/com/fitness/repository/UserRepository.kt` - add trainer-scoped queries
2. `backend/src/main/kotlin/com/fitness/repository/ReservationRepository.kt` - add analytics queries
3. `backend/src/main/kotlin/com/fitness/repository/CreditRepositories.kt` - add revenue queries (or StripePaymentRepository)
4. `backend/src/main/kotlin/com/fitness/controller/admin/AdminSettingsController.kt` - add analytics endpoints
5. `frontend/package.json` - add recharts dependency
6. `frontend/src/services/api.ts` - add analytics API methods
7. `frontend/src/types/api.ts` - add analytics types (or separate file)
8. `frontend/src/i18n/en.json` - add translations
9. `frontend/src/i18n/cs.json` - add translations
10. `frontend/src/App.tsx` - add analytics route

## Key Design Decisions

1. **Trainer Scoping**: All queries filter by `trainerId` from authenticated principal to ensure data isolation
2. **Date Range**: Support week/month/year periods with default of month
3. **Caching**: Consider `@Cacheable` for expensive aggregate queries (5-15 min TTL)
4. **CSV Export**: Generate server-side to handle large datasets

## Testing Considerations
- Test with trainers that have clients vs no clients
- Verify data isolation between trainers
- Test date range edge cases
- Test CSV export with Czech diacritics
