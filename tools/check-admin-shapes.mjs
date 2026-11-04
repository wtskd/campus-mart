/**
 * 管理后台「响应归一化」契约校验（Node 侧，不需要浏览器）
 *
 * 作用：直接复用前端 src/utils/analysisShape.js 的归一化函数，对真实后端响应做断言，
 * 保证页面拿到的结构一定是数组/数字，避免再次出现「图表一直转圈」「表格空白」。
 *
 * 用法：node tools/check-admin-shapes.mjs [baseUrl]
 *   默认 baseUrl = http://127.0.0.1:8081
 */
import {
  normalizeAiUsage,
  normalizeOverview,
  normalizeRetention,
  normalizeRfm,
  normalizeScreen,
  normalizeTrend
} from '../campus-admin-web/src/utils/analysisShape.js'

const BASE = process.argv[2] || 'http://127.0.0.1:8081'

let pass = 0
let fail = 0

function check(name, ok, detail = '') {
  if (ok) {
    pass += 1
    console.log(`  [PASS] ${name}${detail ? ' — ' + detail : ''}`)
  } else {
    fail += 1
    console.log(`  [FAIL] ${name}${detail ? ' — ' + detail : ''}`)
  }
}

async function login() {
  const res = await fetch(`${BASE}/api/auth/app/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  })
  const body = await res.json()
  if (body.code !== 200) throw new Error('登录失败：' + (body.msg || res.status))
  return body.data.token
}

function makeGet(token) {
  return async (path) => {
    const res = await fetch(BASE + path, { headers: { Authorization: 'Bearer ' + token } })
    const body = await res.json()
    if (body.code !== 200) throw new Error(`${path} → code=${body.code} ${body.msg}`)
    return body.data
  }
}

const token = await login()
const get = makeGet(token)

console.log('\n== 首页 / 分析域 ==')
const overview = normalizeOverview(await get('/api/analysis/overview'))
check('overview.todayGmv 是数字', typeof overview.todayGmv === 'number', `todayGmv=${overview.todayGmv}`)
check('overview.orderCount 是数字', typeof overview.orderCount === 'number', `orderCount=${overview.orderCount}`)
check('overview.userCount > 0', overview.userCount > 0, `userCount=${overview.userCount}`)

const trend7 = normalizeTrend(await get('/api/analysis/trend?days=7'))
check('trend 是数组且 7 个点', Array.isArray(trend7) && trend7.length === 7, `len=${trend7.length}`)
check('trend 每点有 statDate', trend7.every((p) => !!p.statDate), trend7.map((p) => p.statDate).join(','))
check('trend.gmv 是数字', trend7.every((p) => typeof p.gmv === 'number'))

const rfm = normalizeRfm(await get('/api/analysis/rfm'))
check('rfm 是数组且非空', Array.isArray(rfm) && rfm.length > 0, `len=${rfm.length}`)
check('rfm.userCount 是数字', rfm.every((r) => typeof r.userCount === 'number'))
check('rfm.segment 非空', rfm.every((r) => !!r.segment), rfm.map((r) => r.segment).join('/'))

const retention = normalizeRetention(await get('/api/analysis/retention?days=7'))
check('retention 是数组且 7 个点', Array.isArray(retention) && retention.length === 7, `len=${retention.length}`)
check(
  'retention.retentionRate 在 0~100（末尾「今天」允许 null，表示次日数据未产生）',
  retention.every(
    (r, i) =>
      r.retentionRate === null ||
      (r.retentionRate >= 0 && r.retentionRate <= 100)
  ),
  retention.map((r) => r.retentionRate).join(',')
)

const aiUsage = normalizeAiUsage(await get('/api/analysis/ai-usage'))
check('aiUsage.byScene 非空', Array.isArray(aiUsage.byScene) && aiUsage.byScene.length > 0, `len=${aiUsage.byScene.length}`)

const screen = normalizeScreen(await get('/api/analysis/screen'))
check('screen.hotItems 非空', Array.isArray(screen.hotItems) && screen.hotItems.length > 0, `len=${screen.hotItems.length}`)
check('screen.recentOrders 非空', Array.isArray(screen.recentOrders) && screen.recentOrders.length > 0, `len=${screen.recentOrders.length}`)
check('screen.trend 7 个点', Array.isArray(screen.trend) && screen.trend.length === 7, `len=${screen.trend.length}`)

console.log('\n== 系统域（学院管理 / 消息通知） ==')
const deptList = await get('/api/system/dept/list')
check('dept/list 是数组', Array.isArray(deptList), `len=${deptList.length}`)
check(
  'dept 节点有 deptName/deptId/status',
  deptList.every((d) => !!d.deptName && d.deptId != null && d.status != null),
  deptList.map((d) => d.deptName).join('/')
)

const page = await get('/api/item/message/page?current=1&size=5')
check('message/page 返回分页对象', !!page && Array.isArray(page.records), `total=${page?.total}`)
check('message 记录含 receiverNickname/msgTypeText', (page.records || []).every((m) => !!m.receiverNickname && !!m.msgTypeText))

console.log(`\nRESULT: ${fail === 0 ? 'PASS' : 'FAIL'}  (pass=${pass}, fail=${fail})\n`)
process.exit(fail === 0 ? 0 : 1)
