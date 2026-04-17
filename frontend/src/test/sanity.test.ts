import { describe, it, expect } from 'vitest'

// Smoke — proves vitest runs, jsdom env works, paths resolve.
describe('test infra sanity', () => {
  it('runs assertions', () => {
    expect(1 + 1).toBe(2)
  })

  it('has access to jsdom globals', () => {
    const el = document.createElement('div')
    el.textContent = 'hello'
    expect(el.textContent).toBe('hello')
  })
})
