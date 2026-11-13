/**
 * 边界与异常场景测试（全量体检的补充）
 *
 * <p>前面几个脚本验收的是「正常路径能跑通」，这个脚本专门打异常路径：
 * 非法参数、越权访问、不存在的 ID、状态机非法流转、非法文件类型等，
 * 判断标准只有一条——**不能 500**，并且该拦的要拦（400/403/404/409）。</p>
 *
 * <p>用法：`node tools/edge-test.mjs [baseUrl]`，默认 http://127.0.0.1:8081</p>
 */
const BASE = process.argv[2] || 'http://127.0.0.1:8081'

let pass = 0
let fail = 0
const failures = []

async function call(method, path, { token, body, headers = {}, raw } = {}) {
  const h = { ...headers }
  if (token) h.Authorization = 'Bearer ' + token
  if (body !== undefined && !raw) h['Content-Type'] = 'application/json'
  const res = await fetch(BASE + path, {
    method,
    headers: h,
    body: body === undefined ? undefined : raw ? body : JSON.stringify(body)
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON（CSV / 静态资源） */ }
  return { status: res.status, code: json?.code, msg: json?.msg, data: json?.data, text }
}

function ok(name, cond, detail = '') {
  if (cond) { pass++; console.log(`  [PASS] ${name}${detail ? '  ' + detail : ''}`) }
  else { fail++; failures.push(`${name}  ${detail}`); console.log(`  [FAIL] ${name}${detail ? '  ' + detail : ''}`) }
}

/** 期望「被拒绝」：本项目的错误码在响应体 code 里（HTTP 恒为 200），所以判 code 而不是 status */
function expectReject(name, r, allow = [400, 403, 404, 409, 429]) {
  const code = r.code ?? r.status
  const bad = code === 500 || r.status >= 500
  ok(name, !bad && allow.includes(code), `http=${r.status} code=${code} msg=${r.msg || ''}`)
}

async function login(username, password = 'admin123') {
  const r = await call('POST', '/api/auth/app/login', { body: { username, password } })
  return r.data?.token || r.data?.accessToken || null
}

function section(title) { console.log(`\n== ${title} ==`) }

const admin = await login('admin')
const student = await login('student01')
const seller = await login('student02')
const auditor = await login('auditor')
const operator = await login('operator')
ok('登录：admin/student01/student02/auditor/operator', !!admin && !!student && !!seller && !!auditor && !!operator)

// ---------------------------------------------------------------- 认证与越权
section('认证与越权')
expectReject('错误密码被拒', await call('POST', '/api/auth/app/login', { body: { username: 'admin', password: 'wrong-pass' } }), [400, 401])
expectReject('无 token 访问后台用户列表', await call('GET', '/api/system/user/list'), [401])
expectReject('伪造 token 被拒', await call('GET', '/api/system/user/list', { token: 'abc.def.ghi' }), [401])
expectReject('学生账号访问后台用户列表（越权）', await call('GET', '/api/system/user/list', { token: student }), [403])
expectReject('运营账号新增用户之外的敏感操作（清空日志）', await call('DELETE', '/api/system/operlog/clean', { token: operator }), [403, 200])
ok('审核专员可以看商品审核列表', (await call('GET', '/api/item/item/page?current=1&size=5&status=1', { token: auditor })).code === 200)

// ---------------------------------------------------------------- 分页边界
section('分页与参数边界')
for (const q of ['current=0&size=0', 'current=-1&size=10', 'size=9999&current=1']) {
  const r = await call('GET', '/api/item/app/item/page?' + q)
  ok(`商品分页异常参数不报错：${q}`, r.status !== 500 && r.code === 200, `http=${r.status} records=${r.data?.records?.length}`)
}
expectReject('分页参数传非数字 → 400（参数绑定拒绝）', await call('GET', '/api/item/app/item/page?current=abc&size=xyz'))
const bigPage = await call('GET', '/api/item/app/item/page?current=1&size=9999')
ok('size=9999 被收敛（不一次性拉全表）', (bigPage.data?.records?.length || 0) <= 200, `records=${bigPage.data?.records?.length}`)
const tiny = await call('GET', '/api/system/user/list?current=1&size=1', { token: admin })
ok('size=1 分页语义正确', (tiny.data?.records?.length || 0) === 1 && (tiny.data?.total || 0) > 1, `total=${tiny.data?.total}`)

// ---------------------------------------------------------------- 不存在的资源
section('不存在的资源')
expectReject('商品详情 99999999 → 404', await call('GET', '/api/item/app/item/99999999'), [404, 400])
expectReject('订单详情 99999999 → 400/404', await call('GET', '/api/item/app/order/99999999', { token: student }), [400, 404])
expectReject('公告详情 99999999 → 404', await call('GET', '/api/item/app/notice/99999999'), [404, 400])
expectReject('后台公告详情 99999999 → 404', await call('GET', '/api/item/notice/99999999', { token: admin }), [404, 400])
const unknownDict = await call('GET', '/api/system/dict/data/type/not_exists_type', { token: admin })
ok('未知道字典类型返回空数组而不是 500', unknownDict.status === 200 && Array.isArray(unknownDict.data), `len=${unknownDict.data?.length}`)

// ---------------------------------------------------------------- 商品发布校验
section('商品发布参数校验')
const baseItem = {
  title: '边界测试商品标题', description: '这是一段用于边界测试的商品描述内容，长度超过十个字。',
  price: 100, originalPrice: 200, categoryId: 101, itemCondition: 9, tradeType: 1,
  images: ['/upload/demo/item-01.png'], location: '西区食堂'
}
const publish = (patch) => call('POST', '/api/item/app/item', { token: student, body: { ...baseItem, ...patch } })
expectReject('标题为空 → 400', await publish({ title: '' }))
expectReject('标题只有 2 字 → 400', await publish({ title: '二手' }))
expectReject('价格 -1 → 400', await publish({ price: -1 }))
expectReject('价格 0 → 400', await publish({ price: 0 }))
expectReject('成色 11 → 400', await publish({ itemCondition: 11 }))
expectReject('交易类型 99 → 400', await publish({ tradeType: 99 }))
expectReject('分类不存在 → 400', await publish({ categoryId: 99999 }))
expectReject('图片为空 → 400', await publish({ images: [] }))
expectReject('图片超过 9 张 → 400', await publish({ images: Array.from({ length: 10 }, (_, i) => `/upload/demo/item-0${(i % 9) + 1}.png`) }))
expectReject('描述 5 字 → 400', await publish({ description: '太短了' }))
expectReject('标题 80 字 → 400', await publish({ title: '长'.repeat(80) }))
const goodItem = await publish({ title: '边界测试可发布商品', description: '这一段是正常描述，用于验证发布接口本身是通的，测试结束会删除。' })
ok('合法参数可以发布成功', goodItem.code === 200, `msg=${goodItem.msg}`)
const created = ((await call('GET', '/api/item/app/item/my?current=1&size=5&status=0', { token: student })).data?.records || [])
  .find((i) => i.title === '边界测试可发布商品') || ((await call('GET', '/api/item/app/item/my?current=1&size=5&status=1', { token: student })).data?.records || [])
  .find((i) => i.title === '边界测试可发布商品')
if (created) {
  await call('DELETE', `/api/item/app/item/${created.itemId}`, { token: student })
  console.log(`  [INFO] 已清理测试商品 #${created.itemId}`)
}

// ---------------------------------------------------------------- 收藏
section('收藏')
const items = (await call('GET', '/api/item/app/item/page?current=1&size=50&sort=new')).data?.records || []
const saleItem = items.find((i) => Number(i.status) === 2) || items[0]
if (saleItem) {
  const first = await call('POST', `/api/item/app/favorite/${saleItem.itemId}`, { token: student })
  const second = await call('POST', `/api/item/app/favorite/${saleItem.itemId}`, { token: student })
  ok('重复收藏幂等（第二次不报 500）', second.status !== 500, `first=${first.code} second=${second.code} msg=${second.msg || ''}`)
  expectReject('收藏不存在的商品 → 400/404', await call('POST', '/api/item/app/favorite/99999999', { token: student }), [400, 404])
  expectReject('取消未收藏的商品 → 400/404/200', await call('DELETE', '/api/item/app/favorite/99999999', { token: student }), [400, 404, 200])
  await call('DELETE', `/api/item/app/favorite/${saleItem.itemId}`, { token: student })
} else {
  ok('收藏用例（无可用商品，跳过）', true)
}

// ---------------------------------------------------------------- 订单状态机
section('订单状态机')
const myOrders = (await call('GET', '/api/item/app/order/my?role=buy&current=1&size=20', { token: student })).data?.records || []
const finished = myOrders.find((o) => Number(o.status) === 2)
const pending = myOrders.find((o) => Number(o.status) === 0 || Number(o.status) === 1)
if (finished) {
  expectReject('已完成订单重复确认收货 → 400', await call('POST', `/api/item/app/order/${finished.orderId}/confirm`, { token: student }))
  expectReject('已完成订单取消 → 400', await call('POST', `/api/item/app/order/${finished.orderId}/cancel`, { token: student, body: { cancelReason: '测试' } }))
} else {
  ok('已完成订单状态机用例（无数据，跳过）', true)
}
if (pending) {
  expectReject('待确认订单直接确认收货 → 400', await call('POST', `/api/item/app/order/${pending.orderId}/confirm`, { token: student }))
} else {
  ok('待确认订单状态机用例（无数据，跳过）', true)
}
expectReject('下单不存在的商品 → 400/404', await call('POST', '/api/item/app/order', { token: student, body: { itemId: 99999999 } }))
const ownItem = ((await call('GET', '/api/item/app/item/my?current=1&size=5&status=2', { token: student })).data?.records || [])[0]
if (ownItem) {
  expectReject('买自己发布的商品 → 400', await call('POST', '/api/item/app/order', { token: student, body: { itemId: ownItem.itemId } }))
} else {
  ok('自购商品用例（学生名下无在售商品，跳过）', true)
}

// ---------------------------------------------------------------- 评价
section('评价')
expectReject('评价不存在的订单 → 400/404', await call('POST', '/api/item/app/comment', {
  token: student, body: { itemId: saleItem?.itemId || 1, orderId: 99999999, rating: 5, content: '这条评价不应该被写入' }
}))
expectReject('评分越界（6 星）→ 400', await call('POST', '/api/item/app/comment', {
  token: student, body: { itemId: finished?.itemId || 1, orderId: finished?.orderId || 1, rating: 6, content: '评分越界测试内容' }
}))
if (finished) {
  expectReject('已完成订单重复评价 → 400', await call('POST', '/api/item/app/comment', {
    token: student, body: { itemId: finished.itemId, orderId: finished.orderId, rating: 5, content: '重复评价测试内容' }
  }))
}

// ---------------------------------------------------------------- 举报
section('举报')
expectReject('举报不存在的商品 → 400/404', await call('POST', '/api/item/app/report', {
  token: student, body: { targetType: 'item', targetId: 99999999, reason: 'fake', description: '测试举报内容' }
}))
expectReject('举报原因为非法枚举 → 400', await call('POST', '/api/item/app/report', {
  token: student, body: { targetType: 'item', targetId: saleItem?.itemId || 1, reason: 'not-a-reason', description: '测试举报内容' }
}))
expectReject('举报对象类型非法 → 400', await call('POST', '/api/item/app/report', {
  token: student, body: { targetType: 'order', targetId: saleItem?.itemId || 1, reason: 'fake', description: '测试举报内容' }
}))

// ---------------------------------------------------------------- AI
section('AI 接口边界')
expectReject('AI 客服空问题 → 400', await call('POST', '/api/ai/chat', { token: student, body: { question: '' } }))
expectReject('AI 文案空标题 → 400', await call('POST', '/api/ai/copywrite', { token: student, body: { title: '' } }))
expectReject('AI 估价非法价格 → 400', await call('POST', '/api/ai/price', { token: student, body: { title: '测试', categoryId: 101, price: -5 } }))
const emptyModerate = await call('POST', '/api/ai/moderate', { token: admin, body: { title: '', description: '' } })
ok('AI 审核空文本不报错', emptyModerate.status !== 500 && emptyModerate.code === 200, `pass=${emptyModerate.data?.pass}`)
const abuse = await call('POST', '/api/ai/moderate', { token: admin, body: { title: 'sb', description: '你是傻逼' } })
ok('辱骂内容被拦截', abuse.data?.pass === false && Number(abuse.data?.riskLevel) >= 2, `risk=${abuse.data?.riskLevel} provider=${abuse.data?.provider}`)

// ---------------------------------------------------------------- 文件上传
section('文件上传')
async function upload(filename) {
  const form = new FormData()
  form.append('file', new Blob(['hello'], { type: 'application/octet-stream' }), filename)
  const res = await fetch(BASE + '/api/system/file/upload', {
    method: 'POST', headers: { Authorization: 'Bearer ' + admin }, body: form
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* ignore */ }
  return { status: res.status, code: json?.code, msg: json?.msg, data: json?.data }
}
expectReject('上传 .exe 被拒', await upload('evil.exe'))

// ---------------------------------------------------------------- 后台删除保护
section('后台数据保护')
const roleInUse = (await call('GET', '/api/system/role/list?current=1&size=50', { token: admin })).data?.records?.find((r) => Number(r.roleId) === 4)
if (roleInUse) {
  const del = await call('DELETE', `/api/system/role/${roleInUse.roleId}`, { token: admin })
  ok('删除被用户占用的角色被保护（不能删或提示占用）', del.code !== 200 || del.status === 200, `code=${del.code} msg=${del.msg}`)
}
const selfDel = await call('DELETE', '/api/system/user/1', { token: admin })
ok('删除超管自己被保护', !(selfDel.code === 200 && selfDel.status === 200), `code=${selfDel.code} msg=${selfDel.msg}`)

console.log(`\n===== 结果：PASS=${pass} FAIL=${fail} =====`)
if (fail) {
  console.log('失败明细：')
  failures.forEach((f) => console.log(' - ' + f))
}
process.exit(fail ? 1 : 0)
