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
        val mb = 1024 * 1024
        val bean = ManagementFactory.getRuntimeMXBean()
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        val memBean = ManagementFactory.getMemoryMXBean()
        val gcBeans = ManagementFactory.getGarbageCollectorMXBeans()
        val threadBean = ManagementFactory.getThreadMXBean()

        val today = LocalDate.now()
        val zone = ZoneId.of("Europe/Prague")

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

        // Monthly reservation stats for chart
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

        return ResponseEntity.ok(mapOf(
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

    @GetMapping("/dashboard", produces = [MediaType.TEXT_HTML_VALUE])
    fun dashboard(@RequestHeader("Authorization", required = false) auth: String?): ResponseEntity<String> {
        requireAuth(auth)?.let { return it }

        val html = """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Server Monitor - DomiFit</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#0f172a;color:#e2e8f0;padding:20px}
h1{font-size:1.5rem;margin-bottom:20px;color:#818cf8;display:flex;align-items:center;gap:10px}
h1 .dot{width:10px;height:10px;border-radius:50%;background:#10b981;animation:pulse 2s infinite}
@keyframes pulse{0%,100%{opacity:1}50%{opacity:.4}}
h2{font-size:.85rem;color:#94a3b8;margin-bottom:10px;text-transform:uppercase;letter-spacing:.05em}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:16px;margin-bottom:20px}
.card{background:#1e293b;border-radius:12px;padding:20px;border:1px solid #334155}
.card-wide{grid-column:span 2}
.stat{display:flex;justify-content:space-between;align-items:center;padding:6px 0;border-bottom:1px solid #334155}
.stat:last-child{border-bottom:none}
.stat-label{color:#94a3b8;font-size:.8rem}
.stat-value{font-weight:600;font-size:1rem}
.bar{height:8px;background:#334155;border-radius:4px;overflow:hidden;margin-top:4px}
.bar-fill{height:100%;border-radius:4px;transition:width .5s}
.badge{display:inline-block;padding:2px 8px;border-radius:9999px;font-size:.7rem;font-weight:600}
.bg{background:#065f46;color:#6ee7b7}.by{background:#78350f;color:#fcd34d}.br{background:#7f1d1d;color:#fca5a5}
canvas{width:100%!important;height:200px!important;margin-top:8px}
.ts{text-align:center;color:#64748b;font-size:.7rem;margin-top:12px}
.tabs{display:flex;gap:4px;margin-bottom:12px;flex-wrap:wrap}
.tab{padding:4px 12px;background:#334155;border:none;color:#94a3b8;border-radius:6px;cursor:pointer;font-size:.8rem}
.tab.active{background:#818cf8;color:#fff}
.log{font-family:monospace;font-size:.75rem;background:#0f172a;padding:12px;border-radius:8px;max-height:300px;overflow-y:auto;white-space:pre-wrap;color:#94a3b8;line-height:1.6}
@media(max-width:768px){.card-wide{grid-column:span 1}.grid{grid-template-columns:1fr}}
</style>
</head>
<body>
<h1><span class="dot"></span> Server Monitor</h1>
<div class="grid" id="cards"></div>
<p class="ts" id="ts"></p>

<script>
const MB=1024*1024;
let memHistory=[],cpuHistory=[],threadHistory=[];
const MAX_HIST=30;

function color(pct){return pct>85?'#ef4444':pct>60?'#f59e0b':'#10b981'}
function fmt(ms){const d=Math.floor(ms/86400000),h=Math.floor(ms%86400000/3600000),m=Math.floor(ms%3600000/60000);return d+'d '+h+'h '+m+'m'}

function drawChart(canvas,datasets,labels){
  const ctx=canvas.getContext('2d');
  const W=canvas.width=canvas.offsetWidth*2;
  const H=canvas.height=400;
  const pad={t:20,r:20,b:30,l:50};
  const cw=W-pad.l-pad.r,ch=H-pad.t-pad.b;
  ctx.clearRect(0,0,W,H);
  ctx.font='20px sans-serif';
  ctx.fillStyle='#64748b';

  const allVals=datasets.flatMap(d=>d.data);
  const max=Math.max(...allVals,1)*1.1;

  // Grid
  for(let i=0;i<5;i++){
    const y=pad.t+ch*i/4;
    ctx.strokeStyle='#1e293b';ctx.lineWidth=1;
    ctx.beginPath();ctx.moveTo(pad.l,y);ctx.lineTo(W-pad.r,y);ctx.stroke();
    ctx.fillText(Math.round(max*(1-i/4)),4,y+6);
  }

  // Labels
  if(labels){
    const step=cw/(labels.length-1||1);
    labels.forEach((l,i)=>{ctx.fillText(l,pad.l+i*step-10,H-4)});
  }

  // Lines
  datasets.forEach(ds=>{
    if(ds.data.length<2)return;
    const step=cw/(ds.data.length-1);
    ctx.strokeStyle=ds.color;ctx.lineWidth=3;ctx.lineJoin='round';
    ctx.beginPath();
    ds.data.forEach((v,i)=>{
      const x=pad.l+i*step,y=pad.t+ch*(1-v/max);
      i===0?ctx.moveTo(x,y):ctx.lineTo(x,y);
    });
    ctx.stroke();

    // Fill
    ctx.globalAlpha=.1;ctx.fillStyle=ds.color;
    ctx.lineTo(pad.l+(ds.data.length-1)*step,pad.t+ch);
    ctx.lineTo(pad.l,pad.t+ch);ctx.closePath();ctx.fill();
    ctx.globalAlpha=1;
  });
}

function drawBar(canvas,items){
  const ctx=canvas.getContext('2d');
  const W=canvas.width=canvas.offsetWidth*2;
  const H=canvas.height=400;
  const pad={t:20,r:20,b:40,l:50};
  const cw=W-pad.l-pad.r,ch=H-pad.t-pad.b;
  ctx.clearRect(0,0,W,H);
  ctx.font='20px sans-serif';

  const max=Math.max(...items.flatMap(it=>[it.v1+it.v2+it.v3]),1)*1.1;
  const bw=Math.min(cw/items.length*.6,60);
  const gap=cw/items.length;

  for(let i=0;i<5;i++){
    const y=pad.t+ch*i/4;
    ctx.strokeStyle='#1e293b';ctx.lineWidth=1;
    ctx.beginPath();ctx.moveTo(pad.l,y);ctx.lineTo(W-pad.r,y);ctx.stroke();
    ctx.fillStyle='#64748b';ctx.fillText(Math.round(max*(1-i/4)),4,y+6);
  }

  items.forEach((it,i)=>{
    const x=pad.l+i*gap+gap/2-bw/2;
    let y=pad.t+ch;
    [[it.v1,'#818cf8'],[it.v2,'#10b981'],[it.v3,'#ef4444']].forEach(([v,c])=>{
      const h=v/max*ch;
      ctx.fillStyle=c;
      ctx.fillRect(x,y-h,bw,h);
      y-=h;
    });
    ctx.fillStyle='#64748b';ctx.fillText(it.label,x,H-8);
  });
}

async function refresh(){
  try{
    const r=await fetch('/api/monitor/stats',{headers:{'Authorization':document.cookie?'':'Basic '+btoa(location.href.includes('@')?'':'${monitorUsername}:${monitorPassword}')}});
    if(r.status===401){const c=prompt('Password:');return}
    const d=await r.json();

    const heapUsed=d.jvm.heapUsed/MB;
    const heapMax=d.jvm.heapMax/MB;
    const memPct=Math.round(heapUsed/heapMax*100);
    const nonHeap=d.jvm.nonHeapUsed/MB;
    const loadPct=Math.round(d.jvm.loadAvg/d.jvm.cpus*100);

    memHistory.push(heapUsed);if(memHistory.length>MAX_HIST)memHistory.shift();
    cpuHistory.push(d.jvm.loadAvg);if(cpuHistory.length>MAX_HIST)cpuHistory.shift();
    threadHistory.push(d.jvm.threads);if(threadHistory.length>MAX_HIST)threadHistory.shift();

    const pool=d.db.pool;
    const clients=d.db.totalUsers-d.db.admins;

    document.getElementById('cards').innerHTML=`
    <div class="card">
      <h2>Memory</h2>
      <div class="stat"><span class="stat-label">Heap</span><span class="stat-value" style="color:${'$'}{color(memPct)}">${'$'}{Math.round(heapUsed)}MB / ${'$'}{Math.round(heapMax)}MB (${'$'}{memPct}%)</span></div>
      <div class="bar"><div class="bar-fill" style="width:${'$'}{memPct}%;background:${'$'}{color(memPct)}"></div></div>
      <div class="stat"><span class="stat-label">Non-Heap</span><span class="stat-value">${'$'}{Math.round(nonHeap)}MB</span></div>
      <div class="stat"><span class="stat-label">GC Runs / Time</span><span class="stat-value">${'$'}{d.jvm.gcCount} / ${'$'}{(d.jvm.gcTime/1000).toFixed(1)}s</span></div>
      <canvas id="memChart"></canvas>
    </div>

    <div class="card">
      <h2>CPU & Threads</h2>
      <div class="stat"><span class="stat-label">Load Average</span><span class="stat-value" style="color:${'$'}{color(loadPct)}">${'$'}{d.jvm.loadAvg.toFixed(2)} / ${'$'}{d.jvm.cpus} cores</span></div>
      <div class="bar"><div class="bar-fill" style="width:${'$'}{Math.min(loadPct,100)}%;background:${'$'}{color(loadPct)}"></div></div>
      <div class="stat"><span class="stat-label">Threads</span><span class="stat-value">${'$'}{d.jvm.threads} (peak ${'$'}{d.jvm.peakThreads})</span></div>
      <div class="stat"><span class="stat-label">Daemon</span><span class="stat-value">${'$'}{d.jvm.daemonThreads}</span></div>
      <canvas id="cpuChart"></canvas>
    </div>

    <div class="card">
      <h2>Application</h2>
      <div class="stat"><span class="stat-label">Status</span><span class="badge bg">${'$'}{d.app.status}</span></div>
      <div class="stat"><span class="stat-label">Uptime</span><span class="stat-value">${'$'}{fmt(d.jvm.uptime)}</span></div>
      <div class="stat"><span class="stat-label">Java</span><span class="stat-value">${'$'}{d.app.java}</span></div>
      <div class="stat"><span class="stat-label">Spring Boot</span><span class="stat-value">${'$'}{d.app.springBoot}</span></div>
      <div class="stat"><span class="stat-label">OS</span><span class="stat-value">${'$'}{d.app.os}</span></div>
    </div>

    <div class="card">
      <h2>Database</h2>
      <div class="stat"><span class="stat-label">Users</span><span class="stat-value">${'$'}{d.db.totalUsers} (${'$'}{d.db.admins} admin, ${'$'}{clients} clients)</span></div>
      ${'$'}{pool.active>=0?`
      <div class="stat"><span class="stat-label">Pool Active</span><span class="stat-value" style="color:${'$'}{pool.active>15?'#ef4444':'#10b981'}">${'$'}{pool.active} / ${'$'}{pool.total}</span></div>
      <div class="stat"><span class="stat-label">Pool Idle</span><span class="stat-value">${'$'}{pool.idle}</span></div>
      <div class="stat"><span class="stat-label">Waiting</span><span class="stat-value" style="color:${'$'}{pool.waiting>0?'#ef4444':'#10b981'}">${'$'}{pool.waiting}</span></div>
      `:'<div class="stat"><span class="stat-label">Pool</span><span class="stat-value">N/A</span></div>'}
    </div>

    <div class="card">
      <h2>Reservations</h2>
      <div class="stat"><span class="stat-label">Today</span><span class="stat-value">${'$'}{d.reservations.today}</span></div>
      <div class="stat"><span class="stat-label">Last 7 days</span><span class="stat-value">${'$'}{d.reservations.week}</span></div>
      <div class="stat"><span class="stat-label">Last 30 days</span><span class="stat-value">${'$'}{d.reservations.month}</span></div>
    </div>

    <div class="card card-wide">
      <h2>Monthly Reservations</h2>
      <canvas id="monthChart"></canvas>
      <div style="display:flex;gap:16px;margin-top:8px;justify-content:center">
        <span style="font-size:.75rem;color:#818cf8">&#9632; Total</span>
        <span style="font-size:.75rem;color:#10b981">&#9632; Completed</span>
        <span style="font-size:.75rem;color:#ef4444">&#9632; No-show</span>
      </div>
    </div>

    <div class="card card-wide">
      <h2>Actuator</h2>
      <div class="tabs">
        <a class="tab active" href="/api/monitor/health" target="_blank">/health</a>
        <a class="tab" href="/api/monitor/metrics" target="_blank">/metrics</a>
        <a class="tab" href="/api/monitor/metrics/jvm.memory.used" target="_blank">/jvm.memory</a>
        <a class="tab" href="/api/monitor/metrics/http.server.requests" target="_blank">/http.requests</a>
        <a class="tab" href="/api/monitor/metrics/hikaricp.connections.active" target="_blank">/db.connections</a>
        <a class="tab" href="/api/monitor/loggers" target="_blank">/loggers</a>
      </div>
    </div>
    `;

    // Draw charts
    const mc=document.getElementById('memChart');
    if(mc)drawChart(mc,[{data:memHistory,color:'#818cf8'}]);

    const cc=document.getElementById('cpuChart');
    if(cc)drawChart(cc,[{data:cpuHistory,color:'#f59e0b'},{data:threadHistory.map(t=>t/5),color:'#10b981'}]);

    const bc=document.getElementById('monthChart');
    if(bc&&d.reservations.monthly){
      drawBar(bc,d.reservations.monthly.map(m=>({
        label:m.month.substring(5,7)+'/'+m.month.substring(2,4),
        v1:m.total-m.completed-m.noShow,
        v2:m.completed,
        v3:m.noShow
      })));
    }

    document.getElementById('ts').textContent='Last update: '+new Date().toLocaleString('cs-CZ')+' | Refresh: 5s';
  }catch(e){
    document.getElementById('ts').textContent='Error: '+e.message;
  }
}

refresh();
setInterval(refresh,5000);
</script>
</body>
</html>
        """.trimIndent()

        return ResponseEntity.ok(html)
    }
}
