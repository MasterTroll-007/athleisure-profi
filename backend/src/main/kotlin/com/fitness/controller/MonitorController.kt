package com.fitness.controller

import com.fitness.repository.ReservationRepository
import com.fitness.repository.UserRepository
import com.fitness.repository.CreditTransactionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.annotation.PostConstruct
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/monitor")
class MonitorController(
    private val userRepository: UserRepository,
    private val reservationRepository: ReservationRepository,
    private val creditTransactionRepository: CreditTransactionRepository,
    @Value("\${monitor.username}") private val monitorUsername: String,
    @Value("\${monitor.password}") private val monitorPassword: String
) {
    // Server-side metrics history — 1 sample/min, kept for 7 days (10080 samples)
    private val MAX_HISTORY = 10080
    private data class MetricSample(
        val timestamp: Long,
        val heapUsedMB: Int,
        val ramUsedMB: Int,
        val cpuLoad: Double,
        val threads: Int,
        val diskUsedPct: Int
    )
    private val metricsHistory = ConcurrentLinkedDeque<MetricSample>()

    @PostConstruct
    fun startMetricsCollection() {
        val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "metrics-collector").apply { isDaemon = true }
        }
        scheduler.scheduleAtFixedRate({
            try { collectMetricSample() } catch (_: Exception) {}
        }, 0, 60, TimeUnit.SECONDS)
    }

    private fun collectMetricSample() {
        val runtime = Runtime.getRuntime()
        val heapUsed = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt()
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        val cpuLoad = osBean.systemLoadAverage
        val threads = ManagementFactory.getThreadMXBean().threadCount
        val ramUsed = try {
            val sun = osBean as com.sun.management.OperatingSystemMXBean
            ((sun.totalMemorySize - sun.freeMemorySize) / (1024 * 1024)).toInt()
        } catch (_: Exception) { 0 }
        val diskPct = try {
            val root = File("/")
            if (root.totalSpace > 0) ((root.totalSpace - root.freeSpace) * 100 / root.totalSpace).toInt() else 0
        } catch (_: Exception) { 0 }

        metricsHistory.addLast(MetricSample(
            Instant.now().toEpochMilli(), heapUsed, ramUsed, cpuLoad, threads, diskPct
        ))
        while (metricsHistory.size > MAX_HISTORY) metricsHistory.pollFirst()
    }

    private fun checkAuth(authHeader: String?): Boolean {
        if (authHeader == null || !authHeader.startsWith("Basic ")) return false
        val decoded = try {
            String(Base64.getDecoder().decode(authHeader.substring(6)), StandardCharsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            return false
        }
        val parts = decoded.split(":", limit = 2)
        if (parts.size != 2) return false
        return constantTimeEquals(parts[0], monitorUsername) &&
            constantTimeEquals(parts[1], monitorPassword)
    }

    private fun constantTimeEquals(candidate: String, expected: String): Boolean =
        MessageDigest.isEqual(
            candidate.toByteArray(StandardCharsets.UTF_8),
            expected.toByteArray(StandardCharsets.UTF_8)
        )

    private fun requireAuth(auth: String?): ResponseEntity<String>? {
        if (!checkAuth(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", "Basic realm=\"Server Monitor\"")
                .body("Unauthorized")
        }
        return null
    }

    @GetMapping("/stats")
    fun stats(@RequestHeader("Authorization", required = false) auth: String?): ResponseEntity<Any> {
        requireAuth(auth)?.let { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build() }

        val runtime = Runtime.getRuntime()
        val bean = ManagementFactory.getRuntimeMXBean()
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        val memBean = ManagementFactory.getMemoryMXBean()
        val gcBeans = ManagementFactory.getGarbageCollectorMXBeans()
        val threadBean = ManagementFactory.getThreadMXBean()

        val today = LocalDate.now()

        val gcCount = gcBeans.sumOf { it.collectionCount }
        val gcTime = gcBeans.sumOf { it.collectionTime }

        val hikariMBean = try {
            val server = ManagementFactory.getPlatformMBeanServer()
            val name = javax.management.ObjectName("com.zaxxer.hikari:type=Pool (HikariPool-1)")
            mapOf(
                "active" to server.getAttribute(name, "ActiveConnections"),
                "idle" to server.getAttribute(name, "IdleConnections"),
                "total" to server.getAttribute(name, "TotalConnections"),
                "waiting" to server.getAttribute(name, "ThreadsAwaitingConnection")
            )
        } catch (_: Exception) {
            mapOf("active" to -1, "idle" to -1, "total" to -1, "waiting" to -1)
        }

        val monthlyStats = (0 until 6).map { i ->
            val monthStart = today.minusMonths(i.toLong()).withDayOfMonth(1)
            val monthEnd = monthStart.plusMonths(1).minusDays(1)
            mapOf(
                "month" to monthStart.toString(),
                "total" to reservationRepository.countByDateRange(monthStart, monthEnd),
                "completed" to reservationRepository.countByStatusAndDateBetween("completed", monthStart, monthEnd),
                "noShow" to reservationRepository.countByStatusAndDateBetween("no_show", monthStart, monthEnd)
            )
        }.reversed()

        // Disk info for all roots
        val diskInfo = File.listRoots().map { root ->
            mapOf(
                "path" to root.absolutePath,
                "total" to root.totalSpace,
                "free" to root.freeSpace,
                "usable" to root.usableSpace,
                "used" to (root.totalSpace - root.freeSpace)
            )
        }

        // System RAM via com.sun.management
        val systemRam = try {
            val sunOsBean = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
            mapOf(
                "total" to sunOsBean.totalMemorySize,
                "free" to sunOsBean.freeMemorySize,
                "used" to (sunOsBean.totalMemorySize - sunOsBean.freeMemorySize)
            )
        } catch (_: Exception) {
            mapOf("total" to -1L, "free" to -1L, "used" to -1L)
        }

        return ResponseEntity.ok(mapOf(
            "disk" to diskInfo,
            "ram" to systemRam,
            "jvm" to mapOf(
                "heapUsed" to runtime.totalMemory() - runtime.freeMemory(),
                "heapMax" to runtime.maxMemory(),
                "nonHeapUsed" to memBean.nonHeapMemoryUsage.used,
                "threads" to threadBean.threadCount,
                "daemonThreads" to threadBean.daemonThreadCount,
                "peakThreads" to threadBean.peakThreadCount,
                "cpus" to runtime.availableProcessors(),
                "loadAvg" to osBean.systemLoadAverage,
                "uptime" to bean.uptime,
                "gcCount" to gcCount,
                "gcTime" to gcTime
            ),
            "app" to mapOf(
                "java" to System.getProperty("java.version"),
                "os" to "${osBean.name} ${osBean.arch}",
                "springBoot" to org.springframework.boot.SpringBootVersion.getVersion(),
                "status" to "UP"
            ),
            "db" to mapOf(
                "totalUsers" to userRepository.count(),
                "admins" to userRepository.countByRole("admin"),
                "pool" to hikariMBean
            ),
            "reservations" to mapOf(
                "today" to reservationRepository.countByDateRange(today, today),
                "week" to reservationRepository.countByDateRange(today.minusDays(7), today),
                "month" to reservationRepository.countByDateRange(today.minusDays(30), today),
                "monthly" to monthlyStats
            )
        ))
    }

    @GetMapping("/history")
    fun history(
        @RequestHeader("Authorization", required = false) auth: String?,
        @RequestParam("hours", defaultValue = "24") hours: Int
    ): ResponseEntity<Any> {
        requireAuth(auth)?.let { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build() }
        val cutoff = Instant.now().minusSeconds(hours.toLong() * 3600).toEpochMilli()
        val filtered = metricsHistory.filter { it.timestamp >= cutoff }
        // Downsample if too many points (max ~500 for chart performance)
        val step = if (filtered.size > 500) filtered.size / 500 else 1
        val sampled = filtered.filterIndexed { i, _ -> i % step == 0 }
        return ResponseEntity.ok(sampled.map { s ->
            mapOf(
                "t" to s.timestamp,
                "heap" to s.heapUsedMB,
                "ram" to s.ramUsedMB,
                "cpu" to s.cpuLoad,
                "threads" to s.threads,
                "disk" to s.diskUsedPct
            )
        })
    }


    private fun monitorDashboardHtml(): String =
        ClassPathResource("monitor-dashboard.html").inputStream.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        }

    @GetMapping("/dashboard", produces = [MediaType.TEXT_HTML_VALUE])
    fun dashboard(
        @RequestHeader("Authorization", required = false) auth: String?
    ): ResponseEntity<String> {
        requireAuth(auth)?.let {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", "Basic realm=\"Server Monitor\"")
                .contentType(MediaType.TEXT_HTML)
                .body("<h1>401 Unauthorized</h1>")
        }

        val html = monitorDashboardHtml()


        // Override CSP for dashboard - allows inline scripts/styles for the monitoring SPA
        return ResponseEntity.ok()
            .header("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'unsafe-inline'; " +
                "style-src 'unsafe-inline'; " +
                "img-src 'self' data:; " +
                "connect-src 'self'; " +
                "frame-ancestors 'none'")
            .body(html)
    }
}
