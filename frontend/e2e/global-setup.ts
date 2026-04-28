import { request } from '@playwright/test'
import { API_URL } from './fixtures/api'
import { resetE2eData } from './fixtures/db'

const FRONTEND_URL = process.env.E2E_BASE_URL ?? 'http://localhost:3000'

async function waitForHttp(url: string, timeoutMs = 120_000): Promise<void> {
  const startedAt = Date.now()
  const context = await request.newContext()
  try {
    while (Date.now() - startedAt < timeoutMs) {
      try {
        const response = await context.get(url, { timeout: 2_500 })
        if (response.status() < 500) return
      } catch {
        // Service is not reachable yet.
      }
      await new Promise((resolve) => setTimeout(resolve, 2_000))
    }
  } finally {
    await context.dispose()
  }

  throw new Error(`Timed out waiting for ${url}`)
}

export default async function globalSetup(): Promise<void> {
  await waitForHttp(`${API_URL}/health`)
  await waitForHttp(`${FRONTEND_URL}/login`)
  await resetE2eData()
}
