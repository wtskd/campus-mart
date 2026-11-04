/**
 * 静态比对：前端声明的 (method, path) 是否都能在后端 controller 里找到对应映射。
 * 目的：抓出「前端调 POST，后端只写了 PUT」这类运行时才 405 的坑。
 * 用法：node tools/check-method-match.mjs
 */
import fs from 'node:fs'
import path from 'node:path'

import { fileURLToPath } from 'node:url'
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

/* ---------------- 工具 ---------------- */
function walk(dir, filter, acc = []) {
  if (!fs.existsSync(dir)) return acc
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name)
    if (e.isDirectory()) { if (e.name !== 'node_modules' && e.name !== 'dist') walk(p, filter, acc) }
    else if (filter(e.name)) acc.push(p)
  }
  return acc
}

/** 把各种动态段统一成 {x} */
function normalize(p) {
  let s = p
  s = s.replace(/\$\{[^}]+\}/g, '{x}')
  s = s.replace(/\{[^}]+\}/g, '{x}')
  s = s.replace(/:[A-Za-z_]\w*/g, '{x}')
  s = s.replace(/\/+/g, '/')
  if (s.length > 1 && s.endsWith('/')) s = s.slice(0, -1)
  return s
}

/* ---------------- 1. 前端声明的接口 ---------------- */
const feFiles = [
  ...walk(path.join(root, 'campus-admin-web/src/api'), (n) => n.endsWith('.js')),
  ...walk(path.join(root, 'campus-app/src/api'), (n) => n.endsWith('.js'))
]

const frontend = new Map() // "METHOD /path" -> 出处
for (const f of feFiles) {
  const src = fs.readFileSync(f, 'utf8')
  const pm = /(?:const|let|var)\s+PREFIX\s*=\s*['"`]([^'"`]*)['"`]/.exec(src)
  const prefix = pm ? pm[1] : ''
  const resolve = (raw) => {
    let u = raw.split('?')[0].trim()
    u = u.replace(/\$\{PREFIX\}/g, prefix)
    if (!u.startsWith('/')) u = '/' + u
    return normalize(u)
  }
  // request.xxx('path')
  const reA = /request\.(get|post|put|delete|patch)\s*\(\s*[`'"]([^`'"]+)[`'"]/g
  let m
  while ((m = reA.exec(src))) {
    frontend.set(`${m[1].toUpperCase()} ${resolve(m[2])}`, path.relative(root, f))
  }
  // request({ url: '...', method: 'xxx' })
  const reB = /url:\s*[`'"]([^`'"]+)[`'"]/g
  while ((m = reB.exec(src))) {
    const around = src.slice(Math.max(0, m.index - 200), m.index + 260)
    const mm = /method:\s*['"](\w+)['"]/i.exec(around)
    const method = (mm ? mm[1] : 'get').toUpperCase()
    frontend.set(`${method} ${resolve(m[1])}`, path.relative(root, f))
  }
}

/* ---------------- 2. 后端映射 ---------------- */
const ctrlFiles = walk(path.join(root, 'campus-modules'), (n) => n.endsWith('Controller.java'))
  .concat(walk(path.join(root, 'campus-auth'), (n) => n.endsWith('Controller.java')))

const backend = new Set()
for (const f of ctrlFiles) {
  const lines = fs.readFileSync(f, 'utf8').split('\n')
  for (let i = 0; i < lines.length; i++) {
    const clsBase = /@RequestMapping\(\s*(?:value\s*=\s*)?(\{[^}]*\}|"[^"]*")/.exec(lines[i])
    // 取最后一个类级 @RequestMapping（粗粒度：扫全文件找第一个出现在 class 前的）
    if (!clsBase) continue
    const bases = [...clsBase[1].matchAll(/"([^"]*)"/g)].map((x) => x[1])
    if (!bases.length) continue
    // 只认第一次出现的（类级）
    for (let j = i + 1; j < lines.length; j++) {
      // 多方法映射：@RequestMapping(value = "/{id}/pay", method = {RequestMethod.POST, RequestMethod.PUT})
      const rm = /@RequestMapping\(\s*(?:value\s*=\s*)?"([^"]*)"\s*,\s*method\s*=\s*\{?([^)}]*)\}?\s*\)/.exec(lines[j])
      if (rm && /RequestMethod\./.test(rm[2])) {
        const verbs = [...rm[2].matchAll(/RequestMethod\.(\w+)/g)].map((x) => x[1].toUpperCase())
        for (const b of bases) {
          for (const v of verbs) {
            backend.add(`${v} ${normalize((b + '/' + rm[1]).replace(/\/+/g, '/'))}`)
          }
        }
        continue
      }
      const mm = /@(Get|Post|Put|Delete|Patch)Mapping\b\s*(?:\(\s*(?:value\s*=\s*)?(\{[^}]*\}|"[^"]*")?\s*\))?/.exec(lines[j])
      if (!mm) {
        if (/^\s*(public|private|protected)\s/.test(lines[j]) && !/@\w+Mapping/.test(lines[j])) {
          // 方法体里不再找
        }
        continue
      }
      const verb = mm[1].toUpperCase()
      const subs = mm[2] ? [...mm[2].matchAll(/"([^"]*)"/g)].map((x) => x[1]) : ['']
      for (const b of bases) {
        for (const s of subs) {
          const full = normalize((b + '/' + s).replace(/\/+/g, '/'))
          backend.add(`${verb} ${full}`)
        }
      }
    }
    break // 只处理类级第一个
  }
}

/* ---------------- 3. 比对 ---------------- */
const missing = []
for (const [key, file] of frontend) {
  if (!backend.has(key)) missing.push({ key, file })
}

console.log(`前端声明接口: ${frontend.size}`)
console.log(`后端映射接口: ${backend.size}`)
console.log(`\n前端调用但后端没有对应 映射（会 404/405）：${missing.length}`)
for (const m of missing.sort((a, b) => a.key.localeCompare(b.key))) {
  console.log(`  [MISS] ${m.key.padEnd(58)}  <- ${m.file}`)
}
process.exit(missing.length ? 1 : 0)
