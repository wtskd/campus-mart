import './win-compat.mjs' // 必须最先生效（见文件内注释）
import { fileURLToPath } from 'node:url'

// 项目根目录（脚本可能从任意工作目录被调用）
const root = fileURLToPath(new URL('..', import.meta.url))

/**
 * 探测 esbuild 是否可用。
 * esbuild 依赖「主进程 <-> 子进程」的管道通信，某些受限沙箱环境会禁止创建子进程管道，
 * 此时 Vite 默认的 JS 压缩、define 替换都会失败。这里做一次极轻量的调用探测：
 *   - 可用   -> 走 Vite 默认的 esbuild 能力（产物更小，构建最快）
 *   - 不可用 -> 关闭 esbuild 插件与压缩，并把 define 替换换成纯 JS 实现，保证 dist 一定产出
 */
let esbuildUsable = false
try {
  const esbuild = await import('esbuild')
  await esbuild.transform('const answer = 42')
  esbuildUsable = true
} catch (error) {
  console.warn(
    '\n[build] esbuild 不可用，自动切换到内置纯 JS 降级方案（不影响产物可用性）。\n' +
      `[build] 原因：${error && (error.code || error.message)}\n`
  )
}

/**
 * 降级方案：把 Vite 内部对 "esbuild" 的 import 重定向到 scripts/esbuild-shim.mjs，
 * 只实现 define 替换（Vite 5 构建期仅用它做 define 替换和压缩，压缩已在此关闭）。
 * 使用 node:module 的同步 hooks，无需修改 node_modules。
 */
if (!esbuildUsable) {
  const { registerHooks } = await import('node:module')
  const shimUrl = new URL('./esbuild-shim.mjs', import.meta.url).href
  registerHooks({
    resolve(specifier, context, nextResolve) {
      if (specifier === 'esbuild') {
        return { url: shimUrl, shortCircuit: true }
      }
      return nextResolve(specifier, context)
    }
  })
  console.log('[build] 已将 esbuild 重定向到内置纯 JS 实现')
}

// 注意：vite / vite.config.js 必须在 hook 注册之后再动态 import，
// 否则 Vite 内部那行 `import esbuild from "esbuild"` 已完成解析，重定向不会生效。
const { build, mergeConfig } = await import('vite')
const { default: baseConfig } = await import('../vite.config.js')

/** 降级时需要覆盖的配置 */
const overrides = {
  // 关闭配置文件自动加载（配置已由本脚本 import 进来）
  configFile: false,
  root,
  build: {
    target: 'esnext',
    // 无 esbuild 时无法做 JS/CSS 压缩
    minify: esbuildUsable ? 'esbuild' : false,
    cssMinify: esbuildUsable
  }
}

if (!esbuildUsable) {
  // 彻底关闭 esbuild 插件（TS/JSX 转换、target 降级等）
  overrides.esbuild = false
}

const startedAt = Date.now()
try {
  await build(mergeConfig(baseConfig, overrides))
  console.log(`\n[build] 构建完成，耗时 ${((Date.now() - startedAt) / 1000).toFixed(1)}s，产物目录：dist/\n`)
} catch (error) {
  console.error('\n[build] 构建失败：\n', error)
  process.exitCode = 1
}
