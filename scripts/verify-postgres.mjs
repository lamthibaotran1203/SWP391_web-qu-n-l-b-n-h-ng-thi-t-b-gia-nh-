import { spawnSync } from 'node:child_process'
import path from 'node:path'
import os from 'node:os'
import { fileURLToPath } from 'node:url'
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const name = `homemart-verify-${process.pid}`
function run(args) {
  const r = spawnSync('docker', args, { cwd: root, stdio: 'inherit', env: { ...process.env, DB_PASSWORD: 'local_test_only' } })
  if (r.error || r.status !== 0) throw new Error('Docker verification command failed')
}
let exitCode = 0
try {
  run(['network', 'create', name])
  run(['run', '-d', '--name', name, '--network', name,
    '-e', 'POSTGRES_DB=homemart_test', '-e', 'POSTGRES_USER=app',
    '-e', 'POSTGRES_PASSWORD=local_test_only', '--health-cmd', 'pg_isready -U app -d homemart_test',
    '--health-interval', '1s', '--health-retries', '30', 'postgres:17.11'])
  run(['run', '--rm', '--network', name, 'postgres:17.11', 'sh', '-c',
    `for attempt in $(seq 1 30); do pg_isready -h ${name} -U app -d homemart_test && exit 0; sleep 1; done; exit 1`])
  run(['run', '--rm', '--network', name, '-v', `${root}:/workspace`,
    '-v', `${path.join(os.homedir(), '.m2')}:/root/.m2`, '-w', '/workspace/backend',
    '-e', `DB_URL=jdbc:postgresql://${name}:5432/homemart_test`, '-e', 'DB_USERNAME=app',
    '-e', 'DB_PASSWORD', 'maven:3.9.16-eclipse-temurin-21',
    'mvn', '-B', 'clean', 'verify'])
} catch (error) { console.error(error.message); exitCode = 1 }
finally {
  spawnSync('docker', ['rm', '-f', name], { stdio: 'ignore' })
  spawnSync('docker', ['network', 'rm', name], { stdio: 'ignore' })
}
process.exit(exitCode)
