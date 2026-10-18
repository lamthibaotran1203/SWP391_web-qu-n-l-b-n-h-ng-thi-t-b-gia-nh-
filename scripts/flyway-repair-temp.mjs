import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { loadLocalEnvironment } from './load-local-env.mjs'

const rootDirectory = path.resolve('.')
const backendDirectory = path.join(rootDirectory, 'backend')

loadLocalEnvironment(rootDirectory)

const dbUrl = process.env.DB_URL
const dbUser = process.env.DB_USERNAME
const dbPass = process.env.DB_PASSWORD

console.log('Running flyway repair...')
console.log('DB URL:', dbUrl?.substring(0, 35) + '...')

const result = spawnSync('mvnw.cmd', [
  'flyway:repair',
  `-Dflyway.url=${dbUrl}`,
  `-Dflyway.user=${dbUser}`,
  `-Dflyway.password=${dbPass}`,
  '-Dflyway.schemas=app',
  '-Dflyway.defaultSchema=app',
  '-Dflyway.locations=classpath:db/migration',
], {
  cwd: backendDirectory,
  env: { ...process.env },
  stdio: 'inherit',
  shell: true,
})

process.exit(result.status ?? 1)
