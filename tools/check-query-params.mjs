/**
 * 后台列表页「筛选参数」有效性检查
 *
 * 背景：操作日志页曾把筛选参数写成 module=/operator=，后端只认 title=/operName=，
 * 结果点「查询」页面毫无变化，看起来像"用空值在筛选"。登录日志也踩过同样的坑
 * （前端发 username=，后端 @RequestParam 是 userName=）。
 *
 * 做法（黑盒，不需要读后端源码）：
 *   1. 从页面源码取出查询对象里用到的 key（v-model="query.x" 与 useTable 初始值）；
 *   2. 先不带筛选调一次列表接口，记下 total；
 *   3. 再带上该 key=不可能命中的值调一次，看 total 是否变化：
 *        total 变小 → 参数生效
 *        total 不变 → 参数被后端忽略（筛选失效，需要改名）
 *        HTTP 4xx   → 参数被绑定但类型不符（算生效，标记 WARN）
 *
 * 用法：node tools/check-query-params.mjs [baseUrl]
 * 前提：后端已启动（默认 http://127.0.0.1:8081）。
 */
import { readFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const VIEWS = join(ROOT, 'campus-admin-web', 'src', 'views')
const BASE = (process.argv[2] || 'http://127.0.0.1:8081').replace(/\/+$/, '')

/** 页面 -> 列表接口（同一页面的筛选都发到这个接口） */
const TARGETS = [
  { page: 'system/user/index.vue', url: '/api/system/user/list?current=1&size=5' },
  { page: 'system/role/index.vue', url: '/api/system/role/list?current=1&size=5' },
  { page: 'system/config/index.vue', url: '/api/system/config/list?current=1&size=5' },
  { page: 'system/operlog/index.vue', url: '/api/system/operlog/list?current=1&size=5' },
  { page: 'system/loginlog/index.vue', url: '/api/system/loginlog/list?current=1&size=5' },
  { page: 'system/file/index.vue', url: '/api/system/file/list?current=1&size=5' },
  { page: 'item/list/index.vue', url: '/api/item/item/page?current=1&size=5' },
  { page: 'item/audit/index.vue', url: '/api/item/item/page?current=1&size=5&status=1' },
  { page: 'item/order/index.vue', url: '/api/item/order/page?current=1&size=5' },
  { page: 'item/comment/index.vue', url: '/api/item/comment/page?current=1&size=5' },
  { page: 'item/report/index.vue', url: '/api/item/report/page?current=1&size=5' },
  { page: 'item/message/index.vue', url: '/api/item/message/page?current=1&size=5' },
  { page: 'system/notice/index.vue', url: '/api/item/notice/page?current=1&size=5' },
  { page: 'system/banner/index.vue', url: '/api/item/banner/page?current=1&size=5' },
  { page: 'ai/prompt/index.vue', url: '/api/ai/prompt/page?current=1&size=5' },
  { page: 'ai/knowledge/index.vue', url: '/api/ai/knowledge/page?current=1&size=5' },
  { page: 'ai/log/index.vue', url: '/api/ai/log/page?current=1&size=5' }
]

/** 这些 key 不是筛选条件（分页/时间区间/前端内部状态） */
const IGNORED_KEYS = new Set([
  'current', 'size', 'page', 'pageNum', 'pageSize', 'total', 'params',
  'beginTime', 'endTime', 'dateRange', 'loading'
])

async function login() {
  const res = await fetch(`${BASE}/api/auth/app/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  })
  const json = await res.json()
  const token = json?.data?.accessToken || json?.data?.token
  if (!token) throw new Error('登录失败：' + JSON.stringify(json))
  return token
}

/** 从页面源码取 query 对象里用到的 key */
function extractQueryKeys(file) {
  const source = readFileSync(file, 'utf8')
  const keys = new Set()
  let m
  const reModel = /v-model="query\.([A-Za-z_$][\w$]*)"/g
  while ((m = reModel.exec(source))) keys.add(m[1])
  const reUse = /useTable\(\s*[^,]+,\s*\{([^}]*)\}/g
  while ((m = reUse.exec(source))) {
    const body = m[1]
    const reKey = /([A-Za-z_$][\w$]*)\s*:/g
    let k
    while ((k = reKey.exec(body))) keys.add(k[1])
  }
  for (const key of keys) if (IGNORED_KEYS.has(key)) keys.delete(key)
  return [...keys]
}

function totalOf(json) {
  const data = json?.data
  if (Array.isArray(data)) return data.length
  if (typeof data?.total === 'number') return data.total
  if (typeof data?.total === 'string') return Number(data.total)
  return null
}

/** 数值型字段用 -999999，文本型用哨兵串；两种情况都不可能命中演示数据 */
function probeValue(key) {
  return /(id|Id|status|type|isRead|read|sort|days|count|num)$/.test(key) ? '-999999' : '__no_match__'
}

const token = await login()
const headers = { Authorization: `Bearer ${token}` }

async function call(url) {
  const res = await fetch(BASE + url, { headers })
  let json = null
  try {
    json = await res.json()
  } catch {
    /* 非 JSON 响应（如 CSV 导出）忽略 */
  }
  return { status: res.status, json }
}

let checked = 0
let problems = 0
let warns = 0
let skipped = 0
const failures = []

for (const target of TARGETS) {
  let keys
  try {
    keys = extractQueryKeys(join(VIEWS, target.page))
  } catch {
    console.log(`[SKIP] ${target.page} 读不到文件`)
    skipped += 1
    continue
  }
  if (!keys.length) {
    console.log(`[SKIP] ${target.page} 没有筛选条件`)
    skipped += 1
    continue
  }
  const baseRes = await call(target.url)
  const baseTotal = totalOf(baseRes.json)
  if (baseTotal == null) {
    console.log(`[SKIP] ${target.page} 接口未返回 total（HTTP ${baseRes.status}）`)
    skipped += 1
    continue
  }
  const bad = []
  for (const key of keys) {
    checked += 1
    const sep = target.url.includes('?') ? '&' : '?'
    const probe = await call(`${target.url}${sep}${key}=${encodeURIComponent(probeValue(key))}`)
    const got = totalOf(probe.json)
    if (probe.status >= 400) {
      // 参数被后端绑定、只是类型不符（或后端主动校验）——算生效
      warns += 1
      console.log(`  [WARN] ${target.page}  ${key} → HTTP ${probe.status}（参数已被绑定，值类型不符）`)
      continue
    }
    if (got == null) {
      warns += 1
      console.log(`  [WARN] ${target.page}  ${key} → 无 total，跳过判定`)
      continue
    }
    if (got === baseTotal) {
      problems += 1
      bad.push(key)
      console.log(`  [FAIL] ${target.page}  ${key} 被后端忽略（total 仍为 ${baseTotal}）`)
    }
  }
  if (bad.length) failures.push({ page: target.page, keys: bad, baseTotal })
  else console.log(`[ OK ] ${target.page}（${keys.length} 个筛选参数）`)
}

console.log('')
console.log(`RESULT: checked=${checked} problems=${problems} warns=${warns} skipped=${skipped}`)
if (failures.length) {
  console.log('被忽略的筛选参数：')
  for (const f of failures) console.log(`  - ${f.page}: ${f.keys.join(', ')}`)
}
process.exit(problems ? 1 : 0)
