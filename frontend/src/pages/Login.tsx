import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import { authApi } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
})

type LoginForm = z.infer<typeof loginSchema>

// Number of EQ bars across the full background. 24 matches the v21 mock.
const EQ_BAR_COUNT = 24

// Stable per-bar randomized animation params so the bars don't reseed on every
// re-render (which would break the wave illusion). Memoized to component mount.
function useEqBars(count: number) {
  return useMemo(() => Array.from({ length: count }, () => ({
    durationS: 0.7 + Math.random() * 1.3,
    delayS: -Math.random() * 2,
    heightVh: 30 + Math.random() * 50,
  })), [count])
}

export default function Login() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { setAuth } = useAuthStore()
  const [rememberMe, setRememberMe] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const bars = useEqBars(EQ_BAR_COUNT)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  })

  // Lock the body so the fixed-position EQ + ticker actually fill the screen
  // (no page-level scrollbar competing with the layout). Restored on unmount.
  useEffect(() => {
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = prev }
  }, [])

  const onSubmit = async (data: LoginForm) => {
    try {
      setError(null)
      const response = await authApi.login(data.email, data.password, rememberMe)
      setAuth(response)
      navigate('/')
    } catch {
      setError(t('errors.invalidCredentials'))
    }
  }

  // Single ticker run — duplicated in markup so the keyframe `translateX(-50%)`
  // produces a seamless loop. Items defined once and rendered twice.
  const tickerItems = [
    t('auth.tickerForge'),
    t('auth.tickerBpm'),
    t('auth.tickerLifters'),
    t('auth.tickerNextSlot'),
    t('auth.tickerChalk'),
  ]

  return (
    <div className="login-v21">
      {/* Full-screen EQ bars (24 of them) animating behind everything. */}
      <div className="eq" aria-hidden>
        {bars.map((b, i) => (
          <div
            key={i}
            className="bar"
            style={{
              animationDuration: `${b.durationS}s`,
              animationDelay: `${b.delayS}s`,
              height: `${b.heightVh}vh`,
            }}
          />
        ))}
      </div>

      {/* Top waveform — animated SVG path. */}
      <div className="wave" aria-hidden>
        <svg viewBox="0 0 1200 80" preserveAspectRatio="none">
          <path d="M0,40 Q60,10 120,40 T240,40 T360,40 T480,40 T600,40 T720,40 T840,40 T960,40 T1080,40 T1200,40">
            <animate
              attributeName="d"
              dur="2.5s"
              repeatCount="indefinite"
              values="M0,40 Q60,10 120,40 T240,40 T360,40 T480,40 T600,40 T720,40 T840,40 T960,40 T1080,40 T1200,40;
                      M0,40 Q60,70 120,40 T240,40 T360,40 T480,40 T600,40 T720,40 T840,40 T960,40 T1080,40 T1200,40;
                      M0,40 Q60,10 120,40 T240,40 T360,40 T480,40 T600,40 T720,40 T840,40 T960,40 T1080,40 T1200,40"
            />
          </path>
        </svg>
      </div>

      {/* Top status bar — brand mark + BPM + LIVE indicator. */}
      <header className="top">
        <div className="brand">
          <span className="mk" aria-hidden>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round">
              <path d="M4 9v6M8 6v12M12 3v18M16 6v12M20 9v6" />
            </svg>
          </span>
          Athleisure<span className="d">.</span>Domi
        </div>
        <div className="right">
          <span className="bpm">♪ 132 BPM</span>
          <span>● LIVE</span>
        </div>
      </header>

      {/* Rotating record disc bottom-right. */}
      <div className="disc-wrap" aria-hidden>
        <div className="disc" />
      </div>

      {/* Centered glass card with login form. */}
      <main className="frame">
        <div className="card">
          <div className="kicker">
            <span className="mini-eq" aria-hidden>
              <span /><span /><span /><span />
            </span>
            {t('auth.landingKicker')}
          </div>
          <h1>
            {t('auth.landingHeadline1')}<br />
            <span className="hot">{t('auth.landingHeadline2')}</span>
          </h1>
          <p className="sub">{t('auth.landingSub')}</p>

          {error && <div className="err">{error}</div>}

          <form onSubmit={handleSubmit(onSubmit)}>
            <div className="f">
              <label htmlFor="email">{t('auth.email')}</label>
              <input
                id="email"
                type="email"
                placeholder="email@example.com"
                autoFocus
                aria-invalid={!!errors.email}
                {...register('email')}
              />
            </div>
            <div className="f">
              <label htmlFor="password">{t('auth.password')}</label>
              <input
                id="password"
                type="password"
                placeholder="••••••••"
                aria-invalid={!!errors.password}
                {...register('password')}
              />
            </div>
            <div className="row">
              <label className="remember">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                />
                {t('auth.rememberMe')}
              </label>
              <Link to="/forgot-password">{t('auth.forgotPassword')}</Link>
            </div>
            <button type="submit" className="btn" disabled={isSubmitting}>
              {t('auth.landingCta')}
            </button>
          </form>

          <div className="foot">
            {t('auth.noAccount')} <Link to="/register">{t('auth.landingCreate')}</Link>
          </div>
        </div>
      </main>

      {/* Bottom marquee. */}
      <div className="ticker" aria-hidden>
        <div className="run">
          {[0, 1].map((round) => (
            tickerItems.flatMap((item, i) => [
              <span key={`${round}-${i}-t`}>{item}</span>,
              <span key={`${round}-${i}-d`} className="dot">●</span>,
            ])
          ))}
        </div>
      </div>

      {/* Page-scoped styles — keep the v21 visual language out of the global
          stylesheet so it doesn't bleed into the rest of the app. */}
      <style>{`
        .login-v21 {
          --bg: #0a0a0b;
          --accent: #e05d52;
          --accent-deep: #ab362e;
          --hot: #f5f5f4;
          --ink: #f0f0f2;
          position: fixed;
          inset: 0;
          background: var(--bg);
          color: var(--ink);
          font-family: 'Inter', sans-serif;
          overflow: hidden;
        }

        /* EQ bars across the entire background. */
        .login-v21 .eq {
          position: fixed; inset: 0; z-index: 0;
          display: flex; align-items: center; justify-content: center;
          gap: 14px; padding: 0 40px;
        }
        .login-v21 .eq .bar {
          flex: 1; max-width: 80px;
          background: linear-gradient(180deg, var(--accent-deep) 0%, var(--accent) 100%);
          border-radius: 3px;
          transform-origin: center;
          will-change: transform;
          animation: eqv21 1.4s ease-in-out infinite;
          box-shadow: 0 0 24px rgba(224, 93, 82, 0.3);
          opacity: 0.85;
        }
        @keyframes eqv21 { 0%,100% { transform: scaleY(0.2); } 50% { transform: scaleY(1); } }

        /* Vinyl disc anchored bottom-right, half off-screen. */
        .login-v21 .disc-wrap { position: fixed; right: -120px; bottom: -120px; width: 420px; height: 420px; z-index: 2; }
        .login-v21 .disc {
          width: 100%; height: 100%; border-radius: 50%; position: relative;
          animation: spinDv21 6s linear infinite;
          background:
            radial-gradient(circle, transparent 0 24%, var(--accent) 24% 26%, transparent 27% 100%),
            radial-gradient(circle at center, #222 0 20%, #0a0a0b 22% 100%),
            repeating-radial-gradient(circle, #111 0 2px, #161616 2px 4px);
          box-shadow: 0 0 80px rgba(224, 93, 82, 0.15), inset 0 0 60px rgba(0,0,0,0.8);
        }
        .login-v21 .disc::before {
          content: ""; position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
          width: 8%; height: 8%; background: var(--hot); border-radius: 50%;
          box-shadow: 0 0 20px var(--hot);
        }
        @keyframes spinDv21 { to { transform: rotate(360deg); } }

        /* Waveform strip near the top. */
        .login-v21 .wave { position: fixed; left: 0; right: 0; top: 30px; height: 80px; z-index: 3; pointer-events: none; }
        .login-v21 .wave svg { width: 100%; height: 100%; display: block; }
        .login-v21 .wave path { stroke: var(--accent); stroke-width: 1.5; fill: none; opacity: 0.5; }

        /* Top status bar. */
        .login-v21 .top {
          position: fixed; top: 0; left: 0; right: 0; z-index: 20;
          display: flex; justify-content: space-between; align-items: center; gap: 12px;
          padding: 24px 32px;
          font-family: 'JetBrains Mono', monospace; font-size: 11px; letter-spacing: 0.3em;
          text-transform: uppercase; color: rgba(240,240,242,0.7); white-space: nowrap;
        }
        .login-v21 .top .brand {
          display: flex; align-items: center; gap: 10px;
          font-family: 'Anton', sans-serif; font-size: 22px; letter-spacing: 0.04em; color: var(--ink);
        }
        .login-v21 .top .brand .mk {
          width: 28px; height: 28px; background: var(--accent); color: var(--bg);
          display: grid; place-items: center; border-radius: 4px;
        }
        .login-v21 .top .brand .d { color: var(--hot); }
        .login-v21 .top .right { display: flex; gap: 20px; align-items: center; white-space: nowrap; }
        .login-v21 .top .bpm { color: var(--accent); }

        /* Glass card centered. */
        .login-v21 .frame { position: relative; z-index: 10; height: 100vh; display: grid; place-items: center; padding: 24px; }
        .login-v21 .card {
          width: min(440px, 92vw);
          background: rgba(10,10,11,0.8);
          backdrop-filter: blur(20px) saturate(1.4);
          -webkit-backdrop-filter: blur(20px) saturate(1.4);
          border: 1px solid rgba(240,240,242,0.1);
          padding: 36px 34px 28px;
          box-shadow: 0 40px 80px -20px rgba(0,0,0,0.7);
          position: relative;
        }
        .login-v21 .card::before {
          content: ""; position: absolute; top: -1px; left: 30px; right: 30px;
          height: 2px; background: var(--accent); box-shadow: 0 0 12px var(--accent);
        }

        .login-v21 .mini-eq { display: inline-flex; gap: 2px; align-items: flex-end; height: 14px; vertical-align: middle; margin-right: 10px; }
        .login-v21 .mini-eq span { width: 3px; background: var(--accent); animation: meqv21 0.8s ease-in-out infinite; }
        .login-v21 .mini-eq span:nth-child(1) { animation-delay: -0.1s; height: 50%; }
        .login-v21 .mini-eq span:nth-child(2) { animation-delay: -0.3s; height: 80%; }
        .login-v21 .mini-eq span:nth-child(3) { animation-delay: -0.5s; height: 40%; }
        .login-v21 .mini-eq span:nth-child(4) { animation-delay: -0.2s; height: 70%; }
        @keyframes meqv21 { 0%,100% { transform: scaleY(0.3); } 50% { transform: scaleY(1); } }

        .login-v21 .kicker {
          display: inline-flex; align-items: center;
          font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.32em;
          text-transform: uppercase; color: var(--accent); margin-bottom: 20px; white-space: nowrap;
        }
        .login-v21 h1 {
          font-family: 'Anton', sans-serif; font-size: 66px; line-height: 0.92; letter-spacing: -0.01em;
          margin: 0 0 16px; text-transform: uppercase;
        }
        .login-v21 h1 .hot { color: var(--accent); }
        .login-v21 .sub { font-size: 14px; color: rgba(240,240,242,0.65); margin: 0 0 26px; }

        .login-v21 .err {
          background: rgba(224, 93, 82, 0.12); border: 1px solid rgba(224, 93, 82, 0.4);
          color: var(--accent); padding: 10px 12px; border-radius: 4px;
          font-size: 13px; margin-bottom: 16px;
        }

        .login-v21 .f { margin-bottom: 14px; }
        .login-v21 .f label {
          display: block; font-family: 'JetBrains Mono', monospace; font-size: 10px;
          letter-spacing: 0.3em; text-transform: uppercase; color: rgba(240,240,242,0.55);
          margin-bottom: 6px;
        }
        .login-v21 .f input {
          width: 100%; background: rgba(240,240,242,0.04);
          border: 1px solid rgba(240,240,242,0.1); border-radius: 4px;
          color: var(--ink); padding: 12px 14px;
          font: inherit; font-size: 15px; outline: none; transition: all .2s;
        }
        .login-v21 .f input:focus {
          border-color: var(--accent);
          background: rgba(224, 93, 82, 0.06);
        }
        .login-v21 .f input[aria-invalid="true"] {
          border-color: rgba(224, 93, 82, 0.7);
        }

        .login-v21 .row {
          display: flex; justify-content: space-between; align-items: center;
          font-size: 12px; margin: 6px 0 18px; color: rgba(240,240,242,0.6);
        }
        .login-v21 .row a { color: var(--accent); text-decoration: none; }
        .login-v21 .remember { display: inline-flex; align-items: center; cursor: pointer; }
        .login-v21 .row input[type=checkbox] {
          appearance: none; width: 14px; height: 14px;
          border: 1px solid rgba(240,240,242,0.25); border-radius: 3px;
          vertical-align: middle; margin-right: 6px; cursor: pointer;
        }
        .login-v21 .row input[type=checkbox]:checked { background: var(--accent); border-color: var(--accent); }

        .login-v21 .btn {
          width: 100%; padding: 15px;
          background: var(--accent); color: var(--bg); border: none; border-radius: 4px;
          font-family: 'Anton', sans-serif; font-size: 18px; letter-spacing: 0.12em;
          text-transform: uppercase; cursor: pointer; transition: all .2s;
          box-shadow: 0 0 30px rgba(224, 93, 82, 0.3);
        }
        .login-v21 .btn:hover:not(:disabled) {
          background: var(--accent-deep); color: var(--ink);
          box-shadow: 0 0 40px rgba(171, 54, 46, 0.5);
        }
        .login-v21 .btn:disabled { opacity: 0.6; cursor: not-allowed; }

        .login-v21 .foot { margin-top: 16px; text-align: center; font-size: 12px; color: rgba(240,240,242,0.55); }
        .login-v21 .foot a { color: var(--accent); text-decoration: none; }

        /* Bottom marquee. */
        .login-v21 .ticker {
          position: fixed; bottom: 0; left: 0; right: 0; height: 38px; z-index: 10;
          background: var(--accent); color: var(--bg);
          display: flex; align-items: center; overflow: hidden;
          font-family: 'Anton', sans-serif; font-size: 18px;
          letter-spacing: 0.06em; text-transform: uppercase;
        }
        .login-v21 .ticker .run {
          display: flex; gap: 24px; white-space: nowrap;
          animation: ticv21 22s linear infinite; padding-left: 24px;
        }
        @keyframes ticv21 { to { transform: translateX(-50%); } }
        .login-v21 .ticker .dot { color: var(--hot); }

        /* Tighter spacing on small viewports. */
        @media (max-width: 520px) {
          .login-v21 h1 { font-size: 52px; }
          .login-v21 .top { padding: 18px 18px; font-size: 10px; }
          .login-v21 .top .brand { font-size: 18px; }
          .login-v21 .top .right { gap: 12px; }
          .login-v21 .disc-wrap { width: 280px; height: 280px; right: -90px; bottom: -90px; }
        }
      `}</style>
    </div>
  )
}
