import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { loadLocalEnvironment } from './load-local-env.mjs'

const rootDirectory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
loadLocalEnvironment(rootDirectory)
const mode = process.argv[2]
if (!['info', 'migrate', 'seed-catalog-demo'].includes(mode)) {
  console.error('Usage: node scripts/run-schema.mjs info|migrate|seed-catalog-demo')
  process.exit(1)
}
const jar = path.join(rootDirectory, 'backend', 'target', 'household-goods-backend-0.1.0-SNAPSHOT.jar')
if (!existsSync(jar)) {
  console.error('Build the application first with npm run build.')
  process.exit(1)
}
const java = process.env.JAVA_HOME
  ? path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
  : 'java'
const result = spawnSync(java, ['-jar', jar, mode === 'migrate' ? '--migrate-schema' : mode === 'seed-catalog-demo' ? '--seed-catalog-demo' : '--schema-info'], {
  cwd: rootDirectory,
  env: process.env,
  stdio: 'inherit',
})
if (result.error) console.error('Unable to run Java. Configure JDK 21 and JAVA_HOME.')
process.exit(result.status ?? 1)
