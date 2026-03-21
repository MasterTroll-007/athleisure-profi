package com.fitness.controller

import com.fitness.repository.ReservationRepository
import com.fitness.repository.UserRepository
import com.fitness.repository.CreditTransactionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.lang.management.ManagementFactory
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@RestController
@RequestMapping("/api/monitor")
class MonitorController(
    private val userRepository: UserRepository,
    private val reservationRepository: ReservationRepository,
    private val creditTransactionRepository: CreditTransactionRepository,
    @Value("\${monitor.username}") private val monitorUsername: String,
    @Value("\${monitor.password}") private val monitorPassword: String
) {
    private fun checkAuth(authHeader: String?): Boolean {
        if (authHeader == null || !authHeader.startsWith("Basic ")) return false
        val decoded = String(Base64.getDecoder().decode(authHeader.substring(6)))
        val parts = decoded.split(":", limit = 2)
        return parts.size == 2 && parts[0] == monitorUsername && parts[1] == monitorPassword
    }

    @GetMapping("/dashboard", produces = [MediaType.TEXT_HTML_VALUE])
    fun dashboard(@RequestHeader("Authorization", required = false) auth: String?): ResponseEntity<String> {
        if (!checkAuth(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", "Basic realm=\"Server Monitor\"")
                .body("Unauthorized")
        }

        val runtime = Runtime.getRuntime()
        val mb = 1024 * 1024
        val totalMem = runtime.totalMemory() / mb
        val freeMem = runtime.freeMemory() / mb
        val usedMem = totalMem - freeMem
        val maxMem = runtime.maxMemory() / mb

        val bean = ManagementFactory.getRuntimeMXBean()
        val uptime = Duration.ofMillis(bean.uptime)
        val uptimeStr = "${uptime.toDays()}d ${uptime.toHours() % 24}h ${uptime.toMinutes() % 60}m"

        val threadCount = ManagementFactory.getThreadMXBean().threadCount
        val cpuCount = runtime.availableProcessors()

        val osBean = ManagementFactory.getOperatingSystemMXBean()
        val loadAvg = osBean.systemLoadAverage

        val today = LocalDate.now()
        val zone = ZoneId.of("Europe/Prague")
        val totalUsers = userRepository.count()
        val totalAdmins = userRepository.countByRole("admin")
        val todayReservations = reservationRepository.countByDateRange(today, today)
        val weekReservations = reservationRepository.countByDateRange(today.minusDays(7), today)

        val memPercent = if (maxMem > 0) (usedMem * 100 / maxMem) else 0
        val memColor = when { memPercent > 85 -> "#ef4444"; memPercent > 60 -> "#f59e0b"; else -> "#10b981" }

        val html = """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="refresh" content="30">
<title>Server Monitor - DomiFit</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #0f172a; color: #e2e8f0; padding: 20px; }
  h1 { font-size: 1.5rem; margin-bottom: 20px; color: #818cf8; }
  h2 { font-size: 1rem; color: #94a3b8; margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.05em; }
  .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; margin-bottom: 24px; }
  .card { background: #1e293b; border-radius: 12px; padding: 20px; border: 1px solid #334155; }
  .stat { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #334155; }
  .stat:last-child { border-bottom: none; }
  .stat-label { color: #94a3b8; font-size: 0.875rem; }
  .stat-value { font-weight: 600; font-size: 1.1rem; }
  .bar { height: 8px; background: #334155; border-radius: 4px; overflow: hidden; margin-top: 4px; }
  .bar-fill { height: 100%; border-radius: 4px; transition: width 0.3s; }
  .badge { display: inline-block; padding: 2px 8px; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; }
  .badge-green { background: #065f46; color: #6ee7b7; }
  .badge-yellow { background: #78350f; color: #fcd34d; }
  .badge-red { background: #7f1d1d; color: #fca5a5; }
  .timestamp { text-align: center; color: #64748b; font-size: 0.75rem; margin-top: 16px; }
</style>
</head>
<body>
<h1>Server Monitor</h1>

<div class="grid">
  <div class="card">
    <h2>JVM</h2>
    <div class="stat">
      <span class="stat-label">Uptime</span>
      <span class="stat-value">$uptimeStr</span>
    </div>
    <div class="stat">
      <span class="stat-label">Heap Used / Max</span>
      <span class="stat-value" style="color:$memColor">${usedMem}MB / ${maxMem}MB</span>
    </div>
    <div class="bar"><div class="bar-fill" style="width:${memPercent}%; background:$memColor"></div></div>
    <div class="stat">
      <span class="stat-label">Threads</span>
      <span class="stat-value">$threadCount</span>
    </div>
    <div class="stat">
      <span class="stat-label">CPUs</span>
      <span class="stat-value">$cpuCount</span>
    </div>
    <div class="stat">
      <span class="stat-label">Load Average</span>
      <span class="stat-value">${"%.2f".format(loadAvg)}</span>
    </div>
  </div>

  <div class="card">
    <h2>Application</h2>
    <div class="stat">
      <span class="stat-label">Status</span>
      <span class="badge badge-green">HEALTHY</span>
    </div>
    <div class="stat">
      <span class="stat-label">Java</span>
      <span class="stat-value">${System.getProperty("java.version")}</span>
    </div>
    <div class="stat">
      <span class="stat-label">OS</span>
      <span class="stat-value">${osBean.name} ${osBean.arch}</span>
    </div>
    <div class="stat">
      <span class="stat-label">Spring Boot</span>
      <span class="stat-value">${org.springframework.boot.SpringBootVersion.getVersion()}</span>
    </div>
  </div>

  <div class="card">
    <h2>Database</h2>
    <div class="stat">
      <span class="stat-label">Total Users</span>
      <span class="stat-value">$totalUsers</span>
    </div>
    <div class="stat">
      <span class="stat-label">Admins</span>
      <span class="stat-value">$totalAdmins</span>
    </div>
    <div class="stat">
      <span class="stat-label">Clients</span>
      <span class="stat-value">${totalUsers - totalAdmins}</span>
    </div>
  </div>

  <div class="card">
    <h2>Reservations</h2>
    <div class="stat">
      <span class="stat-label">Today</span>
      <span class="stat-value">$todayReservations</span>
    </div>
    <div class="stat">
      <span class="stat-label">Last 7 days</span>
      <span class="stat-value">$weekReservations</span>
    </div>
  </div>
</div>

<div class="card" style="margin-bottom:16px">
  <h2>Actuator Endpoints</h2>
  <div style="display:flex; gap:8px; flex-wrap:wrap; margin-top:8px;">
    <a href="/api/monitor/health" style="color:#818cf8; text-decoration:none; padding:4px 12px; background:#334155; border-radius:6px; font-size:0.875rem;">/health</a>
    <a href="/api/monitor/metrics" style="color:#818cf8; text-decoration:none; padding:4px 12px; background:#334155; border-radius:6px; font-size:0.875rem;">/metrics</a>
    <a href="/api/monitor/metrics/jvm.memory.used" style="color:#818cf8; text-decoration:none; padding:4px 12px; background:#334155; border-radius:6px; font-size:0.875rem;">/metrics/jvm.memory</a>
    <a href="/api/monitor/metrics/http.server.requests" style="color:#818cf8; text-decoration:none; padding:4px 12px; background:#334155; border-radius:6px; font-size:0.875rem;">/metrics/http.requests</a>
    <a href="/api/monitor/info" style="color:#818cf8; text-decoration:none; padding:4px 12px; background:#334155; border-radius:6px; font-size:0.875rem;">/info</a>
    <a href="/api/monitor/loggers" style="color:#818cf8; text-decoration:none; padding:4px 12px; background:#334155; border-radius:6px; font-size:0.875rem;">/loggers</a>
  </div>
</div>

<p class="timestamp">Auto-refresh every 30s &bull; ${java.time.LocalDateTime.now(zone).format(java.time.format.DateTimeFormatter.ofPattern("d.M.yyyy HH:mm:ss"))} CET</p>
</body>
</html>
        """.trimIndent()

        return ResponseEntity.ok(html)
    }
}
