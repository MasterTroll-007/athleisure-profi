package com.fitness.controller

import com.fitness.repository.ReservationRepository
import com.fitness.repository.UserRepository
import com.fitness.repository.CreditTransactionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.annotation.PostConstruct
import java.io.File
import java.lang.management.ManagementFactory
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

    @GetMapping("/dashboard", produces = [MediaType.TEXT_HTML_VALUE])
    fun dashboard(): ResponseEntity<String> {

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
h1 .status{font-size:.75rem;color:#94a3b8;font-weight:400;margin-left:auto}
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
.act-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px;margin-top:12px}
.act-card{background:#0f172a;border:1px solid #334155;border-radius:8px;padding:12px;cursor:pointer;transition:border-color .2s}
.act-card:hover{border-color:#818cf8}
.act-card h3{font-size:.8rem;color:#818cf8;margin-bottom:8px}
.act-card .act-value{font-size:1.5rem;font-weight:700;color:#e2e8f0}
.act-card .act-sub{font-size:.7rem;color:#64748b;margin-top:4px}
.act-detail{background:#0f172a;border-radius:8px;padding:12px;margin-top:8px;font-family:monospace;font-size:.75rem;color:#94a3b8;max-height:300px;overflow-y:auto;white-space:pre-wrap;display:none}
.act-detail.open{display:block}
@media(max-width:768px){.card-wide{grid-column:span 1}.grid{grid-template-columns:1fr}.act-grid{grid-template-columns:1fr}}
.login-overlay{position:fixed;inset:0;background:#0f172a;z-index:100;display:flex;align-items:center;justify-content:center}
.login-box{background:#1e293b;border:1px solid #334155;border-radius:12px;padding:32px;width:320px}
.login-box h2{color:#818cf8;margin-bottom:20px;font-size:1.2rem}
.login-box input{width:100%;padding:10px 12px;margin-bottom:12px;background:#0f172a;border:1px solid #334155;border-radius:8px;color:#e2e8f0;font-size:.9rem;outline:none}
.login-box input:focus{border-color:#818cf8}
.login-box button{width:100%;padding:10px;background:#818cf8;color:#fff;border:none;border-radius:8px;font-size:.9rem;font-weight:600;cursor:pointer}
.login-box button:hover{background:#6366f1}
.login-box .error{color:#ef4444;font-size:.8rem;margin-bottom:12px}
</style>
</head>
<body>
<div class="login-overlay" id="loginOverlay" style="display:none">
  <div class="login-box">
    <h2>Server Monitor</h2>
    <div class="error" id="loginError" style="display:none">Nesprávné přihlašovací údaje</div>
    <input type="text" id="loginUser" placeholder="Username" autocomplete="username">
    <input type="password" id="loginPass" placeholder="Password" autocomplete="current-password">
    <button onclick="doLogin()">Přihlásit</button>
  </div>
</div>
<h1><span class="dot"></span> Server Monitor <span class="status" id="ts"></span></h1>
<div style="display:flex;gap:4px;margin-bottom:16px;flex-wrap:wrap">
  <span style="color:#94a3b8;font-size:.8rem;margin-right:8px;align-self:center">History:</span>
  <button class="tab range-btn" data-hours="1" onclick="setRange(1)">1h</button>
  <button class="tab range-btn" data-hours="6" onclick="setRange(6)">6h</button>
  <button class="tab range-btn active" data-hours="24" onclick="setRange(24)">24h</button>
  <button class="tab range-btn" data-hours="72" onclick="setRange(72)">3d</button>
  <button class="tab range-btn" data-hours="168" onclick="setRange(168)">7d</button>
</div>
<div class="grid" id="cards"></div>
<div id="actuator-section"></div>

<script>
const MB=1024*1024;
let memHistory=[],cpuHistory=[],threadHistory=[],ramHistory=[],diskHistory=[];
const MAX_HIST=500;
let historyHours=24;
let historyLoaded=false;
let AUTH=sessionStorage.getItem('monitorAuth')||'';

function showLogin(msg){
  document.getElementById('loginOverlay').style.display='flex';
  if(msg){document.getElementById('loginError').style.display='block';document.getElementById('loginError').textContent=msg}
  document.getElementById('loginUser').focus();
}
function doLogin(){
  const u=document.getElementById('loginUser').value;
  const p=document.getElementById('loginPass').value;
  if(!u||!p)return;
  AUTH='Basic '+btoa(u+':'+p);
  sessionStorage.setItem('monitorAuth',AUTH);
  document.getElementById('loginOverlay').style.display='none';
  document.getElementById('loginError').style.display='none';
  loadHistory(24).then(()=>{refresh();refreshActuator()});
}
document.getElementById('loginPass').addEventListener('keydown',e=>{if(e.key==='Enter')doLogin()});
document.getElementById('loginUser').addEventListener('keydown',e=>{if(e.key==='Enter')document.getElementById('loginPass').focus()});

function color(pct){return pct>85?'#ef4444':pct>60?'#f59e0b':'#10b981'}
function fmt(ms){const d=Math.floor(ms/86400000),h=Math.floor(ms%86400000/3600000),m=Math.floor(ms%3600000/60000);return d+'d '+h+'h '+m+'m'}
function fmtBytes(b){if(b>1e9)return(b/1e9).toFixed(1)+'GB';if(b>1e6)return(b/1e6).toFixed(1)+'MB';if(b>1e3)return(b/1e3).toFixed(1)+'KB';return b+'B'}

function drawChart(canvas,datasets,labels){
  const ctx=canvas.getContext('2d');
  const W=canvas.width=canvas.offsetWidth*2;
  const H=canvas.height=400;
  const pad={t:20,r:20,b:30,l:50};
  const cw=W-pad.l-pad.r,ch=H-pad.t-pad.b;
  ctx.clearRect(0,0,W,H);
  ctx.font='20px sans-serif';ctx.fillStyle='#64748b';
  const allVals=datasets.flatMap(d=>d.data);
  const max=Math.max(...allVals,1)*1.1;
  for(let i=0;i<5;i++){
    const y=pad.t+ch*i/4;
    ctx.strokeStyle='#1e293b';ctx.lineWidth=1;
    ctx.beginPath();ctx.moveTo(pad.l,y);ctx.lineTo(W-pad.r,y);ctx.stroke();
    ctx.fillText(Math.round(max*(1-i/4)),4,y+6);
  }
  if(labels){const step=cw/(labels.length-1||1);labels.forEach((l,i)=>{ctx.fillText(l,pad.l+i*step-10,H-4)})}
  datasets.forEach(ds=>{
    if(ds.data.length<2)return;
    const step=cw/(ds.data.length-1);
    ctx.strokeStyle=ds.color;ctx.lineWidth=3;ctx.lineJoin='round';
    ctx.beginPath();
    ds.data.forEach((v,i)=>{const x=pad.l+i*step,y=pad.t+ch*(1-v/max);i===0?ctx.moveTo(x,y):ctx.lineTo(x,y)});
    ctx.stroke();
    ctx.globalAlpha=.1;ctx.fillStyle=ds.color;
    ctx.lineTo(pad.l+(ds.data.length-1)*step,pad.t+ch);ctx.lineTo(pad.l,pad.t+ch);ctx.closePath();ctx.fill();
    ctx.globalAlpha=1;
  });
}

function drawBar(canvas,items){
  const ctx=canvas.getContext('2d');
  const W=canvas.width=canvas.offsetWidth*2;
  const H=canvas.height=400;
  const pad={t:20,r:20,b:40,l:50};
  const cw=W-pad.l-pad.r,ch=H-pad.t-pad.b;
  ctx.clearRect(0,0,W,H);ctx.font='20px sans-serif';
  const max=Math.max(...items.flatMap(it=>[it.v1+it.v2+it.v3]),1)*1.1;
  const bw=Math.min(cw/items.length*.6,60);const gap=cw/items.length;
  for(let i=0;i<5;i++){const y=pad.t+ch*i/4;ctx.strokeStyle='#1e293b';ctx.lineWidth=1;ctx.beginPath();ctx.moveTo(pad.l,y);ctx.lineTo(W-pad.r,y);ctx.stroke();ctx.fillStyle='#64748b';ctx.fillText(Math.round(max*(1-i/4)),4,y+6)}
  items.forEach((it,i)=>{const x=pad.l+i*gap+gap/2-bw/2;let y=pad.t+ch;[[it.v1,'#818cf8'],[it.v2,'#10b981'],[it.v3,'#ef4444']].forEach(([v,c])=>{const h=v/max*ch;ctx.fillStyle=c;ctx.fillRect(x,y-h,bw,h);y-=h});ctx.fillStyle='#64748b';ctx.fillText(it.label,x,H-8)});
}

function drawGauge(canvas,pct,label,clr){
  const ctx=canvas.getContext('2d');
  const W=canvas.width=200;const H=canvas.height=120;
  const cx=W/2,cy=H-10,r=70;
  ctx.clearRect(0,0,W,H);
  ctx.beginPath();ctx.arc(cx,cy,r,-Math.PI,0);ctx.strokeStyle='#334155';ctx.lineWidth=12;ctx.lineCap='round';ctx.stroke();
  ctx.beginPath();ctx.arc(cx,cy,r,-Math.PI,-Math.PI+Math.PI*(pct/100));ctx.strokeStyle=clr;ctx.lineWidth=12;ctx.lineCap='round';ctx.stroke();
  ctx.fillStyle='#e2e8f0';ctx.font='bold 24px sans-serif';ctx.textAlign='center';ctx.fillText(pct+'%',cx,cy-20);
  ctx.fillStyle='#64748b';ctx.font='12px sans-serif';ctx.fillText(label,cx,cy-2);
}

async function fetchActuator(path){
  try{const r=await fetch('/api/monitor/'+path,{headers:{Authorization:AUTH}});if(!r.ok)return null;return await r.json()}catch(e){return null}
}

async function refreshActuator(){
  const section=document.getElementById('actuator-section');
  const [health,metrics,httpReqs,jvmMem,hikari]=await Promise.all([
    fetchActuator('health'),
    fetchActuator('metrics'),
    fetchActuator('metrics/http.server.requests'),
    fetchActuator('metrics/jvm.memory.used'),
    fetchActuator('metrics/hikaricp.connections.active')
  ]);

  let html='<div class="card card-wide" style="margin-top:0"><h2>Spring Boot Actuator</h2>';

  // Health status
  if(health){
    const st=health.status||'UNKNOWN';
    const badge=st==='UP'?'bg':st==='DOWN'?'br':'by';
    html+='<div class="stat"><span class="stat-label">Health Status</span><span class="badge '+badge+'">'+st+'</span></div>';
    if(health.components){
      Object.entries(health.components).forEach(([k,v])=>{
        const cst=v.status||'?';
        const cb=cst==='UP'?'bg':'br';
        html+='<div class="stat"><span class="stat-label" style="padding-left:16px">'+k+'</span><span class="badge '+cb+'">'+cst+'</span></div>';
      });
    }
  }

  html+='<div class="act-grid">';

  // HTTP Requests
  if(httpReqs&&httpReqs.measurements){
    const count=httpReqs.measurements.find(m=>m.statistic==='COUNT');
    const total=httpReqs.measurements.find(m=>m.statistic==='TOTAL_TIME');
    const max=httpReqs.measurements.find(m=>m.statistic==='MAX');
    html+='<div class="act-card" onclick="toggleDetail(\'http-detail\')"><h3>HTTP Requests</h3>';
    html+='<div class="act-value">'+(count?count.value:'-')+'</div>';
    html+='<div class="act-sub">Total time: '+(total?(total.value).toFixed(2)+'s':'-')+'</div>';
    html+='<div class="act-sub">Max: '+(max?(max.value*1000).toFixed(0)+'ms':'-')+'</div>';
    html+='</div>';
  }

  // JVM Memory
  if(jvmMem&&jvmMem.measurements){
    const val=jvmMem.measurements.find(m=>m.statistic==='VALUE');
    html+='<div class="act-card" onclick="toggleDetail(\'mem-detail\')"><h3>JVM Memory Used</h3>';
    html+='<div class="act-value">'+(val?fmtBytes(val.value):'-')+'</div>';
    html+='<div class="act-sub">All memory pools combined</div>';
    html+='</div>';
  }

  // HikariCP
  if(hikari&&hikari.measurements){
    const val=hikari.measurements.find(m=>m.statistic==='VALUE');
    html+='<div class="act-card"><h3>DB Pool Active</h3>';
    html+='<div class="act-value">'+(val?val.value:'-')+'</div>';
    html+='<div class="act-sub">Active connections</div>';
    html+='</div>';
  }

  // Available metrics list
  if(metrics&&metrics.names){
    html+='<div class="act-card" onclick="toggleDetail(\'metrics-list\')"><h3>Available Metrics</h3>';
    html+='<div class="act-value">'+metrics.names.length+'</div>';
    html+='<div class="act-sub">Click to view all</div>';
    html+='</div>';
  }

  html+='</div>';

  // Detail sections
  if(metrics&&metrics.names){
    html+='<div class="act-detail" id="metrics-list">'+metrics.names.sort().join('\n')+'</div>';
  }

  // Actuator quick links
  html+='<div style="display:flex;gap:8px;margin-top:16px;flex-wrap:wrap">';
  [['health','Health'],['metrics','Metrics'],['loggers','Loggers'],['info','Info'],['env','Environment']].forEach(([p,l])=>{
    html+='<a href="/api/monitor/'+p+'" target="_blank" style="padding:4px 12px;background:#334155;color:#94a3b8;border-radius:6px;font-size:.8rem;text-decoration:none;transition:background .2s" onmouseover="this.style.background=\'#475569\'" onmouseout="this.style.background=\'#334155\'">/'+l.toLowerCase()+'</a>';
  });
  html+='</div></div>';

  section.innerHTML=html;
}

function toggleDetail(id){
  const el=document.getElementById(id);
  if(el)el.classList.toggle('open');
}

async function refresh(){
  try{
    const r=await fetch('/api/monitor/stats',{headers:{Authorization:AUTH}});
    if(r.status===401){sessionStorage.removeItem('monitorAuth');AUTH='';showLogin('Nesprávné přihlašovací údaje');return}
    if(!r.ok){document.getElementById('ts').textContent='Error: '+r.status;return}
    const d=await r.json();

    const heapUsed=d.jvm.heapUsed/MB;
    const heapMax=d.jvm.heapMax/MB;
    const memPct=Math.round(heapUsed/heapMax*100);
    const nonHeap=d.jvm.nonHeapUsed/MB;
    const loadPct=Math.round(d.jvm.loadAvg/d.jvm.cpus*100);
    const ram=d.ram||{};
    const ramTotal=ram.total>0?ram.total:1;
    const ramUsed=ram.used||0;
    const ramFree=ram.free||0;
    const ramPct=ramTotal>0?Math.round(ramUsed/ramTotal*100):0;
    const pool=d.db.pool;
    const clients=d.db.totalUsers-d.db.admins;

    memHistory.push(heapUsed);if(memHistory.length>MAX_HIST)memHistory.shift();
    ramHistory.push(ramUsed/MB);if(ramHistory.length>MAX_HIST)ramHistory.shift();
    cpuHistory.push(d.jvm.loadAvg);if(cpuHistory.length>MAX_HIST)cpuHistory.shift();
    threadHistory.push(d.jvm.threads);if(threadHistory.length>MAX_HIST)threadHistory.shift();

    document.getElementById('cards').innerHTML=`
    <div class="card">
      <h2>Memory</h2>
      <div style="display:flex;justify-content:center"><canvas id="memGauge" width="200" height="120"></canvas></div>
      <div class="stat"><span class="stat-label">Heap</span><span class="stat-value" style="color:${'$'}{color(memPct)}">${'$'}{Math.round(heapUsed)}MB / ${'$'}{Math.round(heapMax)}MB</span></div>
      <div class="bar"><div class="bar-fill" style="width:${'$'}{memPct}%;background:${'$'}{color(memPct)}"></div></div>
      <div class="stat"><span class="stat-label">Non-Heap</span><span class="stat-value">${'$'}{Math.round(nonHeap)}MB</span></div>
      <div class="stat"><span class="stat-label">GC Runs / Time</span><span class="stat-value">${'$'}{d.jvm.gcCount} / ${'$'}{(d.jvm.gcTime/1000).toFixed(1)}s</span></div>
      <canvas id="memChart"></canvas>
    </div>

    <div class="card">
      <h2>System RAM</h2>
      <div style="display:flex;justify-content:center"><canvas id="ramGauge" width="200" height="120"></canvas></div>
      <div class="stat"><span class="stat-label">Used</span><span class="stat-value" style="color:${'$'}{color(ramPct)}">${'$'}{fmtBytes(ramUsed)} / ${'$'}{fmtBytes(ramTotal)}</span></div>
      <div class="bar"><div class="bar-fill" style="width:${'$'}{ramPct}%;background:${'$'}{color(ramPct)}"></div></div>
      <div class="stat"><span class="stat-label">Free</span><span class="stat-value">${'$'}{fmtBytes(ramFree)}</span></div>
      <canvas id="ramChart"></canvas>
    </div>

    <div class="card">
      <h2>CPU & Threads</h2>
      <div style="display:flex;justify-content:center"><canvas id="cpuGauge" width="200" height="120"></canvas></div>
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
      <h2>Disk</h2>
      ${'$'}{d.disk.map(dk=>{
        const total=dk.total;const used=dk.used;const free=dk.free;
        const pct=total>0?Math.round(used/total*100):0;
        return `
        <div class="stat"><span class="stat-label">${'$'}{dk.path}</span><span class="stat-value" style="color:${'$'}{color(pct)}">${'$'}{pct}%</span></div>
        <div class="bar"><div class="bar-fill" style="width:${'$'}{pct}%;background:${'$'}{color(pct)}"></div></div>
        <div class="stat"><span class="stat-label">Used</span><span class="stat-value">${'$'}{fmtBytes(used)} / ${'$'}{fmtBytes(total)}</span></div>
        <div class="stat"><span class="stat-label">Free</span><span class="stat-value">${'$'}{fmtBytes(free)}</span></div>
        <div class="stat"><span class="stat-label">Usable</span><span class="stat-value">${'$'}{fmtBytes(dk.usable)}</span></div>
        `;
      }).join('<div style="border-top:1px solid #334155;margin:8px 0"></div>')}
    </div>

    <div class="card">
      <h2>Database</h2>
      <div class="stat"><span class="stat-label">Users</span><span class="stat-value">${'$'}{d.db.totalUsers} (${'$'}{d.db.admins} admin, ${'$'}{clients} clients)</span></div>
      ${'$'}{pool.active>=0?`
      <div class="stat"><span class="stat-label">Pool Active</span><span class="stat-value" style="color:${'$'}{pool.active>15?'#ef4444':'#10b981'}">${'$'}{pool.active} / ${'$'}{pool.total}</span></div>
      <div class="bar"><div class="bar-fill" style="width:${'$'}{pool.total>0?pool.active/pool.total*100:0}%;background:${'$'}{pool.active>15?'#ef4444':'#10b981'}"></div></div>
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
    `;

    // Draw gauges
    const mg=document.getElementById('memGauge');
    if(mg)drawGauge(mg,memPct,'Heap Usage',color(memPct));
    const rg=document.getElementById('ramGauge');
    if(rg)drawGauge(rg,ramPct,'RAM Usage',color(ramPct));
    const cg=document.getElementById('cpuGauge');
    if(cg)drawGauge(cg,Math.min(loadPct,100),'CPU Load',color(loadPct));

    // Draw charts
    const rc=document.getElementById('ramChart');
    if(rc)drawChart(rc,[{data:ramHistory,color:'#f472b6'}]);
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

    document.getElementById('ts').textContent='Updated: '+new Date().toLocaleTimeString()+' | Auto-refresh: 5s';
  }catch(e){
    document.getElementById('ts').textContent='Error: '+e.message;
  }
}

async function loadHistory(hours){
  historyHours=hours;
  try{
    const r=await fetch('/api/monitor/history?hours='+hours,{headers:{Authorization:AUTH}});
    if(!r.ok)return;
    const data=await r.json();
    memHistory=data.map(s=>s.heap);
    ramHistory=data.map(s=>s.ram);
    cpuHistory=data.map(s=>s.cpu);
    threadHistory=data.map(s=>s.threads);
    diskHistory=data.map(s=>s.disk);
    historyLoaded=true;
  }catch(e){console.error('History load failed',e)}
}

function setRange(hours){
  document.querySelectorAll('.range-btn').forEach(b=>b.classList.remove('active'));
  document.querySelector('[data-hours="'+hours+'"]')?.classList.add('active');
  loadHistory(hours).then(()=>refresh());
}

if(!AUTH){showLogin()}else{loadHistory(24).then(()=>{refresh();refreshActuator()})}
setInterval(()=>{if(AUTH)refresh()},5000);
setInterval(()=>{if(AUTH)refreshActuator()},15000);
</script>
</body>
</html>
        """.trimIndent()

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
