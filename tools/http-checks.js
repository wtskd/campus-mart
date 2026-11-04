// CampusMart 接口冒烟测试（node 直连真实服务，不用 curl）
// 用法：node tools/http-checks.js [baseUrl]
// baseUrl 默认 http://127.0.0.1:8081（网关）
const base = (process.argv[2] || 'http://127.0.0.1:8081').replace(/\/$/, '');

let pass = 0, fail = 0;
const fails = [];

async function req(method, path, { body, token, timeout = 20000 } = {}) {
  const t0 = Date.now();
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = 'Bearer ' + token;
  try {
    const res = await fetch(base + path, {
      method, headers,
      body: body ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(timeout)
    });
    const text = await res.text();
    let json = null;
    try { json = JSON.parse(text); } catch (e) { /* 非 JSON（如 CSV/静态资源） */ }
    return { status: res.status, json, text, ms: Date.now() - t0, contentType: res.headers.get('content-type') || '' };
  } catch (e) {
    return { status: 0, json: null, text: e.message, ms: Date.now() - t0, error: true };
  }
}

function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  [PASS] ${name}  ${detail || ''}`); }
  else { fail++; fails.push(name + ' :: ' + (detail || '')); console.log(`  [FAIL] ${name}  ${detail || ''}`); }
}

(async () => {
  console.log('===== CampusMart 冒烟测试 @ ' + base + ' =====\n');

  // ---------- 1. 匿名接口 ----------
  console.log('[1] 匿名只读接口');
  let r = await req('GET', '/api/item/app/category/tree');
  check('GET /api/item/app/category/tree', r.status === 200 && r.json && r.json.code === 200,
    `status=${r.status} code=${r.json && r.json.code} ms=${r.ms}`);

  r = await req('GET', '/api/item/app/item/page?current=1&size=3');
  const items = (r.json && r.json.data && r.json.data.records) || [];
  check('GET /api/item/app/item/page', r.status === 200 && r.json && r.json.code === 200 && items.length > 0,
    `status=${r.status} records=${items.length} total=${r.json && r.json.data && r.json.data.total}`);

  const sampleItem = items[0] || null;

  r = await req('GET', '/api/item/app/item/' + (sampleItem ? sampleItem.itemId : '1'));
  check('GET /api/item/app/item/{id}', r.status === 200 && r.json && r.json.code === 200,
    `status=${r.status} title=${r.json && r.json.data && r.json.data.title}`);

  r = await req('GET', '/api/item/app/banner/list');
  check('GET /api/item/app/banner/list', r.status === 200 && r.json && r.json.code === 200,
    `status=${r.status} count=${Array.isArray(r.json && r.json.data) ? r.json.data.length : 'n/a'}`);

  r = await req('GET', '/api/item/app/notice/list');
  check('GET /api/item/app/notice/list', r.status === 200 && r.json && r.json.code === 200,
    `status=${r.status}`);

  // 静态图片
  r = await req('GET', '/upload/demo/item-01.png');
  check('GET /upload/demo/item-01.png', r.status === 200 && /image/.test(r.contentType),
    `status=${r.status} type=${r.contentType}`);

  // ---------- 2. 登录 ----------
  console.log('\n[2] 登录与令牌');
  let r2 = await req('POST', '/api/auth/app/login', { body: { username: 'student01', password: 'admin123' } });
  const stuToken = r2.json && r2.json.data && r2.json.data.token;
  check('POST /api/auth/app/login (student01)', r2.status === 200 && !!stuToken,
    `status=${r2.status} code=${r2.json && r2.json.code} msg=${r2.json && r2.json.msg} ms=${r2.ms}`);

  // 后台登录页面需要图形验证码；自动化自测改走 App 登录通道（同一账号体系，跳过验证码）
  let r3 = await req('POST', '/api/auth/app/login', { body: { username: 'admin', password: 'admin123' } });
  const adminToken = r3.json && r3.json.data && r3.json.data.token;
  check('POST /api/auth/app/login (admin)', r3.status === 200 && !!adminToken,
    `status=${r3.status} code=${r3.json && r3.json.code} msg=${r3.json && r3.json.msg} ms=${r3.ms}`);

  const token = adminToken || stuToken;
  if (!token) {
    console.log('\n!! 无法获取令牌，后续接口测试跳过');
    console.log(`\n结果：PASS=${pass} FAIL=${fail}`);
    process.exit(1);
  }

  // ---------- 3. 登录态接口 ----------
  console.log('\n[3] 登录态接口（用 admin 令牌）');
  r = await req('GET', '/api/auth/me', { token });
  check('GET /api/auth/me', r.status === 200 && r.json && r.json.code === 200,
    `nickname=${r.json && r.json.data && r.json.data.nickname} perms=${r.json && r.json.data && (r.json.data.perms || []).length}`);

  r = await req('GET', '/api/auth/menus', { token });
  const menus = (r.json && r.json.data) || [];
  check('GET /api/auth/menus', r.status === 200 && Array.isArray(menus) && menus.length > 0,
    `顶层菜单=${menus.length} 首项=${menus[0] && menus[0].menuName}`);

  r = await req('GET', '/api/system/user/list?current=1&size=5', { token });
  check('GET /api/system/user/list', r.status === 200 && r.json && r.json.code === 200,
    `total=${r.json && r.json.data && r.json.data.total} ms=${r.ms}`);

  r = await req('GET', '/api/system/dict/data/type/order_status', { token });
  check('GET /api/system/dict/data/type/order_status',
    r.status === 200 && r.json && r.json.code === 200 && Array.isArray(r.json.data),
    `items=${Array.isArray(r.json && r.json.data) ? r.json.data.length : 'n/a'}`);

  r = await req('GET', '/api/system/menu/list', { token });
  check('GET /api/system/menu/list', r.status === 200 && r.json && r.json.code === 200 && Array.isArray(r.json.data),
    `menus=${Array.isArray(r.json && r.json.data) ? r.json.data.length : 'n/a'}`);

  r = await req('GET', '/api/system/operlog/list?current=1&size=5', { token });
  check('GET /api/system/operlog/list', r.status === 200 && r.json && r.json.code === 200,
    `total=${r.json && r.json.data && r.json.data.total}`);

  r = await req('GET', '/api/item/item/page?current=1&size=5', { token });
  check('GET /api/item/item/page (后台)', r.status === 200 && r.json && r.json.code === 200,
    `total=${r.json && r.json.data && r.json.data.total}`);

  r = await req('GET', '/api/item/order/page?current=1&size=5', { token });
  check('GET /api/item/order/page (后台)', r.status === 200 && r.json && r.json.code === 200,
    `total=${r.json && r.json.data && r.json.data.total}`);

  r = await req('GET', '/api/item/comment/page?current=1&size=5', { token });
  check('GET /api/item/comment/page', r.status === 200 && r.json && r.json.code === 200,
    `total=${r.json && r.json.data && r.json.data.total}`);

  // ---------- 4. 数据分析 ----------
  console.log('\n[4] 数据分析模块');
  for (const [name, path] of [
    ['overview', '/api/analysis/overview'],
    ['trend?days=7', '/api/analysis/trend?days=7'],
    ['category', '/api/analysis/category'],
    ['funnel?days=30', '/api/analysis/funnel?days=30'],
    ['rfm', '/api/analysis/rfm'],
    ['retention?days=7', '/api/analysis/retention?days=7'],
    ['hot-item?limit=5', '/api/analysis/hot-item?limit=5'],
    ['ai-usage', '/api/analysis/ai-usage'],
    ['screen', '/api/analysis/screen']
  ]) {
    r = await req('GET', path, { token, timeout: 30000 });
    const data = r.json && r.json.data;
    const okShape = r.status === 200 && r.json && r.json.code === 200 && data !== null && data !== undefined;
    check('GET ' + path, okShape, `status=${r.status} ms=${r.ms} ${okShape ? 'keys=' + (Array.isArray(data) ? 'array(' + data.length + ')' : Object.keys(data).slice(0, 6).join(',')) : 'msg=' + (r.json && r.json.msg)}`);
  }

  // ---------- 5. AI 模块 ----------
  console.log('\n[5] AI 模块（真实远程调用）');
  r = await req('POST', '/api/ai/copywrite', {
    token,
    timeout: 45000,
    body: { keywords: '索尼 WH-1000XM4 降噪耳机', category: '数码', condition: 9, originalPrice: 2299, price: 1350, remark: '一年前买的，功能完好，附原装收纳包' }
  });
  const cw = r.json && r.json.data;
  check('POST /api/ai/copywrite', r.status === 200 && r.json && r.json.code === 200 && cw && cw.title,
    `provider=${cw && cw.provider} costMs=${cw && cw.costMs} title=${cw && cw.title}`);

  r = await req('POST', '/api/ai/chat', { token, timeout: 45000, body: { question: '在校园易站怎么发布二手商品？' } });
  const chat = r.json && r.json.data;
  check('POST /api/ai/chat (RAG)', r.status === 200 && r.json && r.json.code === 200 && chat && chat.answer,
    `provider=${chat && chat.provider} sources=${chat && chat.sources ? chat.sources.length : 0} answer=${(chat && chat.answer || '').slice(0, 40)}`);

  r = await req('POST', '/api/ai/moderate', { token, timeout: 45000, body: { title: '出二手教材 高等数学', description: '九成新，笔记很少，价格好商量' } });
  const mod = r.json && r.json.data;
  check('POST /api/ai/moderate', r.status === 200 && r.json && r.json.code === 200 && mod && mod.pass !== undefined,
    `pass=${mod && mod.pass} risk=${mod && mod.riskLevel} provider=${mod && mod.provider}`);

  r = await req('POST', '/api/ai/price', { token, timeout: 45000, body: { itemName: 'iPad Air 4 64G', category: '数码', condition: 8, originalPrice: 4799 } });
  const pr = r.json && r.json.data;
  check('POST /api/ai/price', r.status === 200 && r.json && r.json.code === 200 && pr && pr.suggestPrice != null,
    `suggest=${pr && pr.suggestPrice} range=${pr && pr.minPrice}-${pr && pr.maxPrice} provider=${pr && pr.provider}`);

  r = await req('GET', '/api/ai/recommend?limit=4');
  check('GET /api/ai/recommend (匿名)', r.status === 200 && r.json && r.json.code === 200 && Array.isArray(r.json.data),
    `count=${Array.isArray(r.json && r.json.data) ? r.json.data.length : 'n/a'}`);

  r = await req('GET', '/api/ai/log/page?current=1&size=5', { token });
  check('GET /api/ai/log/page', r.status === 200 && r.json && r.json.code === 200,
    `total=${r.json && r.json.data && r.json.data.total}`);

  r = await req('GET', '/api/ai/stat/overview', { token });
  check('GET /api/ai/stat/overview', r.status === 200 && r.json && r.json.code === 200 && r.json.data,
    `keys=${r.json && r.json.data ? Object.keys(r.json.data).slice(0, 5).join(',') : 'n/a'}`);

  // ---------- 汇总 ----------
  console.log(`\n===== 结果：PASS=${pass}  FAIL=${fail} =====`);
  if (fails.length) { console.log('失败项：'); fails.forEach(f => console.log('  - ' + f)); }
  process.exit(fail === 0 ? 0 : 1);
})();
