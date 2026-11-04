/**
 * esbuild 受限环境替代模块（只在 scripts/build.mjs 探测到 esbuild 不可用时启用）
 *
 * 背景：Vite 5 在「生产构建」时会用 esbuild 做两件纯文本处理：
 *   1. 内置 define 替换（vite:define 插件）
 *   2. JS/CSS 压缩（vite:esbuild-transpile，本项目的构建脚本已关闭）
 * esbuild 本体通过「主进程 ↔ 子进程管道」工作，在禁止子进程管道的沙箱里会报 spawn EPERM。
 *
 * 因此这里提供一个纯 JS 的最小实现，只覆盖 define 替换：
 * 通过 node:module 的 registerHooks 把 Vite 内部 `import 'esbuild'` 的解析结果
 * 重定向到本文件，其余 Vite 行为完全不变。
 */
import { replaceDefines } from './define-replace.mjs'

/** 与 esbuild.transform 签名对齐：{ code, map } */
export function transform(code, options = {}) {
  return Promise.resolve({
    code: replaceDefines(String(code), options.define || {}),
    map: ''
  })
}

/** esbuild.transformSync 的最小实现 */
export function transformSync(code, options = {}) {
  return transform(code, options)
}

/** esbuild.formatMessages 的最小实现（Vite 用于格式化错误信息） */
export function formatMessages(messages) {
  return Promise.resolve((messages || []).map((m) => m.text || String(m)))
}

/** 构建/代码拆分能力不可用（本项目构建脚本已确保不会调用） */
export function build() {
  return Promise.reject(new Error('[esbuild-fallback] 当前环境只支持 transform，不支持 build'))
}

/** 版本号与真实依赖保持一致，避免 Vite 内部版本判断异常 */
export const version = '0.21.5'

export default { transform, transformSync, formatMessages, build, version }
