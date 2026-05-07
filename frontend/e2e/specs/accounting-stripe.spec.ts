import { createHmac } from 'node:crypto'
import { expect, test, type APIRequestContext } from '@playwright/test'
import {
  API_URL,
  E2E_USERS,
  authHeaders,
  expectJson,
  isoDate,
  loginApi,
  type AdminCreditPackageDTO,
  type CreditBalanceResponse,
  type PurchaseCreditsResponse,
} from '../fixtures/api'
import { queryE2eSql, resetE2eData, runE2eSql } from '../fixtures/db'
import { readZipEntries } from '../fixtures/zip'

const E2E_STRIPE_WEBHOOK_SECRET = process.env.STRIPE_WEBHOOK_SECRET ?? 'whsec_e2e_local'

test.beforeEach(async () => {
  await resetE2eData()
})

async function getBalance(request: APIRequestContext, token: string): Promise<number> {
  const balance = await expectJson<CreditBalanceResponse>(
    await request.get(`${API_URL}/credits/balance`, { headers: authHeaders(token) })
  )
  return balance.balance
}

async function createCreditPackage(
  request: APIRequestContext,
  adminToken: string,
  name: string,
  credits = 250,
  priceCzk = 1500
): Promise<AdminCreditPackageDTO> {
  return expectJson<AdminCreditPackageDTO>(
    await request.post(`${API_URL}/admin/packages`, {
      headers: authHeaders(adminToken),
      data: {
        nameCs: name,
        nameEn: name,
        description: `${name} description`,
        credits,
        priceCzk,
        currency: 'CZK',
        isActive: true,
        sortOrder: 10,
        highlightType: 'NONE',
        isBasic: false,
      },
    }),
    201
  )
}

function signedStripeHeader(payload: string): string {
  const timestamp = Math.floor(Date.now() / 1000)
  const signature = createHmac('sha256', E2E_STRIPE_WEBHOOK_SECRET)
    .update(`${timestamp}.${payload}`)
    .digest('hex')
  return `t=${timestamp},v1=${signature}`
}

function checkoutSessionEventPayload(
  type: string,
  sessionId: string,
  paymentIntentId: string,
  paymentStatus = 'paid'
): string {
  return JSON.stringify({
    id: `evt_e2e_${Date.now()}`,
    object: 'event',
    api_version: '2024-06-20',
    created: Math.floor(Date.now() / 1000),
    livemode: false,
    pending_webhooks: 1,
    request: null,
    type,
    data: {
      object: {
        id: sessionId,
        object: 'checkout.session',
        payment_intent: paymentIntentId,
        payment_status: paymentStatus,
      },
    },
  })
}

test('simulated credit purchase completes locally and accounting period export includes HTML summary', async ({ request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client = await loginApi(request, E2E_USERS.client1)
  const creditPackage = await createCreditPackage(request, admin.accessToken, 'E2E Accounting Package', 300, 1800)
  const balanceBefore = await getBalance(request, client.accessToken)

  const purchase = await expectJson<PurchaseCreditsResponse>(
    await request.post(`${API_URL}/credits/purchase`, {
      headers: authHeaders(client.accessToken),
      data: { packageId: creditPackage.id },
    })
  )
  expect(purchase.status).toBe('completed')
  expect(purchase.gwUrl).toBeNull()
  expect(purchase.credits).toBe(creditPackage.credits)
  expect(purchase.newBalance).toBe(balanceBefore + creditPackage.credits)

  const today = isoDate(new Date())
  const exportResponse = await request.get(
    `${API_URL}/admin/export/accounting/period?start=${today}&end=${today}&syncStripe=false`,
    { headers: authHeaders(admin.accessToken) }
  )
  expect(exportResponse.status()).toBe(200)
  expect(exportResponse.headers()['content-type']).toContain('application/zip')
  expect(exportResponse.headers()['content-disposition']).toContain(`accounting-${today}_${today}.zip`)

  const entries = readZipEntries(Buffer.from(await exportResponse.body()))
  expect([...entries.keys()].sort()).toEqual([
    'balance_transactions.csv',
    'credit_movements.csv',
    'payments.csv',
    'payouts.csv',
    'souhrn.html',
    'summary.csv',
  ])

  const html = entries.get('souhrn.html')?.toString('utf8')
  const summaryCsv = entries.get('summary.csv')?.toString('utf8')
  const paymentsCsv = entries.get('payments.csv')?.toString('utf8')

  expect(html).toContain('E2E Accounting Package')
  expect(html).toContain(E2E_USERS.client1)
  expect(html).toContain('Stripe')
  expect(summaryCsv).toContain('gross_paid,1800.00,CZK')
  expect(summaryCsv).toContain('credits_sold,300,')
  expect(paymentsCsv).toContain('E2E Accounting Package')
  expect(paymentsCsv).toContain(E2E_USERS.client1)
})

test('signed Stripe checkout webhook completes a pending payment idempotently', async ({ request }) => {
  const admin = await loginApi(request, E2E_USERS.admin)
  const client = await loginApi(request, E2E_USERS.client2)
  const creditPackage = await createCreditPackage(request, admin.accessToken, 'E2E Webhook Package', 400, 2000)
  const balanceBefore = await getBalance(request, client.accessToken)
  const sessionId = `cs_test_e2e_${Date.now()}`
  const paymentIntentId = `pi_e2e_${Date.now()}`

  await runE2eSql(`
    INSERT INTO stripe_payments (
      id, user_id, stripe_session_id, amount, currency, status,
      credit_package_id, created_at, updated_at
    )
    SELECT gen_random_uuid(), users.id, '${sessionId}', ${creditPackage.priceCzk},
           'CZK', 'pending', '${creditPackage.id}'::uuid, NOW(), NOW()
    FROM users
    WHERE users.email = '${E2E_USERS.client2}';
  `)

  const payload = checkoutSessionEventPayload(
    'checkout.session.completed',
    sessionId,
    paymentIntentId
  )
  const invalidSignature = await request.post(`${API_URL}/stripe/webhook`, {
    headers: {
      'Content-Type': 'application/json',
      'Stripe-Signature': 't=123,v1=invalid',
    },
    data: payload,
  })
  expect(invalidSignature.status()).toBe(400)
  expect(await getBalance(request, client.accessToken)).toBe(balanceBefore)

  const completed = await request.post(`${API_URL}/stripe/webhook`, {
    headers: {
      'Content-Type': 'application/json',
      'Stripe-Signature': signedStripeHeader(payload),
    },
    data: payload,
  })
  expect(completed.status()).toBe(200)
  expect(await getBalance(request, client.accessToken)).toBe(balanceBefore + creditPackage.credits)

  const status = await expectJson<{ sessionId: string; status: string; amount: number }>(
    await request.get(`${API_URL}/stripe/status/${sessionId}`, {
      headers: authHeaders(client.accessToken),
    })
  )
  expect(status.sessionId).toBe(sessionId)
  expect(status.status).toBe('completed')
  expect(status.amount).toBe(creditPackage.priceCzk)

  const secondDelivery = await request.post(`${API_URL}/stripe/webhook`, {
    headers: {
      'Content-Type': 'application/json',
      'Stripe-Signature': signedStripeHeader(payload),
    },
    data: payload,
  })
  expect(secondDelivery.status()).toBe(200)
  expect(await getBalance(request, client.accessToken)).toBe(balanceBefore + creditPackage.credits)

  const purchaseTransactions = Number(await queryE2eSql(`
    SELECT COUNT(*)
    FROM credit_transactions
    WHERE user_id = (SELECT id FROM users WHERE email = '${E2E_USERS.client2}')
      AND stripe_payment_id = '${sessionId}'
      AND type = 'purchase';
  `))
  expect(purchaseTransactions).toBe(1)
})
