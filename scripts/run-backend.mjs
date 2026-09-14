import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import { loadLocalEnvironment } from './load-local-env.mjs'

const rootDirectory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const backendDirectory = path.join(rootDirectory, 'backend')
const wrapper = process.platform === 'win32' ? 'mvnw.cmd' : './mvnw'

loadLocalEnvironment(rootDirectory)

const result = spawnSync(wrapper, ['spring-boot:run'], {
  cwd: backendDirectory,
  env: {
    ...process.env,
    SPRING_PROFILES_ACTIVE: process.env.SPRING_PROFILES_ACTIVE || 'local',
  },
  stdio: 'inherit',
  shell: process.platform === 'win32',
})

process.exit(result.status ?? 1)
