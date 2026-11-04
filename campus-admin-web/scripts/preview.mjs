import { preview, mergeConfig } from 'vite'
import baseConfig from '../vite.config.js'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('..', import.meta.url))

const server = await preview(
  mergeConfig(baseConfig, {
    configFile: false,
    root,
    preview: { host: '127.0.0.1', port: 4173 }
  })
)
server.printUrls()
