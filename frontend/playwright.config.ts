import { defineConfig, devices } from '@playwright/test'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const isCi = !!process.env.CI
const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:4173'
const rootDir = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: isCi,
  retries: isCi ? 1 : 0,
  workers: 1,
  reporter: isCi ? [['html', { open: 'never' }], ['list']] : [['list']],
  timeout: 60_000,
  expect: { timeout: 10_000 },
  outputDir: './test-results',
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  globalSetup: path.resolve(rootDir, './e2e/support/global-setup.ts'),
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
})
