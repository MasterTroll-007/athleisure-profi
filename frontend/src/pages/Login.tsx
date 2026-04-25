import { useEffect, useState } from 'react'
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

const SUPPORTED_LANGS = ['en', 'cs'] as const
type SupportedLang = (typeof SUPPORTED_LANGS)[number]

export default function Login() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const { setAuth } = useAuthStore()
  const [rememberMe, setRememberMe] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // Bumped on every failed submit so the .err element re-mounts and the
  // shake animation replays — even when the message text didn't change.
  const [errorAttempt, setErrorAttempt] = useState(0)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  })

  useEffect(() => {
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = prev }
  }, [])

  const onSubmit = async (data: LoginForm) => {
    try {
      const response = await authApi.login(data.email, data.password, rememberMe)
      setAuth(response)
      navigate('/')
    } catch {
      setError(t('errors.invalidCredentials'))
      setErrorAttempt((a) => a + 1)
    }
  }

  // Client-side validation failures (e.g. malformed email) get the same
  // strip + shake treatment as auth failures, with a format-specific message.
  const onInvalid = (formErrors: typeof errors) => {
    if (formErrors.email) setError(t('errors.invalidEmail'))
    else setError(t('errors.invalidCredentials'))
    setErrorAttempt((a) => a + 1)
  }

  const activeLang: SupportedLang = (SUPPORTED_LANGS as readonly string[]).includes(i18n.language)
    ? (i18n.language as SupportedLang)
    : 'cs'
  const setLang = (l: SupportedLang) => {
    i18n.changeLanguage(l)
    localStorage.setItem('locale', l)
  }

  return (
    <div className="login-v5">
      {/* Coach portrait — vertically centered, screen-blended into the dark stage,
          with a top/bottom 20% mask fade so it dissolves into the background. */}
      <div className="coach" aria-hidden>
        <img src="/coach-back.jpg" alt="" />
      </div>

      <header className="topbar">
        <div className="lang" role="group" aria-label="Language">
          {SUPPORTED_LANGS.map((l) => (
            <button
              key={l}
              type="button"
              className={l === activeLang ? 'on' : ''}
              onClick={() => setLang(l)}
            >
              {l === 'cs' ? 'CZ' : 'EN'}
            </button>
          ))}
        </div>
      </header>

      <main className="hero">
        <form className="loginForm" onSubmit={handleSubmit(onSubmit, onInvalid)} noValidate>
          {error && <div className="err" key={errorAttempt}>{error}</div>}

          <div className="f">
            <label htmlFor="email">{t('auth.email')}</label>
            <input
              id="email"
              type="email"
              placeholder={t('auth.emailPlaceholder')}
              autoComplete="email"
              autoFocus
              aria-invalid={!!errors.email || !!error}
              {...register('email')}
            />
          </div>

          <div className="f">
            <label htmlFor="password">
              <span>{t('auth.password')}</span>
              <Link to="/forgot-password">{t('auth.forgotShort')}</Link>
            </label>
            <input
              id="password"
              type="password"
              placeholder="••••••••"
              autoComplete="current-password"
              aria-invalid={!!errors.password || !!error}
              {...register('password')}
            />
          </div>

          <div className="rowR">
            <label>
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
              />
              <span> {t('auth.stayLoggedIn')}</span>
            </label>
          </div>

          <button type="submit" className="submit" disabled={isSubmitting}>
            {t('auth.signInCta')}
          </button>
        </form>
      </main>

      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;500;700;900&family=JetBrains+Mono:wght@400;500&display=swap');

        .login-v5 {
          --a: #ffb347;
          --ink: #05040a;
          position: fixed; inset: 0;
          background: var(--ink); color: #f5f5f2;
          font-family: 'Outfit', sans-serif;
          overflow: hidden;
          display: grid; grid-template-rows: auto 1fr;
        }

        .login-v5 .coach {
          position: absolute; left: 50%; top: 50%;
          transform: translate(-50%, -50%);
          z-index: 1; pointer-events: none;
          width: min(60vh, 640px); aspect-ratio: 2/3; opacity: 0.95;
        }
        .login-v5 .coach img {
          width: 100%; height: 100%; display: block;
          object-fit: cover; object-position: center center;
          mix-blend-mode: screen;
          filter: grayscale(0.1) contrast(1.15) brightness(1.05);
          /* Two masks intersected: 20% vertical fade top/bottom + 5% horizontal
             fade left/right, so the portrait dissolves into the dark stage on
             every edge instead of cutting off sharply. */
          -webkit-mask-image:
            linear-gradient(180deg, transparent 0%, #000 20%, #000 80%, transparent 100%),
            linear-gradient(90deg,  transparent 0%, #000 5%,  #000 95%, transparent 100%);
                  mask-image:
            linear-gradient(180deg, transparent 0%, #000 20%, #000 80%, transparent 100%),
            linear-gradient(90deg,  transparent 0%, #000 5%,  #000 95%, transparent 100%);
          -webkit-mask-composite: source-in;
                  mask-composite: intersect;
        }

        .login-v5 .topbar {
          position: relative; z-index: 10;
          display: flex; align-items: center; justify-content: flex-end;
          padding: 22px 32px;
        }

        .login-v5 .lang {
          display: inline-flex; align-items: center;
          background: rgba(5,4,10,0.28);
          backdrop-filter: blur(6px) saturate(1.05);
          -webkit-backdrop-filter: blur(6px) saturate(1.05);
          border: 1px solid rgba(255,255,255,0.08);
          border-radius: 999px; padding: 3px;
          font-family: 'JetBrains Mono', monospace;
        }
        .login-v5 .lang button {
          background: transparent; border: none; cursor: pointer;
          color: rgba(255,255,255,0.55);
          font-family: inherit; font-size: 11px; letter-spacing: 0.18em; font-weight: 500;
          padding: 7px 14px; border-radius: 999px;
          transition: color .2s, background .2s;
        }
        .login-v5 .lang button.on { background: rgba(255,255,255,0.10); color: #fff; }
        .login-v5 .lang button:hover:not(.on) { color: rgba(255,255,255,0.85); }

        .login-v5 .hero {
          position: relative; z-index: 5;
          display: grid; place-items: center; text-align: center;
          padding: 0 24px;
        }

        .login-v5 .loginForm {
          width: min(380px, 92vw); text-align: left;
          font-family: 'JetBrains Mono', monospace;
          background: rgba(5,4,10,0.28);
          backdrop-filter: blur(6px) saturate(1.05);
          -webkit-backdrop-filter: blur(6px) saturate(1.05);
          border: 1px solid rgba(255,255,255,0.06);
          border-radius: 16px;
          padding: 28px 24px;
          box-shadow: 0 30px 80px -30px rgba(0,0,0,0.6);
        }

        .login-v5 .err {
          display: flex; align-items: center; gap: 10px;
          font-family: 'JetBrains Mono', monospace;
          font-size: 10px; letter-spacing: 0.2em; text-transform: uppercase;
          color: #ff9a9a;
          background: rgba(255, 90, 90, 0.07);
          border: 1px solid rgba(255, 90, 90, 0.22);
          border-radius: 10px;
          padding: 11px 14px;
          margin-bottom: 14px;
          animation: errShake .35s cubic-bezier(.36,.07,.19,.97);
        }
        .login-v5 .err::before {
          content: ""; flex: 0 0 auto;
          width: 6px; height: 6px; border-radius: 50%;
          background: #ff7474;
          box-shadow: 0 0 8px rgba(255, 100, 100, 0.6);
        }
        @keyframes errShake {
          10%, 90% { transform: translateX(-1px); }
          20%, 80% { transform: translateX(2px); }
          30%, 50%, 70% { transform: translateX(-3px); }
          40%, 60% { transform: translateX(3px); }
        }

        .login-v5 .f { margin-bottom: 14px; position: relative; }
        .login-v5 .f label {
          display: flex; justify-content: space-between; align-items: baseline;
          font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase;
          color: rgba(255,255,255,0.7); margin-bottom: 6px;
        }
        .login-v5 .f label a {
          color: var(--a); text-decoration: none;
          text-transform: none; letter-spacing: 0.04em;
          font-size: 11px; font-weight: 500;
        }
        .login-v5 .f label a:hover { text-decoration: underline; }
        .login-v5 .f input {
          width: 100%;
          background: rgba(255,255,255,0.20);
          border: 1px solid rgba(255,255,255,0.10);
          border-radius: 10px;
          padding: 13px 14px;
          font: inherit; font-size: 14px;
          color: #f5f5f2; outline: none;
          font-family: 'Outfit', sans-serif;
          letter-spacing: 0; text-transform: none;
          transition: border-color .2s, background .2s;
        }
        .login-v5 .f input::placeholder { color: rgba(255,255,255,0.55); }
        .login-v5 .f input:focus {
          border-color: var(--a);
          background: rgba(255,255,255,0.22);
        }
        .login-v5 .f input[aria-invalid="true"] {
          border-color: rgba(255, 120, 120, 0.6);
        }
        /* Chrome autofill paints its own pale-blue background — keep the
           translucent glass look by masking it with an inset shadow and
           freezing the colour transition. */
        .login-v5 .f input:-webkit-autofill,
        .login-v5 .f input:-webkit-autofill:hover,
        .login-v5 .f input:-webkit-autofill:focus,
        .login-v5 .f input:-webkit-autofill:active {
          -webkit-box-shadow: 0 0 0 1000px rgba(20, 18, 28, 0.55) inset;
                  box-shadow: 0 0 0 1000px rgba(20, 18, 28, 0.55) inset;
          -webkit-text-fill-color: #f5f5f2;
          caret-color: #f5f5f2;
          transition: background-color 9999s ease-in-out 0s;
        }

        .login-v5 .rowR {
          display: flex; justify-content: space-between; align-items: center;
          margin: 10px 0 18px;
          font-size: 11px; letter-spacing: 0.08em;
          color: rgba(255,255,255,0.7);
        }
        .login-v5 .rowR label {
          display: inline-flex; align-items: center; gap: 8px; cursor: pointer;
          margin: 0; letter-spacing: 0.08em; text-transform: none;
          font-size: 11px; color: rgba(255,255,255,0.7);
        }
        .login-v5 .rowR input[type=checkbox] {
          width: 13px; height: 13px; accent-color: var(--a);
        }

        /* Brushed matte stainless-steel button. The base layer is a vertical
           tonal gradient (light → mid → light) for the rolled-edge look; a
           repeating linear gradient layered on top fakes the fine horizontal
           brush grain. A pseudo-element holds a diagonal sheen that sweeps
           across on hover. */
        .login-v5 .submit {
          position: relative; overflow: hidden;
          width: 100%; padding: 16px 28px;
          border-radius: 12px; cursor: pointer;
          font-family: 'JetBrains Mono', monospace;
          font-size: 12px; letter-spacing: 0.22em; text-transform: uppercase; font-weight: 600;
          color: #1a1a1f;
          background:
            repeating-linear-gradient(
              90deg,
              rgba(255,255,255,0.05) 0 1px,
              rgba(0,0,0,0.05) 1px 2px
            ),
            linear-gradient(
              180deg,
              #d8d9dc 0%,
              #b9bbc0 45%,
              #a8aab0 55%,
              #c6c8cc 100%
            );
          border: none;
          box-shadow:
            inset 0 1px 0 rgba(255,255,255,0.55),
            inset 0 -1px 0 rgba(0,0,0,0.25),
            0 1px 0 rgba(0,0,0,0.4),
            0 6px 18px -6px rgba(0,0,0,0.6);
          text-shadow: 0 1px 0 rgba(255,255,255,0.35);
          transition: box-shadow .2s ease, filter .2s ease, opacity .15s ease;
        }
        .login-v5 .submit::before {
          content: ""; position: absolute; inset: 0; pointer-events: none;
          background: linear-gradient(
            115deg,
            transparent 0%,
            transparent 35%,
            rgba(255,255,255,0.55) 50%,
            transparent 65%,
            transparent 100%
          );
          transform: translateX(-110%);
          transition: transform .55s ease;
        }
        .login-v5 .submit:hover:not(:disabled)::before { transform: translateX(110%); }
        .login-v5 .submit:active:not(:disabled) {
          filter: brightness(0.96);
          box-shadow:
            inset 0 2px 4px rgba(0,0,0,0.25),
            inset 0 -1px 0 rgba(255,255,255,0.4);
        }
        .login-v5 .submit:focus-visible {
          outline: 2px solid var(--a); outline-offset: 2px;
        }
        .login-v5 .submit:disabled { opacity: 0.55; cursor: not-allowed; filter: grayscale(0.3); }

      `}</style>
    </div>
  )
}
