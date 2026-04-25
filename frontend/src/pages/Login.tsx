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
      setError(null)
      const response = await authApi.login(data.email, data.password, rememberMe)
      setAuth(response)
      navigate('/')
    } catch {
      setError(t('errors.invalidCredentials'))
    }
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
        <form className="loginForm" onSubmit={handleSubmit(onSubmit)} noValidate>
          {error && <div className="err">{error}</div>}

          <div className="f">
            <label htmlFor="email">{t('auth.email')}</label>
            <input
              id="email"
              type="email"
              placeholder={t('auth.emailPlaceholder')}
              autoComplete="email"
              autoFocus
              aria-invalid={!!errors.email}
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
              aria-invalid={!!errors.password}
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

          <div className="alt">
            <span>{t('auth.noAccount')} </span>
            <Link to="/register">{t('auth.landingCreate')}</Link>
          </div>
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
          -webkit-mask-image: linear-gradient(180deg, transparent 0%, #000 20%, #000 80%, transparent 100%);
                  mask-image: linear-gradient(180deg, transparent 0%, #000 20%, #000 80%, transparent 100%);
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
          background: rgba(255, 90, 90, 0.10);
          border: 1px solid rgba(255, 90, 90, 0.35);
          color: #ffb0b0;
          padding: 10px 12px; border-radius: 10px;
          font-family: 'Outfit', sans-serif; font-size: 13px;
          margin-bottom: 14px;
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

        .login-v5 .submit {
          width: 100%; padding: 16px 28px;
          border-radius: 12px; border: none; cursor: pointer;
          font-family: 'JetBrains Mono', monospace;
          font-size: 12px; letter-spacing: 0.22em; text-transform: uppercase; font-weight: 500;
          color: #0b0a0e; background: #fff;
          box-shadow:
            0 0 0 1px var(--a),
            0 12px 50px -6px rgba(255,140,70,.6),
            0 0 40px rgba(255,140,70,.4);
          transition: transform .12s ease, opacity .15s ease;
        }
        .login-v5 .submit:hover:not(:disabled) { transform: translateY(-1px); }
        .login-v5 .submit:disabled { opacity: 0.6; cursor: not-allowed; }

        .login-v5 .alt {
          text-align: center; margin-top: 16px;
          font-size: 11px; color: rgba(255,255,255,0.65); letter-spacing: 0.04em;
        }
        .login-v5 .alt a {
          color: var(--a); text-decoration: none;
          border-bottom: 1px solid rgba(255,179,71,0.4);
          padding-bottom: 1px;
        }
      `}</style>
    </div>
  )
}
