import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import type { FullConfig } from '@playwright/test'

const requiredEnv = ['E2E_DB_NAME', 'E2E_DB_USER', 'E2E_DB_PASSWORD'] as const

function assertEnv(name: (typeof requiredEnv)[number]): string {
  const value = process.env[name]
  if (!value) {
    throw new Error(`Missing required env ${name} for Playwright seed setup.`)
  }
  return value
}

export default async function globalSetup(_config: FullConfig) {
  const dbName = assertEnv('E2E_DB_NAME')
  const dbUser = assertEnv('E2E_DB_USER')
  const dbPassword = assertEnv('E2E_DB_PASSWORD')
  const dbHost = process.env.E2E_DB_HOST || '127.0.0.1'
  const dbPort = process.env.E2E_DB_PORT || '3306'
  const mysqlBin = process.env.E2E_MYSQL_BIN || 'mysql'
  const currentDir = path.dirname(fileURLToPath(import.meta.url))
  const seedFile = path.resolve(currentDir, '../fixtures/e1_seed.sql')

  if (!fs.existsSync(seedFile)) {
    throw new Error(`Playwright seed file not found: ${seedFile}`)
  }

  execFileSync(
    mysqlBin,
    [
      `--host=${dbHost}`,
      `--port=${dbPort}`,
      `--user=${dbUser}`,
      `--password=${dbPassword}`,
      '--default-character-set=utf8mb4',
      dbName
    ],
    { stdio: ['pipe', 'inherit', 'inherit'], input: fs.readFileSync(seedFile) }
  )

  const apiBase = process.env.E2E_API_BASE_URL || 'http://127.0.0.1:8089/api'
  const response = await fetch(`${apiBase}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'e1_user', password: 'test123456' })
  })
  const body = await response.json() as { code?: number; message?: string }
  if (!response.ok || body.code !== 0) {
    throw new Error(`Seed login verification failed: HTTP ${response.status} code=${body.code} message=${body.message ?? 'unknown'}`)
  }
}
