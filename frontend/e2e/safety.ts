const DEFAULT_FRONTEND_URL = 'http://localhost:3000'
const DEFAULT_API_URL = 'http://localhost:8080/api'

const PRODUCTION_HOSTS = new Set([
  'rezervace-pankova.online',
  'www.rezervace-pankova.online',
])

const LOCAL_HOSTS = new Set([
  'localhost',
  '127.0.0.1',
  '::1',
])

function parseUrl(value: string, label: string): URL {
  try {
    return new URL(value)
  } catch {
    throw new Error(`Invalid ${label}: ${value}`)
  }
}

function assertLocalUrl(url: URL, label: string): void {
  const hostname = url.hostname.toLowerCase()

  if (PRODUCTION_HOSTS.has(hostname)) {
    throw new Error(
      `Refusing to run E2E tests against production ${label}: ${url.origin}. ` +
      'E2E tests mutate users, slots, reservations and credits.'
    )
  }

  if (!LOCAL_HOSTS.has(hostname)) {
    throw new Error(
      `Refusing to run E2E tests against non-local ${label}: ${url.origin}. ` +
      'CI must use the local docker-compose stack only.'
    )
  }
}

export function getSafeE2eUrls(): { frontendUrl: string; apiUrl: string } {
  const frontendUrl = process.env.E2E_BASE_URL ?? DEFAULT_FRONTEND_URL
  const apiUrl = process.env.E2E_API_URL ?? DEFAULT_API_URL

  if (process.env.CI !== 'true') {
    throw new Error(
      'E2E tests are allowed only in CI/CD (CI=true). ' +
      'This prevents accidental writes to production or shared databases.'
    )
  }

  assertLocalUrl(parseUrl(frontendUrl, 'E2E_BASE_URL'), 'frontend URL')
  assertLocalUrl(parseUrl(apiUrl, 'E2E_API_URL'), 'API URL')

  return { frontendUrl, apiUrl }
}
