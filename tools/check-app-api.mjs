/**
 * 小程序/H5（campus-app）接口契约探测
 * - 从 src/api/*.js 静态提取 request.get(...) 的路径
 * - 登录 student01 后带 token 逐条实发 GET
 * - 汇总 200 / 4xx / 5xx，非 JSON（文件下载）按 200 计
 *
 * 用法：node tools/check-app-api.mjs [baseUrl]
 */
import fs from 'node:fs'
import path from 'node:path'

const BASE = (process.argv[2] || 'http://127.0.0.1:8081').replace(/\/$/, '')
import { fileURLToPath } from 'node:url'
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const API_DIR = path.join(ROOT, 'campus-app/src/api')

const files = fs.readdirSync(API_DIR).filter((f) => f.endsWith('.js'))
const RE = /request\.(get|post|put|del)\(\s*[`'"]([^`'"]+)[`'"]/g

const calls = []
for (const f of files) {
  const src = fs.readFileSync(path.join(API_DIR, f), 'utf8')
  let m
  while ((m = RE.exec(src))) {
    calls.push({ file: f, method: m[1], url: m[2] })
  }
}

const samples = { itemId: 3254, orderId: 0, noticeId: 1, categoryId: 101, commentId: 1, messageId: 1 }

function fill(url) {
  return url.replace(/\$\{(\w+)\}/g, (_, k) => String(samples[k] ?? 1))
}

async function login() {
  const r = await fetch(`${BASE}/api/auth/app/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'student01', password: 'admin123' })
  })
  const j = await r.json()
  return j?.data?.token || ''
}

const token = await login()
if (!token) {
  console.log('登录失败，无法探测需要登录的接口（继续匿名探测）')
}

// 先补一个真实订单号
try {
  const r = await fetch(`${BASE}/api/item/app/order/my?current=1&size=1&role=buy`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  const j = await r.json()
  const first = j?.data?.records?.[0]
  if (first) samples.orderId = first.orderId
} catch (e) {}

const results = []
for (const c of calls) {
  if (c.method !== 'get') continue
  const url = fill(c.url)
  try {
    const res = await fetch(BASE + '/api' + url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      signal: AbortSignal.timeout(15000)
    })
    const text = await res.text()
    let code = res.status
    let msg = ''
    try {
      const j = JSON.parse(text)
      code = j.code ?? res.status
      msg = j.msg || ''
    } catch (e) {
      msg = '非 JSON'
    }
    results.push({ file: c.file, method: 'GET', url, http: res.status, code, msg })
  } catch (e) {
    results.push({ file: c.file, method: 'GET', url, http: 0, code: -1, msg: e.message })
  }
}

let ok = 0
let bad = 0
for (const r of results) {
  const pass = r.code === 200
  if (pass) ok++
  else bad++
  console.log(`${pass ? 'OK  ' : 'FAIL'} [${r.file}] GET ${r.url} -> http=${r.http} code=${r.code} ${r.msg}`)
}
console.log(`\nTOTAL=${results.length} OK=${ok} FAIL=${bad}`)
