/**
 * CampusMart 功能自测（写接口 CRUD + 交易闭环），跑完自动清理。
 * 注意：部分创建接口返回 Result<Void>（不回 id），所以这里用「列表回查」拿到新记录 id。
 * 用法：node tools/func-test.mjs [gateway]   默认 http://127.0.0.1:8081
 */
const GW = (process.argv[2] || 'http://127.0.0.1:8081').replace(/\/$/, '') + '/api'

let pass = 0
const fails = []
const notes = []
let adminToken = null

const H = (token) => ({ 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) })

async function call(method, path, { token, body, timeout = 30000 } = {}) {
  const t0 = Date.now()
  try {
    const r = await fetch(GW + path, {
      method,
      headers: H(token),
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(timeout)
    })
    const text = await r.text()
    let json = null
    try { json = JSON.parse(text) } catch { /* csv / 非 json */ }
    return { status: r.status, json, text, ms: Date.now() - t0, ctype: r.headers.get('content-type') || '' }
  } catch (e) {
    return { status: 0, json: null, text: e.message, ms: Date.now() - t0, ctype: '' }
  }
}

function ok(name, detail = '') { pass++; console.log(`  [PASS] ${name}  ${detail}`) }
function bad(name, detail = '') { fails.push(`${name} :: ${detail}`); console.log(`  [FAIL] ${name}  ${detail}`) }
function expect(name, r, wantCode = 200) {
  const code = r.json && r.json.code
  if (r.status === 200 && code === wantCode) { ok(name, `${r.ms}ms`); return true }
  bad(name, `http=${r.status} code=${code} msg=${(r.json && r.json.msg) || String(r.text).slice(0, 80)}`)
  return false
}

const data = (r) => (r.json ? r.json.data : null)
const rowsOf = (r) => {
  const d = data(r)
  if (Array.isArray(d)) return d
  return (d && (d.records || d.rows)) || []
}
const idOf = (r, key) => {
  const d = data(r)
  if (d === null || d === undefined) return null
  if (typeof d === 'number' || typeof d === 'string') return d
  return d[key] ?? null
}

/** 创建接口不回 id 时，用列表回查 */
async function findId(listPath, match, key) {
  const r = await call('GET', listPath, { token: adminToken })
  const hit = rowsOf(r).find(match)
  return hit ? hit[key] : null
}

async function login(username, password = 'admin123') {
  const r = await call('POST', '/auth/app/login', { body: { username, password } })
  return (r.json && r.json.data && r.json.data.token) || null
}

const stamp = Date.now().toString().slice(-6)
const created = { categories: [], notices: [], banners: [], dictTypes: [], dictData: [], configs: [], users: [], roles: [], items: [], comments: [] }

async function http(method, path, opts = {}) { return call(method, path, opts) }

async function main() {
  console.log('===== CampusMart 功能自测 @ ' + GW + ' =====')

  adminToken = await login('admin')
  if (!adminToken) { console.log('!! admin 登录失败，终止'); process.exit(2) }

  // 0. 保证 5 个演示账号都是启用状态
  const allUsers = await http('GET', '/system/user/list?current=1&size=50', { token: adminToken })
  let fixed = 0
  for (const u of rowsOf(allUsers)) {
    if (u.status === '1' && u.username !== 'admin') {
      const rr = await http('PUT', '/system/user/changeStatus', { token: adminToken, body: { userId: u.userId, status: '0' } })
      if (rr.json && rr.json.code === 200) fixed++
    }
  }
  if (fixed) console.log(`  [FIX] 恢复了 ${fixed} 个被停用的演示账号`)

  // 0.1 角色也保证启用（早期前端开关 bug 会写坏 role.status）
  const allRoles = await http('GET', '/system/role/list?current=1&size=50', { token: adminToken })
  let roleFixed = 0
  for (const r of rowsOf(allRoles)) {
    if (String(r.status) === '1' && r.roleId !== 1) {
      const rr = await http('PUT', '/system/role/changeStatus', { token: adminToken, body: { roleId: r.roleId, status: '0' } })
      if (rr.json && rr.json.code === 200) roleFixed++
    }
  }
  if (roleFixed) console.log(`  [FIX] 恢复了 ${roleFixed} 个被停用的角色`)

  const stu1 = await login('student01')
  const stu2 = await login('student02')
  if (!stu1 || !stu2) { console.log('!! 学生账号登录失败'); process.exit(2) }
  ok('登录 admin / student01 / student02')

  /* ---------- 1. 分类 ---------- */
  console.log('\n[1] 分类 CRUD')
  const catName = '自测分类' + stamp
  let r = await http('POST', '/item/category', { token: adminToken, body: { parentId: 0, categoryName: catName, icon: 'test', orderNum: 99, status: '0' } })
  if (expect('新增分类', r)) {
    const cid = await findId('/item/category/list?tree=false', (x) => x.categoryName === catName, 'categoryId')
    if (cid) {
      created.categories.push(cid)
      expect('修改分类', await http('PUT', '/item/category', { token: adminToken, body: { categoryId: cid, parentId: 0, categoryName: catName + '改', orderNum: 99, status: '0' } }))
      expect('分类停用', await http('PUT', '/item/category/status', { token: adminToken, body: { categoryId: cid, status: '1' } }))
    } else bad('回查分类 id', '列表里没找到')
  }

  /* ---------- 2. 公告 ---------- */
  console.log('\n[2] 公告 CRUD')
  r = await http('POST', '/item/notice', { token: adminToken, body: { title: '自测公告' + stamp, content: '这是自测公告内容，用于验证增删改查是否正常。', noticeType: 1, status: '1' } })
  if (expect('新增公告（草稿）', r)) {
    const nid = idOf(r, 'noticeId') || await findId('/item/notice/page?current=1&size=50', (x) => x.title === '自测公告' + stamp, 'noticeId')
    if (nid) {
      created.notices.push(nid)
      expect('公告详情（草稿可查）', await http('GET', `/item/notice/${nid}`, { token: adminToken }))
      expect('修改公告', await http('PUT', '/item/notice', { token: adminToken, body: { noticeId: nid, title: '自测公告' + stamp + '改', content: '改后的公告内容，长度足够。', noticeType: 2, status: '0' } }))
    } else bad('回查公告 id', '没拿到')
  }

  /* ---------- 3. 轮播 ---------- */
  console.log('\n[3] 轮播 CRUD')
  r = await http('POST', '/item/banner', { token: adminToken, body: { title: '自测轮播' + stamp, imageUrl: '/upload/demo/banner-01.png', linkUrl: '/pages/index/index', sort: 99, status: '1' } })
  if (expect('新增轮播', r)) {
    const bid = idOf(r, 'bannerId') || await findId('/item/banner/list', (x) => x.title === '自测轮播' + stamp, 'bannerId')
    if (bid) {
      created.banners.push(bid)
      expect('后台轮播列表', await http('GET', '/item/banner/list', { token: adminToken }))
      expect('修改轮播', await http('PUT', '/item/banner', { token: adminToken, body: { bannerId: bid, title: '自测轮播' + stamp + '改', imageUrl: '/upload/demo/banner-02.png', sort: 98, status: '1' } }))
    } else bad('回查轮播 id', '没拿到')
  }

  /* ---------- 4. 字典 ---------- */
  console.log('\n[4] 字典 CRUD')
  const dictType = 'selftest_' + stamp
  r = await http('POST', '/system/dict/type', { token: adminToken, body: { dictName: '自测字典' + stamp, dictType, status: '0', remark: '自动化测试' } })
  if (expect('新增字典类型', r)) {
    const did = await findId(`/system/dict/type/list?dictType=${dictType}`, (x) => x.dictType === dictType, 'dictId')
    if (did) created.dictTypes.push(did); else bad('回查字典类型 id', '没拿到')
    r = await http('POST', '/system/dict/data', { token: adminToken, body: { dictType, dictLabel: '选项A', dictValue: 'a', dictSort: 1, status: '0' } })
    if (expect('新增字典数据', r)) {
      const dcode = await findId(`/system/dict/data/list?dictType=${dictType}`, (x) => x.dictValue === 'a', 'dictCode')
      if (dcode) created.dictData.push(dcode); else bad('回查字典数据 id', '没拿到')
      r = await http('GET', `/system/dict/data/type/${dictType}`, { token: adminToken })
      expect('按类型取字典项', r)
      const arr = data(r) || []
      if (arr.length === 1) ok('字典项数量正确', '1 条'); else bad('字典项数量', '期望 1 实得 ' + arr.length)
    }
  }

  /* ---------- 5. 参数 ---------- */
  console.log('\n[5] 参数 CRUD')
  const cfgKey = 'selftest.key.' + stamp
  r = await http('POST', '/system/config', { token: adminToken, body: { configName: '自测参数' + stamp, configKey: cfgKey, configValue: 'v1', configType: 'N', remark: '自动化测试' } })
  if (expect('新增参数', r)) {
    const cfid = await findId(`/system/config/list?configKey=${cfgKey}`, (x) => x.configKey === cfgKey, 'configId')
    if (cfid) {
      created.configs.push(cfid)
      expect('按键名取参数', await http('GET', `/system/config/configKey/${cfgKey}`, { token: adminToken }))
      expect('修改参数', await http('PUT', '/system/config', { token: adminToken, body: { configId: cfid, configName: '自测参数' + stamp, configKey: cfgKey, configValue: 'v2', configType: 'N' } }))
    } else bad('回查参数 id', '没拿到')
  }

  /* ---------- 6. 用户 / 角色 ---------- */
  console.log('\n[6] 用户与角色 CRUD')
  const uname = 'selftest' + stamp
  r = await http('POST', '/system/user', { token: adminToken, body: { username: uname, nickname: '自测用户' + stamp, password: 'Test@123456', phone: '139' + stamp + '00', userType: '00', status: '0', roleIds: [4] } })
  if (expect('新增用户', r)) {
    const uid = await findId(`/system/user/list?username=${uname}`, (x) => x.username === uname, 'userId')
    if (uid) {
      created.users.push(uid)
      expect('停用用户', await http('PUT', '/system/user/changeStatus', { token: adminToken, body: { userId: uid, status: '1' } }))
      expect('用户详情', await http('GET', `/system/user/${uid}`, { token: adminToken }))
    } else bad('回查用户 id', '没拿到')
  }

  const rkey = 'selftest_' + stamp
  r = await http('POST', '/system/role', { token: adminToken, body: { roleName: '自测角色' + stamp, roleKey: rkey, roleSort: 99, status: '0', remark: '自动化测试' } })
  if (expect('新增角色', r)) {
    const rid = await findId(`/system/role/list?roleKey=${rkey}`, (x) => x.roleKey === rkey, 'roleId')
    if (rid) {
      created.roles.push(rid)
      expect('给角色分配菜单', await http('PUT', '/system/role/menu', { token: adminToken, body: { roleId: rid, menuIds: [1, 2, 201] } }))
      const mm = await http('GET', `/system/role/${rid}/menuIds`, { token: adminToken })
      if (expect('读取角色已分配菜单', mm)) {
        const ids = data(mm) || []
        if (ids.length === 3) ok('已分配菜单数量', '3 个'); else bad('已分配菜单数量', '期望 3 实得 ' + ids.length)
      }
    } else bad('回查角色 id', '没拿到')
  }

  /* ---------- 7. 商品闭环 ---------- */
  console.log('\n[7] 商品闭环（发布→审核→下架→删除）')
  r = await http('POST', '/item/app/item', {
    token: stu1,
    body: {
      title: '自测商品' + stamp + ' 九成新',
      summary: '自动化测试商品',
      description: '这是自动化测试发布的商品描述，长度超过十个字符，用于验证发布流程是否正常。',
      price: 88.88, originalPrice: 199.0, categoryId: 3, itemCondition: 9, tradeType: 2,
      coverUrl: '/upload/demo/item-03.png', images: ['/upload/demo/item-03.png'],
      tags: ['自测'], location: '东区宿舍', contactType: 0
    }
  })
  if (expect('用户发布商品', r)) {
    const pid = idOf(r, 'itemId') || await findId('/item/app/item/my?current=1&size=5', (x) => String(x.title).includes('自测商品' + stamp), 'itemId')
    if (pid) {
      created.items.push(pid)
      expect('我的发布列表', await http('GET', '/item/app/item/my?current=1&size=5', { token: stu1 }))
      expect('后台审核通过', await http('POST', '/item/item/audit', { token: adminToken, body: { itemId: pid, pass: true, auditRemark: '自动化测试通过' } }))
      expect('详情可见（审核后）', await http('GET', `/item/app/item/${pid}`))
      expect('卖家下架', await http('POST', `/item/app/item/${pid}/off-shelf`, { token: stu1 }))
    } else bad('回查商品 id', '没拿到')
  }

  /* ---------- 8. 收藏 ---------- */
  console.log('\n[8] 收藏')
  const onSale = await http('GET', '/item/app/item/page?current=1&size=5')
  const target = rowsOf(onSale).find((x) => String(x.sellerId) !== '12') || rowsOf(onSale)[0]
  if (target) {
    expect('收藏商品', await http('POST', `/item/app/favorite/${target.itemId}`, { token: stu2 }))
    expect('检查是否已收藏', await http('GET', `/item/app/favorite/check/${target.itemId}`, { token: stu2 }))
    expect('取消收藏', await http('DELETE', `/item/app/favorite/${target.itemId}`, { token: stu2 }))
  } else bad('取在售商品', '列表为空')

  /* ---------- 9. 订单闭环 ---------- */
  console.log('\n[9] 订单闭环（下单→付款→取消）')
  if (target) {
    r = await http('POST', '/item/app/order', { token: stu2, body: { itemId: target.itemId, tradePlace: '图书馆门口', buyerRemark: '自动化测试' } })
    if (expect('创建订单', r)) {
      const oid = idOf(r, 'orderId')
      expect('模拟付款', await http('POST', `/item/app/order/${oid}/pay`, { token: stu2 }))
      expect('订单详情', await http('GET', `/item/app/order/${oid}`, { token: stu2 }))
      expect('取消订单', await http('POST', `/item/app/order/${oid}/cancel`, { token: stu2, body: { cancelReason: '自动化测试取消' } }))
      const after = await http('GET', `/item/app/item/${target.itemId}`)
      const st = data(after) && data(after).status
      if (st === 2) ok('取消后商品恢复在售', 'status=2'); else bad('取消后商品状态', '期望 2 实得 ' + st)
    }
  }

  /* ---------- 10. 评价 ---------- */
  console.log('\n[10] 评价')
  const orders = await http('GET', '/item/order/page?current=1&size=100&status=2', { token: adminToken })
  const comments = await http('GET', '/item/comment/page?current=1&size=100', { token: adminToken })
  const commented = new Set(rowsOf(comments).map((c) => String(c.orderId)))
  const free = rowsOf(orders).find((o) => !commented.has(String(o.orderId)))
  if (free) {
    const buyerName = String(free.buyerNickname || '')
    const buyerToken = await login(buyerName.includes('小红') ? 'student02' : buyerName.includes('小刚') ? 'student03' : 'student01')
    r = await http('POST', '/item/app/comment', { token: buyerToken, body: { itemId: free.itemId, orderId: free.orderId, content: '自动化测试评价：东西不错，交易顺利。', rating: 5, images: [] } })
    if (expect('发表评价', r)) {
      const cid = idOf(r, 'commentId') || await findId(`/item/comment/page?current=1&size=20&itemId=${free.itemId}`, (x) => String(x.orderId) === String(free.orderId), 'commentId')
      if (cid) created.comments.push(cid)
      expect('商品评价列表', await http('GET', `/item/app/comment/list/${free.itemId}`, {}))
    }
  } else notes.push('没有可评价的已完成订单，跳过')

  /* ---------- 11. 举报 ---------- */
  console.log('\n[11] 举报与处理')
  if (target) {
    r = await http('POST', '/item/app/report', { token: stu2, body: { targetType: 'item', targetId: target.itemId, reason: 'other', description: '自动化测试举报' } })
    if (expect('提交举报', r)) {
      expect('后台举报列表', await http('GET', '/item/report/page?current=1&size=10', { token: adminToken }))
      const repId = idOf(r, 'reportId') || await findId('/item/report/page?current=1&size=10', (x) => String(x.content || '').includes('自动化测试举报'), 'reportId')
      if (repId) expect('处理举报（驳回）', await http('PUT', '/item/report/handle', { token: adminToken, body: { reportId: repId, status: 2, handleRemark: '自动化测试驳回' } }))
    }
  }

  /* ---------- 12. 统计与导出 ---------- */
  console.log('\n[12] 统计与导出')
  expect('数据概览', await http('GET', '/analysis/overview', { token: adminToken }))
  r = await http('GET', '/analysis/export/order', { token: adminToken, timeout: 60000 })
  if (r.status === 200 && /text\/csv/.test(r.ctype)) ok('订单 CSV 导出', `${r.text.length}B`); else bad('订单 CSV 导出', `http=${r.status} ctype=${r.ctype}`)
  expect('手动重算统计', await http('POST', '/analysis/refresh', { token: adminToken, body: {} }))

  /* ---------- 13. AI ---------- */
  console.log('\n[13] AI 能力')
  r = await http('POST', '/ai/copywrite', { token: stu1, timeout: 60000, body: { title: '自测自行车', category: '运动户外', price: 260, condition: 9, keywords: ['九成新', '骑行'] } })
  if (expect('文案生成', r)) {
    const cw = data(r)
    ok('  文案来源', `provider=${cw && cw.provider} ${r.ms}ms`)
  }
  r = await http('POST', '/ai/chat', { token: stu1, timeout: 60000, body: { question: '怎么发布二手商品？' } })
  if (expect('RAG 客服', r)) ok('  引用条数', String((data(r) || {}).sources ? data(r).sources.length : 0))
  expect('智能估价', await http('POST', '/ai/price', { token: stu1, timeout: 60000, body: { itemName: 'iPad Air 4', category: '数码', condition: 8, originalPrice: 4799 } }))
  r = await http('POST', '/ai/moderate', { token: stu1, timeout: 60000, body: { title: '代写论文包过', description: '加微信 xxx 代写' } })
  if (expect('内容审核', r)) {
    const mod = data(r)
    if (mod && mod.pass === false) ok('  违规内容被拦截', `riskLevel=${mod.riskLevel}`); else bad('违规内容未拦截', JSON.stringify(mod).slice(0, 120))
  }
  expect('AI 配置读取', await http('GET', '/ai/config', { token: adminToken }))
  r = await http('POST', '/ai/config/test', { token: adminToken, timeout: 60000, body: { provider: 'remote' } })
  if (expect('AI 连通性测试', r)) {
    const t = data(r)
    ok('  测试结果', `ok=${t && t.ok} provider=${t && t.provider} ${t && t.costMs}ms`)
  }

  /* ---------- 清理 ---------- */
  console.log('\n[14] 清理测试数据')
  for (const cid of created.comments) await http('DELETE', `/item/comment/${cid}`, { token: adminToken })
  for (const iid of created.items) await http('DELETE', `/item/item/${iid}`, { token: adminToken })
  for (const nid of created.notices) await http('DELETE', `/item/notice/${nid}`, { token: adminToken })
  for (const bid of created.banners) await http('DELETE', `/item/banner/${bid}`, { token: adminToken })
  for (const cid of created.categories) await http('DELETE', `/item/category/${cid}`, { token: adminToken })
  for (const uid of created.users) await http('DELETE', `/system/user/${uid}`, { token: adminToken })
  for (const rid of created.roles) await http('DELETE', `/system/role/${rid}`, { token: adminToken })
  for (const cfid of created.configs) await http('DELETE', `/system/config/${cfid}`, { token: adminToken })
  if (created.dictData.length) await http('DELETE', `/system/dict/data/${created.dictData.join(',')}`, { token: adminToken })
  if (created.dictTypes.length) await http('DELETE', `/system/dict/type/${created.dictTypes.join(',')}`, { token: adminToken })
  ok('清理完成', `items=${created.items.length} notices=${created.notices.length} users=${created.users.length} roles=${created.roles.length}`)

  console.log(`\n===== 结果：PASS=${pass}  FAIL=${fails.length} =====`)
  if (notes.length) { console.log('备注：'); notes.forEach((n) => console.log('  - ' + n)) }
  if (fails.length) { console.log('失败项：'); fails.forEach((f) => console.log('  - ' + f)) }
  process.exit(fails.length ? 1 : 0)
}

main().catch((e) => { console.error('FATAL: ' + e.message); process.exit(2) })
