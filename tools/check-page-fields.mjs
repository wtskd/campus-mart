/**
 * 后台列表页字段一致性检查
 *
 * 背景：操作日志页曾把 `title / businessType / operName / costTime`
 * 写成 `module / operType / operator / costMs`，表格整列为空，筛选参数后端也识别不了
 * （前端发 module=、后端只认 title=），看起来像"用空值筛选"。
 *
 * 这个脚本做的事：
 *   1. 对每个列表页，取 `<el-table-column ... prop="x">` 里的字段名
 *      （列内容全写在 template 里、没有 prop 的表格，退回用 `row.x` 里出现的字段）；
 *   2. 真的调一次对应后端接口，拿第一条记录的真实字段名；
 *   3. 两边比对，列出「页面声明了但接口没返回」的字段（= 必然是空列）。
 *
 * 用法：node tools/check-page-fields.mjs [baseUrl]
 * 前提：后端已启动（默认 http://127.0.0.1:8081）。
 */
import { readFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const VIEWS = join(ROOT, 'campus-admin-web', 'src', 'views')
const BASE = (process.argv[2] || 'http://127.0.0.1:8081').replace(/\/+$/, '')

/** 页面 -> 用于取样本记录的接口 */
const TARGETS = [
  { page: 'system/user/index.vue', url: '/api/system/user/list?current=1&size=1' },
  { page: 'system/role/index.vue', url: '/api/system/role/list?current=1&size=1' },
  { page: 'system/menu/index.vue', url: '/api/system/menu/list' },
  { page: 'system/dept/index.vue', url: '/api/system/dept/list' },
  { page: 'system/config/index.vue', url: '/api/system/config/list?current=1&size=1' },
  { page: 'system/operlog/index.vue', url: '/api/system/operlog/list?current=1&size=1' },
  { page: 'system/loginlog/index.vue', url: '/api/system/loginlog/list?current=1&size=1' },
  { page: 'system/file/index.vue', url: '/api/system/file/list?current=1&size=1' },
  { page: 'system/dict/index.vue', url: '/api/system/dict/type/list?current=1&size=1' },
  { page: 'item/list/index.vue', url: '/api/item/item/page?current=1&size=1' },
  { page: 'item/audit/index.vue', url: '/api/item/item/page?current=1&size=1&status=1' },
  { page: 'item/category/index.vue', url: '/api/item/category/list' },
  { page: 'item/order/index.vue', url: '/api/item/order/page?current=1&size=1' },
  { page: 'item/comment/index.vue', url: '/api/item/comment/page?current=1&size=1' },
  { page: 'item/report/index.vue', url: '/api/item/report/page?current=1&size=1' },
  { page: 'item/message/index.vue', url: '/api/item/message/page?current=1&size=1' },
  { page: 'system/notice/index.vue', url: '/api/item/notice/page?current=1&size=1' },
  { page: 'system/banner/index.vue', url: '/api/item/banner/page?current=1&size=1' },
  { page: 'ai/prompt/index.vue', url: '/api/ai/prompt/page?current=1&size=1' },
  { page: 'ai/knowledge/index.vue', url: '/api/ai/knowledge/page?current=1&size=1' },
  { page: 'ai/log/index.vue', url: '/api/ai/log/page?current=1&size=1' }
]

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

/** 截取 <el-table ...> 到它的结束标签，避免把弹窗表单字段也算进来 */
function tableBlock(file) {
  const source = readFileSync(file, 'utf8')
  const start = source.indexOf('<el-table')
  if (start < 0) return ''
  const end = source.indexOf('</el-table>', start)
  return end > start ? source.slice(start, end) : source.slice(start)
}

function extractTableProps(file) {
  const table = tableBlock(file)
  const props = []
  const re = /<el-table-column[^>]*\bprop="([^"]+)"/g
  let m
  while ((m = re.exec(table))) props.push(m[1])
  return [...new Set(props)]
}

/** 没有 prop 的表格（列内容全在 template 里）退一步：从 row.xxx 里取字段名 */
function extractRowFields(file) {
  const table = tableBlock(file)
  const fields = []
  const re = /\brow\.([A-Za-z_$][\w$]*)/g
  let m
  while ((m = re.exec(table))) if (!m[1].startsWith('$')) fields.push(m[1])
  return [...new Set(fields)]
}

function pick(record, path) {
  return path.split('.').reduce((acc, key) => (acc == null ? acc : acc[key]), record)
}

function firstRecord(json) {
  const data = json?.data
  if (Array.isArray(data)) return data[0]
  if (Array.isArray(data?.records)) return data.records[0]
  if (Array.isArray(data?.list)) return data.list[0]
  return null
}

const token = await login()
const headers = { Authorization: `Bearer ${token}` }

let checked = 0
let problems = 0
let skipped = 0

for (const target of TARGETS) {
  const file = join(VIEWS, target.page)
  let props
  try {
    props = extractTableProps(file)
    if (!props.length) props = extractRowFields(file)
  } catch (error) {
    console.log(`[SKIP] ${target.page} 读不到文件`)
    skipped += 1
    continue
  }
  if (!props.length) {
    console.log(`[SKIP] ${target.page} 模板里找不到可校验的字段`)
    skipped += 1
    continue
  }

  let record = null
  try {
    const res = await fetch(BASE + target.url, { headers })
    record = firstRecord(await res.json())
  } catch (error) {
    console.log(`[SKIP] ${target.page} 接口请求失败：${error.message}`)
    skipped += 1
    continue
  }
  if (!record) {
    console.log(`[SKIP] ${target.page} 接口没有返回记录（表可能为空），无法比对`)
    skipped += 1
    continue
  }

  const missing = props.filter((prop) => pick(record, prop) === undefined)
  checked += 1
  if (missing.length) {
    problems += 1
    console.log(`[FAIL] ${target.page}`)
    console.log(`       页面声明但接口未返回：${missing.join(', ')}`)
    console.log(`       接口实际字段：${Object.keys(record).join(', ')}`)
  } else {
    console.log(`[ OK ] ${target.page}（${props.length} 个字段）`)
  }
}

console.log(`\nRESULT: checked=${checked} problems=${problems} skipped=${skipped}`)
process.exit(problems ? 1 : 0)
