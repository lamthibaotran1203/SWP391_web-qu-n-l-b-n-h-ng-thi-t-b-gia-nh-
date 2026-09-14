import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import { loadLocalEnvironment } from './load-local-env.mjs'

const rootDirectory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const backendDirectory = path.join(rootDirectory, 'backend')
const wrapper = process.platform === 'win32' ? 'mvnw.cmd' : './mvnw'
const args = process.argv.slice(2)

loadLocalEnvironment(rootDirectory)

const result = spawnSync(wrapper, args, {
  cwd: backendDirectory,
  env: process.env,
  stdio: 'inherit',
  shell: process.platform === 'win32',
})

process.exit(result.status ?? 1)
