/**
 * 重算数据分析的 4 张预聚合表（stat_overview_daily / stat_category_daily / stat_funnel_daily / stat_user_rfm）。
 *
 * 什么时候需要跑：导入了 sql/05-rich-data.sql（会清空并重建 biz_* 与 stat_* 数据）之后，
 * 或者想手动刷新「首页 / 数据大屏」上的数字。
 *
 * 前置条件：6 个后端服务已经启动（bash scripts/start-all.sh），默认走网关 8081。
 * 用法：node tools/refresh-stats.mjs [baseUrl] [startDate] [endDate]
 *   默认 baseUrl=http://127.0.0.1:8081，日期默认「近 90 天」。
 */
const BASE = (process.argv[2] || 'http://127.0.0.1:8081').replace(/\/$/, '')
const today = new Date()
const daysAgo = (n) => {
  const d = new Date(today.getTime() - n * 86400000)
  return d.toISOString().slice(0, 10)
}
const START = process.argv[3] || daysAgo(90)
const END = process.argv[4] || daysAgo(0)

async function api(path, { method = 'GET', body, token } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = 'Bearer ' + token
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON 原样返回 */ }
  return { status: res.status, json, text }
}

const login = await api('/api/auth/app/login', {
  method: 'POST',
  body: { username: process.argv[5] || 'admin', password: process.argv[6] || 'admin123' }
})
if (login.json?.code !== 200) {
  console.error('登录失败：', login.status, login.text.slice(0, 200))
  process.exit(1)
}
const token = login.json.data.token

console.log(`重算统计：${START} ~ ${END}`)
const t0 = Date.now()
const r = await api(`/api/analysis/refresh?startDate=${START}&endDate=${END}`, { method: 'POST', token })
console.log(`HTTP ${r.status}  ${Date.now() - t0}ms`)
console.log(r.json ? JSON.stringify(r.json.data ?? r.json, null, 2) : r.text.slice(0, 500))
process.exit(r.json?.code === 200 ? 0 : 1)
