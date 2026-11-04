// 必须先于 vite 引入：绕过 Windows 下 Vite 的 `net use` 子进程探测（受限沙箱会 EPERM）
import './win-compat.mjs'
import { createServer, mergeConfig } from 'vite'
import baseConfig from '../vite.config.js'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('..', import.meta.url))

// 解析 --mode xxx / --mock 参数
const argv = process.argv.slice(2)
let mode = 'development'
for (let i = 0; i < argv.length; i++) {
  if (argv[i] === '--mode' && argv[i + 1]) mode = argv[++i]
  else if (argv[i].startsWith('--mode=')) mode = argv[i].slice('--mode='.length)
  else if (argv[i] === '--mock') mode = 'mock'
}

// 同 build.mjs：探测 esbuild 是否可用，不可用时关闭依赖预构建
let esbuildUsable = false
try {
  const esbuild = await import('esbuild')
  await esbuild.transform('const answer = 42')
  esbuildUsable = true
} catch (error) {
  console.warn(
    '\n[dev] esbuild 不可用，关闭依赖预构建（浏览器将按 ESM 原样加载 node_modules）。\n' +
      `[dev] 原因：${error && (error.code || error.message)}\n`
  )
}

const overrides = {
  configFile: false,
  root,
  mode,
  server: { host: '127.0.0.1', port: 5180 }
}

if (!esbuildUsable) {
  overrides.esbuild = false
  // noDiscovery + 空 include = 关闭依赖预构建（Vite 5.1+ 的官方方式）
  overrides.optimizeDeps = { noDiscovery: true, include: [] }
}

const server = await createServer(mergeConfig(baseConfig, overrides))
await server.listen()
server.printUrls()
