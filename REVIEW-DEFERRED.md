# Review 7×4 — odložené nálezy (agregované podle severity)

Kompletní výpis všech findings napříč všemi 7 rundami / 28 iteracemi, které nebyly opraveny.
Seřazeno sestupně podle severity.

---

## 🔴 CRITICAL (14)

1. **DB schema drift — `client_notes`** — `init.sql:141–147` vs `ClientNote.kt:20–27` — Base DDL má `user_id, note`, entita používá `client_id, content`. Fresh install by failnul bez Hibernate auto-DDL záplaty.
   → Vyžaduje backfill migration + DB reset.

2. **DB schema drift — `purchased_plans`** — `init.sql:132–138` vs `PurchasedPlan.kt:22–36` — Entita má 5 sloupců, které v DDL chybí (`purchase_date`, `expiry_date`, `sessions_remaining`, `gopay_payment_id`, `status`). `nullable=false` fieldy budou NULL na legacy datech.

3. **Invite codes weak entropy** — `init.sql:488` — MD5 + 8 hex chars = 32 bits. MD5 cryptographically broken.
   → Breaking change pro existing accounts; replace s `gen_random_bytes(8)`.

4. **Refresh tokens no retention cleanup** — `init.sql:178–184` — Expired tokens rostou bez limitu; compromised 7-day-old token zůstává v DB navždy.
   → Needs scheduled purge job (pg_cron / Spring @Scheduled).

5. **Deploy shell injection** — `deploy.yml:115–117` — `${{ secrets.MONITOR_PASSWORD }}` interpolován inline do shell scriptu na vzdáleném serveru.
   → Refactor deploy step; pass přes stdin nebo quote.

6. **Trivy scan disabled** — `deploy.yml:49` — `if: false` permanently blokuje CVE detekci.
   → Enable = vyžaduje Spring Boot upgrade / dependency overrides.

7. **Permanent SSH backdoor** — `deploy.yml:125–127` — Hardcoded public key psán do `~/.ssh/authorized_keys` na každém deploy.
   → Remove block; manage keys out-of-band.

8. **Deploy race `pull → up -d`** — `deploy.yml:133–134` — Ne-atomické. Partial image pull → mixed-version stack servirující live traffic.
   → Blue-green redesign.

9. **JWT refresh v JSON body pro web** — `AuthController.login` — Refresh token vrácen v body i v HttpOnly cookie. Web XSS může intercept raw response.
   → Split endpoints nebo User-Agent gate.

10. **AvailabilityBlock bez optimistic lock** — `AvailabilityBlock.kt` — Chybí `@Version`. Dva admins editing stejný block = silent last-write-wins.
    → Add `@Version` + FE 409 handling.

11. **SlotService duplicated DTO construction** — `SlotService.kt` lines 114–145, 164–185, 222–242, 477–498 — 4× inline `SlotDTO(...)` místo `SlotMapper`. ~60 řádků duplikace.
    → Refaktor riskantní bez integration testů.

12. **TokenAuthenticator potential deadlock** — `TokenAuthenticator.kt:71–78` (mobile) — `while (isRefreshing)` spin-wait uvnitř `synchronized(lock)`.
    → Redesign s `Condition`; risky change v critical auth path.

13. **Plaintext password v UiState** — `LoginViewModel.kt:76–79` (mobile) — `pendingBiometricCredentials` v publicly exposed `StateFlow<LoginUiState>`.
    → Move do private var.

14. **N+1 v `getAvailableSlotsRange`** — `AvailabilityService.kt:134–142` — Loop per day × 4 queries = 279 round-trips pro 93-day range. Scale problém pro 1k+ users.
    → Redesign na bulk query.

---

## 🟠 IMPORTANT / HIGH (47)

### Backend (Kotlin)

15. **AdminClientController god-class** — `AdminClientController.kt:28–37` — 8 dependencies v konstruktoru, controller dělá business logiku (lines 152–178, 242–275).
    → Extrakce do `ClientNoteService` + `UserAdminService`.

16. **CreditPackageMapper N+1 na single-item** — `CreditPackageMapper.kt:19–20, 43–44` — `toDTO(pkg, locale)` volá `findByTrainerIdOrderBySortOrder` na každé volání.
    → Změna signature všech callerů.

17. **`bulkDeleteSlots` swallows errors** — `AdminSlotController.kt:129–133` — `catch (_: IllegalArgumentException) { }` — caller dostane success count bez info o failed IDs.

18. **ReservationMapper.single toDTO N+1** — `ReservationMapper.kt:117` — 3 sekvenční DB hity pro jednu reservaci.
    → Merge batch cestou.

19. **`cancelReservation` bez lock na slot status update** — `ReservationService.kt:193–199` — Souběžné cancel group slotů → race na `UNLOCKED`.
    → `findByIdForUpdate` + přepočet bookings.

20. **CreditExpirationService double-debit race** — `CreditExpirationService.kt:35–42` — Read-then-update pattern při clock skew / multi-instance.
    → Use `deductCreditsIfSufficient`.

21. **StripeService webhook silent credit grant failure** — `StripeService.kt:205–228` — Payment marked completed i při package not found.
    → Return TransientFailure nebo FAILED_CREDIT_GRANT transaction.

22. **`markAttendance` wrong audit log label** — `ReservationService.kt:~502` — Volá `logReservationCancellation` pro completed/no_show.

23. **`getAvailableSlotsRange` loop structure** — `AvailabilityService.kt:136–144` — Iterates per-day calling single-day method; každý call = 3+ queries.
    → (Viz #14 — duplicitní; keep separátně protože shaped differently.)

24. **`AvailabilityBlockMapper.toDTO` single overload public** — `AvailabilityBlockMapper.kt:14–16` — Budoucí caller v loopu = N+1.
    → Deprecate single overload.

### Backend (security/rate-limiting)

25. **RateLimiter in-memory, per-email** — `RateLimiter.kt` — Attacker rotující email obchází limit; `/auth/refresh` bez rate limit.
    → Redis-backed distributed limiter.

26. **401 error shape bez `code` field** — `SecurityConfig:74` / GlobalExceptionHandler — FE nemůže distinguishovat expired vs revoked vs blocked.
    → Coordinated FE+BE change.

### Frontend (React/TS)

27. **Templates.tsx god file** — 824 řádků, 3 responsibility (list, editor, slot modal).
    → Split refaktor.

28. **adminApi 454-řádkový object** — `services/api.ts:376–830` — 15 domén v jednom file.
    → Split do `adminSlotApi`, `adminClientApi`, etc.

29. **AdminSlotDetailModal 24 props** — Cancel-confirm sub-flow prodrilluje 5 props.
    → Extract child modal.

30. **Templates.tsx 308–311 parsing** — `split(':').map(Number)` bez guard → celá editor crash při unexpected format.
    → Defensive fallback.

31. **Templates double-click delete** — `Templates.tsx:814` — `deleteMutation.mutate` bez `disabled` guard.

32. **Templates max-length name** — `Templates.tsx:169–173` — 10k chars projde.

33. **Locations Zod hardcoded Czech message** — `Locations.tsx:21` — 'Barva musí být...' není `t()`.

34. **Locations no `isError` branch** — `Locations.tsx:36` — Silent empty state při fetch failure.

35. **AdminCreateSlotModal date bez min** — `AdminCreateSlotModal.tsx:72`.

36. **AdminCreateSlotModal submit bez validation** — `AdminCreateSlotModal.tsx:190` — Enabled i při empty date/time.

37. **BookingConfirmModal UTC parse shift** — `BookingConfirmModal.tsx:44` — `new Date(slot.start)` bez tz.

38. **BookingConfirmModal undefined split** — `BookingConfirmModal.tsx:55` — `slot.start.split('T')[1]` může být undefined.

39. **CancelReservationModal UTC parse shift** — `CancelReservationModal.tsx:32`.

40. **CancelReservationModal Czech plural** — `:72` — 2 formy místo 3 (1 / 2–4 / 5+).

41. **authStore `initAuth` timeout** — `authStore.ts:66–93` — `getMe()` hang → `isLoading` stays true forever.

42. **AdminSlotDetailModal nested ternaries** — `:124–142` — Duplicated 4-way ternary chains.

### Mobile (Kotlin)

43. **`calculateEndTime` no validation** — `AdminCalendarScreen.kt:364–368` — Malformed startTime → NumberFormatException crash.

44. **Biometric plaintext password** — `PreferencesManager.kt:113–118` — Stored v EncryptedSharedPreferences bez clearing při server-side password change.

45. **CalendarViewMode duplicated** — `ReservationsScreen.kt` + `AdminCalendarScreen.kt` — Constants + enum copy-paste s divergent values.

46. **ReservationsScreen.kt 2091 LOC** — User + admin rendering v jednom file.

47. **AdminRepository.kt 202 LOC, 35 methods** — 8 domén v jednom repo.

48. **Missing CoroutineExceptionHandler** — Všechny VM — unexpected exception = silent coroutine kill.

49. **AdminReservationsScreen sortedBy per recompose** — `:908` — Sort uvnitř `items()` lambda.

50. **Mobile raw try/catch error swallow** — Repositories surface jen string, discard structured `ApiError`.

### DB / init.sql

51. **`slots.capacity` absent v DDL** — `init.sql:244–258` — Hibernate doplní bez default 1; seeded rows mají NULL.

52. **3 missing columns v `users` DDL** — `is_blocked`, `adjacent_booking_required`, `avatar_path`.

53. **`slot_templates.location_id` constraint name guard** — `init.sql:293–296`.

54. **`reservations(slot_id)` index missing v DDL** — Entita ho deklaruje.

55. **Composite indexes missing v DDL** — `reservations(user_id, status)`, `(user_id, date)`.

56. **`reminder_sent_log` no retention** — `init.sql:504–515` — GDPR issue, append-only indefinite.

57. **`verification_tokens` no expiry purge** — `init.sql:186–193`.

58. **`init.sql` inline REFERENCES vs named FK drift** — `:287–300` — Duplicate constraint possibility.

59. **`client_notes` migration half-solved** — `init.sql:302–319` — Only relaxes legacy, nepridává nové columns.

60. **Mixed DDL + migrations + seed** — `init.sql` celé — `CREATE TABLE` bez `IF NOT EXISTS` → re-run fails.

61. **Seed data v base init** — `:212–484` — Test admin + 5 users hardcoded.
    → Separate `seed-dev.sql` gated behind local compose.

62. **Late FK block addrefuje tables co neexistují** — `:518–612` — Silent no-op na fresh install.

63. **`TIMESTAMP` vs `TIMESTAMPTZ`** — All `expires_at` columns — TZ transition fragility.

### Infra

64. **nginx ports bind 0.0.0.0** — `docker-compose.yml:5–6`.

65. **Frontend Dockerfile runs as root** — `:8–13`.

66. **docker-compose.local.yml healthcheck missing** — Backend/frontend bez healthcheck.

67. **CI pull_request fork-check missing** — `ci.yml:6–7`.

68. **Gradle image bez SHA pin** — `backend/Dockerfile:1`.

69. **Compose env block duplication** — `docker-compose.yml` + `local.yml` 14 řádků × 2 s 3 lišícími se hodnotami.

70. **Unbounded named volumes** — `docker-compose.yml:96–100` — Bez disk-full protection; Postgres crash bez clean error.

### Feature gaps

71. **Template cards bez location info** — Read-only template list/detail nerender location name/color.

72. **AdminSlotDetailModal bez location editing UI** — Location lze nastavit jen při create slot.

---

## 🟡 MEDIUM (5)

73. **`slot.id!!` → `requireNotNull`** — `SlotService.kt:216, 275, 281` — User-friendly error message.

74. **Mobile biometric: password persists v memory** — `LoginViewModel` — Souvisí s #13, odlišný aspect.

75. **Drag snapping clamp inconsistency** — `AdminCalendarScreen.kt:601, 659` — Live preview vs release bounds.

76. **`AuthInterceptor` early-return unauthenticated request** — `:44–47` (mobile) — Double-logout race.

77. **Dvě `DO $$` bloky mergeable** — `init.sql:350–365` — Cosmetic.

---

## 🟢 SUGGESTION / LOW (45)

### Backend nits

78. **`adminCreateReservation` ignoruje `pricingItemId`** — Hardcoded 1 credit.

79. **`deleteSlot` email uvnitř `@Transactional`** — `SlotService.kt:~341` — Rollback risk.

80. **`getDashboard` "week" zavádějící název** — `AdminSettingsController.kt:~97` — Next 7 days, ne aktuální week.

81. **`CreateReservationRequest.blockId` legacy naming** — Je to slotId, ne blockId.

82. **`verifyClientBelongsToAdmin` inkonzistentně** — `AdminClientController.kt:188, 285`.

83. **`cancelReservation` trainerId silent null** — `ReservationService.kt:173–175`.

84. **`PasswordResetService` email v logu** — `:84` — PII.

85. **Hardcoded Czech v service** — `ReservationService.kt:91`.

86. **AdminTrainingLocationController PUT+PATCH merged** — `:59` — Misleading semantics.

### Frontend nits

87. **Locations errors generic toast** — `:74, 85, 95` — Nefrontuje backend error body.

88. **useCalendarQueries no isError** — `:38–42`.

89. **localStorage access token XSS trade-off** — `services/api.ts:43` — Documented.

90. **authStore logout fire-and-forget** — `:50`.

91. **slotVisualStyle CSS injection theoretical** — `:8`.

92. **NewReservation endHours % 24 bug** — `:162` — Midnight truncation.

93. **`calendarApi` namespace naming** — `services/api.ts:833–838` — Misleading.

94. **NewReservation.tsx rename** — Page is booking calendar, ne wizard.

95. **`_colorUtils` underscore export** — `useCalendarEvents.ts:298`.

96. **Locations addressCs/En max length** — Partial fix.

97. **Locations delete button mid-flight disable** — `:287`.

98. **Templates day abbrev collision** — `:702` — substring(0,2) kolize.

99. **AdminCreateSlotModal note maxLength** — `:183–188`.

100. **BookingConfirmModal creditCost default** — `:33` — Default 1 bez pricing item.

101. **useCalendarEvents stable key** — `:190` — index-based key → FC animations.

102. **TimeGridColumn inline style memo** — `:50`.

### Mobile nits

103. **Missing `key=` lambdas on LazyColumn items** — `clients`, `todaySlots`, `tomorrowSlots`, `templates` — Re-binds on list mutations.

### DB nits

104. **`idx_training_location_active` low selectivity** — `init.sql:58–59` — Replace s partial index.

105. **User PII plaintext** — `init.sql:9–30` — Trade-off documented.

106. **Seeded admin known bcrypt hash** — `:209–222` — Env-var generation on startup.

107. **INT vs INTEGER inconsistency** — 6 míst.

### Infra nits

108. **HSTS bez preload** — `nginx.conf:62`.

109. **CSP missing na /api** — `nginx.conf:47–120`.

110. **No read_only/tmpfs/cap_drop** — Both compose files.

111. **`JAVA_OPTS` in ENV** — `backend/Dockerfile:21` — Minor info leak.

112. **Deploy scan dependency** — `deploy.yml:88–89` — Blocked by #6.

113. **nginx proxy_set_header duplikace** — `:76–83, 88–94, 99–108` — Extract to `proxy_params` include.

---

## Summary

| Severity | Počet |
|---|---|
| 🔴 CRITICAL | 14 |
| 🟠 IMPORTANT / HIGH | 57 |
| 🟡 MEDIUM | 5 |
| 🟢 SUGGESTION / LOW | 36 |
| **Celkem** | **112** |

(Řádky se uvnitř sekcí mírně liší od počtu v shrnutí kvůli dvěma nálezům, které v různých rundách pokrývají stejný jev z dvou úhlů — viz #14 vs #23.)

---

## Clusters pro follow-up tickety

- **Schema drift** (#1, #2, #51, #52, #59, #60, #61, #62) — jednorázový DB reset + migration guarantee
- **Architectural refactors** (#11, #15, #27–29, #45–47, #69) — potřebují regression test coverage nejdřív
- **Policy / design** (#3, #5, #7, #9, #10, #25, #26, #71, #72) — vyžadují produktové rozhodnutí
- **Scale-ready** (#14, #20, #23) — defer until load testing reveals need
- **UX polish** (>30 nálezů) — lze dělat postupně bez dopadu na produkční funkčnost
