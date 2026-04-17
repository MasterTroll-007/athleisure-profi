import { setupServer } from 'msw/node'
import { handlers } from './handlers'

// Shared MSW server used by every vitest test via `beforeAll(server.listen)`.
export const server = setupServer(...handlers)
