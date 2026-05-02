import { afterEach, describe, expect, it } from 'vitest'
import { getSafeE2eUrls } from '../../e2e/safety'

const originalEnv = { ...process.env }

describe('E2E safety guard', () => {
  afterEach(() => {
    process.env.CI = originalEnv.CI
    process.env.E2E_BASE_URL = originalEnv.E2E_BASE_URL
    process.env.E2E_API_URL = originalEnv.E2E_API_URL
  })

  it('refuses to run outside CI', () => {
    process.env.CI = 'false'
    process.env.E2E_BASE_URL = 'http://localhost:3000'
    process.env.E2E_API_URL = 'http://localhost:8080/api'

    expect(() => getSafeE2eUrls()).toThrow(/allowed only in CI\/CD/)
  })

  it('refuses production URLs even in CI', () => {
    process.env.CI = 'true'
    process.env.E2E_BASE_URL = 'https://rezervace-pankova.online'
    process.env.E2E_API_URL = 'http://localhost:8080/api'

    expect(() => getSafeE2eUrls()).toThrow(/Refusing to run E2E tests against production/)
  })

  it('accepts local frontend and API URLs in CI', () => {
    process.env.CI = 'true'
    process.env.E2E_BASE_URL = 'http://localhost:3000'
    process.env.E2E_API_URL = 'http://127.0.0.1:8080/api'

    expect(getSafeE2eUrls()).toEqual({
      frontendUrl: 'http://localhost:3000',
      apiUrl: 'http://127.0.0.1:8080/api',
    })
  })
})
