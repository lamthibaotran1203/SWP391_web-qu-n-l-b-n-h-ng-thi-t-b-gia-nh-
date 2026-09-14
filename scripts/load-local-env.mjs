import { existsSync } from 'node:fs'
import path from 'node:path'

export function loadLocalEnvironment(rootDirectory) {
  const environmentFile = path.join(rootDirectory, '.env')
  if (!existsSync(environmentFile)) {
    return
  }

  const existingKeys = new Set(Object.keys(process.env))
  process.loadEnvFile(environmentFile)

  for (const [key, value] of Object.entries(process.env)) {
    if (value === '' && !existingKeys.has(key)) {
      delete process.env[key]
    }
  }
}
