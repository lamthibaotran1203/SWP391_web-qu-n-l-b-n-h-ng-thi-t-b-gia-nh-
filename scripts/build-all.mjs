import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const rootDirectory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const frontendDirectory = path.join(rootDirectory, 'frontend')
const backendDirectory = path.join(rootDirectory, 'backend')
const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm'
const mavenWrapper = process.platform === 'win32' ? 'mvnw.cmd' : './mvnw'

function run(command, args, cwd) {
  const result = spawnSync(command, args, {
    cwd,
    env: process.env,
    stdio: 'inherit',
    shell: process.platform === 'win32',
  })

  if (result.status !== 0) {
    process.exit(result.status ?? 1)
  }
}

run(npmCommand, ['ci'], frontendDirectory)
run(npmCommand, ['run', 'build'], frontendDirectory)
run(mavenWrapper, ['clean', 'package', '-DskipTests'], backendDirectory)
