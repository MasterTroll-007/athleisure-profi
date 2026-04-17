import { defineConfig, mergeConfig } from 'vitest/config'
import viteConfig from './vite.config'

// Vitest reuses the Vite config (alias `@/`, plugins) so imports match
// production source-tree layout without duplication.
export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      globals: true,
      setupFiles: ['./src/test/setup.ts'],
      exclude: ['node_modules', 'dist', 'e2e'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html', 'lcov'],
        reportsDirectory: './coverage',
        exclude: [
          'node_modules/',
          'dist/',
          'e2e/',
          '**/*.config.*',
          '**/*.d.ts',
          'src/main.tsx',
          'src/test/**',
          'src/i18n/**',
        ],
      },
    },
  })
)
