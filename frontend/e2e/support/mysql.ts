import { execFileSync } from 'node:child_process'

function required(name: string): string {
  const value = process.env[name]
  if (!value) throw new Error(`Missing required env ${name} for E2E MySQL assertions.`)
  return value
}

export function mysqlExec(sql: string): string {
  const output = execFileSync(
    process.env.E2E_MYSQL_BIN || 'mysql',
    [
      `--host=${process.env.E2E_DB_HOST || '127.0.0.1'}`,
      `--port=${process.env.E2E_DB_PORT || '3306'}`,
      `--user=${required('E2E_DB_USER')}`,
      `--password=${required('E2E_DB_PASSWORD')}`,
      '--default-character-set=utf8mb4',
      '-N',
      '-B',
      required('E2E_DB_NAME'),
      '-e',
      sql
    ],
    { encoding: 'utf8' }
  )
  return output.replace(/\s+$/g, '')
}

export function mysqlRows(sql: string): string[][] {
  const output = mysqlExec(sql)
  if (!output) return []
  return output.split(/\r?\n/).map((line) => line.split('\t'))
}
