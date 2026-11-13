// 生成「像真实在跑」的演示数据 → campus-mart/sql/05-rich-data.sql
// 用法： node tools/gen-rich-seed.mjs（在 campus-mart 目录下执行）
// 说明： 固定随机种子，结果可复现；重复执行会先清空业务表再插入。
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const WS = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const OUT = path.join(WS, 'campus-mart/sql/05-rich-data.sql');

// ---------------- 随机（固定种子，可复现） ----------------
let seed = 20261102;
function rnd() {
  seed |= 0;
  seed = (seed + 0x6d2b79f5) | 0;
  let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
}
const int = (a, b) => a + Math.floor(rnd() * (b - a + 1));
const pick = (arr) => arr[Math.floor(rnd() * arr.length)];
const chance = (p) => rnd() < p;
const shuffle = (arr) => {
  const a = arr.slice();
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(rnd() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
};

// ---------------- 时间 ----------------
const NOW = new Date('2026-11-02T10:30:00+08:00');
const DAY = 86400000;
const fmt = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ` +
  `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`;
const ymd = (d) => fmt(d).slice(0, 10);
const ymdCompact = (d) => ymd(d).replace(/-/g, '');
const addDays = (d, n) => new Date(d.getTime() + n * DAY);
const addHours = (d, n) => new Date(d.getTime() + n * 3600000);
const addMin = (d, n) => new Date(d.getTime() + n * 60000);
const randTime = (from, to) => new Date(from.getTime() + Math.floor(rnd() * (to.getTime() - from.getTime())));

const clampNow = (d) => (d.getTime() > NOW.getTime() ? new Date(NOW.getTime() - int(5, 120) * 60000) : d);
const q = (s) => (s === null || s === undefined ? 'NULL' : `'${String(s).replace(/\\/g, '\\\\').replace(/'/g, "''")}'`);
const money = (n) => Number(n).toFixed(2);

// ---------------- 常量 ----------------
const PWD = '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2'; // admin123
const IMG = Array.from({ length: 12 }, (_, i) => `/upload/demo/item-${String(i + 1).padStart(2, '0')}.png`);
const AVATAR = Array.from({ length: 3 }, (_, i) => `/upload/demo/avatar-${String(i + 1).padStart(2, '0')}.png`);
const LOCATIONS = ['东区1号楼', '东区3号楼', '西区5号楼', '西区7号楼', '南苑2号楼', '南苑6号楼', '北苑4号楼', '图书馆门口', '一食堂门口', '二食堂门口', '西门快递点', '体育馆', '大学生活动中心'];

// ---------------- 新增学生用户 ----------------
const NEW_STUDENTS = [
  ['student04', '陈嘉怡', 101, '2023', '软件工程'], ['student05', '李昊然', 101, '2022', '计算机科学'],
  ['student06', '王思远', 102, '2023', '软件工程'], ['student07', '刘妍希', 102, '2024', '数字媒体'],
  ['student08', '张子豪', 101, '2021', '网络工程'], ['student09', '赵梓涵', 103, '2022', '人工智能'],
  ['student10', '周宇轩', 103, '2023', '数据科学'], ['student11', '吴佳琪', 104, '2023', '电子商务'],
  ['student12', '徐一鸣', 101, '2024', '计算机科学'], ['student13', '孙梦瑶', 102, '2022', '软件工程'],
  ['student14', '马文博', 103, '2024', '智能科学'], ['student15', '朱雨桐', 104, '2023', '市场营销'],
  ['student16', '胡明轩', 101, '2023', '物联网工程'], ['student17', '郭静怡', 103, '2022', '人工智能'],
];

// ---------------- 新增二级分类 ----------------
const SUBCATS = [
  [104, 1, '平板电脑', 4], [105, 1, '耳机音箱', 5],
  [204, 2, '考证资料', 4], [205, 2, '课堂笔记', 5],
  [301, 3, '宿舍电器', 1], [302, 3, '清洁洗护', 2], [303, 3, '厨房餐具', 3], [304, 3, '雨具收纳', 4], [305, 3, '床上用品', 5],
  [401, 4, '球类运动', 1], [402, 4, '健身器材', 2], [403, 4, '骑行装备', 3], [404, 4, '户外露营', 4],
  [501, 5, '护肤保养', 1], [502, 5, '彩妆香氛', 2], [503, 5, '个护小电', 3],
  [604, 6, '床垫床品', 3], [605, 6, '简易衣柜', 4], [606, 6, '台灯照明', 5],
  [701, 7, '吉他尤克里里', 1], [702, 7, '键盘乐器', 2], [703, 7, '桌游棋牌', 3],
  [801, 8, '手办周边', 1], [802, 8, '其他闲置', 2],
];

// ---------------- 商品模板 ----------------
const CAT = {
  101: { n: 22, price: [600, 3600], tags: ['手机', '数码'], titles: ['iPhone 12 128G 国行', 'iPhone 13 256G 星光色', '小米 13 8+256 骁龙版', '小米 14 12+256', '华为 Mate 50 256G', 'Redmi K60 12+512', 'OPPO Reno9 8+256', 'vivo X90 8+256', '一加 Ace2 16+256', '荣耀 Magic5 8+256', 'iPhone 11 64G 备用机', 'realme GT Neo5 12+256'] },
  102: { n: 12, price: [1200, 6800], tags: ['笔记本', '电脑'], titles: ['联想小新 Pro16 2023', 'ThinkPad X1 Carbon 2019', 'MacBook Air M1 8+256', '戴尔灵越 14 Plus', '机械革命蛟龙 16K', '华为 MateBook D14', '惠普星 14 轻薄本', 'ROG 魔霸新锐 2022', '组装台式整机 10400F', '微软 Surface Laptop 4'] },
  103: { n: 14, price: [20, 800], tags: ['配件', '数码'], titles: ['AirPods Pro 2 国行', '罗技 MX Master 3S', 'Keychron K8 机械键盘', 'AOC 27 寸 2K 显示器', '希捷移动硬盘 1T', '倍思 65W 氮化镓充电器', 'USB-C 七合一扩展坞', '索尼 WH-1000XM4', 'iPad 妙控键盘', '雷蛇毒蝰鼠标'] },
  104: { n: 8, price: [800, 3200], tags: ['平板', '数码'], titles: ['iPad 9 64G WLAN', 'iPad Air 4 64G', '华为 MatePad 11', '小米平板 6 Pro', 'iPad mini 5 64G', '荣耀平板 V8'] },
  105: { n: 9, price: [40, 1200], tags: ['耳机', '数码'], titles: ['索尼 WF-1000XM4', '漫步者 W820NB', 'JBL GO3 蓝牙音箱', '华为 FreeBuds 5i', 'Beats Studio3', '铁三角 ATH-M40x'] },

  201: { n: 26, price: [6, 60], tags: ['教材', '书籍'], titles: ['《数据结构》C 语言版 严蔚敏', '《计算机网络》第 8 版 谢希仁', '《操作系统概念》第九版', '《高等数学》同济第七版 上下册', '《线性代数》同济第六版', '《概率论与数理统计》浙大版', '《大学物理》马文蔚 上册', '《Java 核心技术 卷 I》', '《深入理解计算机系统》CSAPP', '《算法导论》第三版'] },
  202: { n: 14, price: [8, 90], tags: ['考研', '书籍'], titles: ['张宇考研数学 1000 题', '考研英语一真题超精读', '肖秀荣 1000 题+背诵手册', '王道 408 四件套', '考研政治徐涛核心考案', '英语二历年真题解析', '考研数学复习全书'] },
  203: { n: 12, price: [5, 45], tags: ['书籍', '读物'], titles: ['《三体》全集 三册', '《活着》余华', '《人类简史》', '《明朝那些事儿》全套', '《围城》钱钟书', '《平凡的世界》三册', '《解忧杂货店》', '《月亮与六便士》'] },
  204: { n: 8, price: [10, 70], tags: ['考证', '书籍'], titles: ['四级真题+词汇书', '六级真题超详解', '计算机二级 MS Office 题库', '教师资格证 综合素质', '初级会计实务教材', '雅思真经 5-18'] },
  205: { n: 6, price: [5, 30], tags: ['笔记', '学习'], titles: ['高数手写笔记 全册', '数据结构考研笔记', '计网课堂笔记整理版', '英语作文模板手写版'] },

  301: { n: 12, price: [15, 260], tags: ['宿舍', '电器'], titles: ['宿舍迷你电煮锅', '小型暖风机', '桌面小风扇', '充电台灯', '电热水壶 0.8L', '挂烫机 手持款', 'mini 冰箱 20L'] },
  302: { n: 8, price: [8, 80], tags: ['生活', '日用'], titles: ['蓝月亮洗衣液套装', '洗发水护发素套装', '纸巾整箱抽纸', '沐浴露 750ml', '除菌湿巾 10 包'] },
  303: { n: 7, price: [5, 60], tags: ['生活', '厨房'], titles: ['陶瓷碗碟四件套', '不锈钢保温饭盒', '便携餐具套装', '马克杯情侣款'] },
  304: { n: 7, price: [5, 60], tags: ['生活', '收纳'], titles: ['全自动折叠雨伞', '宿舍收纳箱大号', '床下收纳袋', '衣架 20 个装'] },
  305: { n: 6, price: [30, 200], tags: ['宿舍', '床品'], titles: ['纯棉四件套 1.5m', '加厚冬被 6 斤', '记忆棉枕头一对', '珊瑚绒毯子'] },

  401: { n: 10, price: [15, 200], tags: ['运动', '球类'], titles: ['斯伯丁篮球 7 号', '尤尼克斯羽毛球拍', '李宁足球 5 号', '乒乓球拍横拍套装', '排球 软式'] },
  402: { n: 8, price: [10, 320], tags: ['运动', '健身'], titles: ['可调节哑铃 20kg', '瑜伽垫加厚 8mm', '弹力带套装', '健腹轮', '跳绳 计数款'] },
  403: { n: 6, price: [100, 1600], tags: ['骑行', '运动'], titles: ['捷安特 ATX 山地车', '美利达折叠自行车', '电动滑板车 通勤款', '骑行头盔+手套'] },
  404: { n: 6, price: [30, 600], tags: ['户外', '露营'], titles: ['双人帐篷 防雨', '户外折叠椅两把', '露营灯 充电款', '登山包 40L'] },

  501: { n: 8, price: [15, 320], tags: ['护肤', '个护'], titles: ['珂润面霜 40g 全新', '雅诗兰黛小棕瓶 50ml', '芙丽芳丝洗面奶', '安耐晒金瓶防晒', '面膜 10 片装'] },
  502: { n: 6, price: [20, 260], tags: ['彩妆', '个护'], titles: ['YSL 方管口红 1966', '兰蔻持妆粉底液', '完美日记眼影盘', '祖玛珑蓝风铃香水 30ml'] },
  503: { n: 6, price: [30, 400], tags: ['个护', '小电'], titles: ['戴森吹风机 平替款', '飞利浦电动牙刷', '直发卷发两用棒', '洁面仪'] },

  601: { n: 8, price: [25, 320], tags: ['宿舍', '家具'], titles: ['宿舍折叠桌 60cm', '床上电脑桌', '人体工学椅 二手', '书桌 1.2m 实木'] },
  602: { n: 7, price: [8, 100], tags: ['宿舍', '收纳'], titles: ['三层收纳推车', '桌面收纳盒套装', '鞋架 四层', '真空压缩袋 5 个'] },
  604: { n: 5, price: [40, 260], tags: ['宿舍', '床品'], titles: ['乳胶床垫 90cm', '床垫保护垫', '学生宿舍床帘'] },
  605: { n: 5, price: [45, 220], tags: ['宿舍', '家具'], titles: ['简易布衣柜', '组装挂衣架', '宿舍储物柜'] },
  606: { n: 5, price: [15, 160], tags: ['宿舍', '台灯'], titles: ['护眼台灯 可调光', '磁吸宿舍灯条', 'USB 小夜灯'] },

  701: { n: 6, price: [100, 1000], tags: ['乐器', '文娱'], titles: ['雅马哈 F310 民谣吉他', '尤克里里 23 寸', '卡林巴拇指琴 17 音', '电子琴 61 键'] },
  702: { n: 4, price: [150, 1400], tags: ['乐器', '文娱'], titles: ['雅马哈 P45 电钢琴', 'MIDI 键盘 25 键', '口琴 十孔'] },
  703: { n: 6, price: [12, 130], tags: ['桌游', '文娱'], titles: ['三国杀 标准版', 'UNO 牌+狼人杀', '大富翁 豪华版', '剧本杀 盒装'] },

  801: { n: 5, price: [20, 420], tags: ['手办', '周边'], titles: ['原神 手办 可莉', '宝可梦 皮卡丘公仔', '高达模型 MG', '动漫立牌套装'] },
  802: { n: 6, price: [10, 220], tags: ['闲置', '其他'], titles: ['雨伞+水杯 打包', '行李箱 24 寸', '背包 通勤款', '充电宝 20000mAh'] },
};

const REASONS = ['换新机了', '毕业清宿舍', '买重复了', '用不上了', '换城市带不走', '双十一冲动消费', '室友合买多了一套', '实习结束回校'];
const DETAILS = ['功能一切正常', '有轻微使用痕迹，不影响使用', '包装盒和发票都在', '无磕碰无进水', '只用了两三个月', '一直放在宿舍没怎么动', '外观 95 新，屏幕无划痕', '原装配件齐全'];
const TRADES = ['西区可自提，也可走平台快递', '图书馆门口当面交易', '支持验货，满意再付款', '价格可小刀，急出', '可约在食堂门口，晚上都在宿舍', '同城可送，外地走快递'];

// ---------------- 生成用户 ----------------
const users = [];
const existingStudents = [11, 12, 13];
NEW_STUDENTS.forEach(([username, nickname, deptId, grade, major], i) => {
  users.push({
    id: 21 + i, username, nickname, deptId, major,
    studentNo: `${grade}${String(3000 + i * 37)}`,
    phone: `13${int(1, 9)}${String(int(10000000, 99999999))}`,
    credit: int(86, 100),
    avatar: AVATAR[i % AVATAR.length],
    createTime: randTime(addDays(NOW, -150), addDays(NOW, -20)),
  });
});
const studentIds = [...existingStudents, ...users.map((u) => u.id)]; // 17 个学生

// ---------------- 生成商品 ----------------
const CAT_KEYS = Object.keys(CAT).map(Number);
const plan = [];
for (const cid of CAT_KEYS) {
  for (let i = 0; i < CAT[cid].n; i++) plan.push(cid);
}
const items = [];
let itemId = 3001;
for (const cid of shuffle(plan)) {
  const c = CAT[cid];
  const title = pick(c.titles);
  const cond = pick([7, 9, 9, 9, 10]);
  const [lo, hi] = c.price;
  const original = Math.round((lo + rnd() * (hi - lo)) / 10) * 10;
  const factor = cond === 10 ? 0.85 : cond === 9 ? 0.6 : 0.42;
  const price = Math.max(3, Math.round((original * factor) / 5) * 5);
  const seller = pick(studentIds);
  const publish = randTime(new Date('2026-09-01T08:00:00+08:00'), new Date('2026-11-01T22:00:00+08:00'));
  const status = pick([2, 2, 2, 2, 4, 4, 4, 4, 4, 1, 5]);
  items.push({
    id: itemId++, cid, rootCid: cid >= 100 && cid < 200 ? 1 : cid >= 200 && cid < 300 ? 2 : cid >= 300 && cid < 400 ? 3 : cid >= 400 && cid < 500 ? 4 : cid >= 500 && cid < 600 ? 5 : cid >= 600 && cid < 700 ? 6 : cid >= 700 && cid < 800 ? 7 : 8,
    title: `${title} ${pick(['9 成新', '95 新', '几乎全新', '自用一年', '带原装配件', '急出'])}`.trim(),
    summary: '', price, original, cond,
    trade: chance(0.82) ? 1 : pick([2, 3]),
    seller, publish, status,
    location: pick(LOCATIONS),
    view: 0, tags: c.tags,
    desc: `${pick(REASONS)}，${pick(DETAILS)}。${pick(TRADES)}。`,
    images: shuffle(IMG).slice(0, int(2, 3)),
    aiGen: chance(0.45) ? 1 : 0,
  });
}
// 状态配额（按比例分配，铺满所有商品）
// 在售 45% / 已售出 32% / 待审核 8% / 已预订 5% / 下架 5% / 违规 3% / 草稿 2%
{
  const ratios = [[2, 0.45], [4, 0.32], [1, 0.08], [3, 0.05], [5, 0.05], [6, 0.03], [0, 0.02]];
  const pool = shuffle(items.map((_, i) => i));
  const counts = ratios.map(([st, r]) => [st, Math.floor(items.length * r)]);
  counts[0][1] += items.length - counts.reduce((s, c) => s + c[1], 0);
  let p = 0;
  for (const [st, cnt] of counts) {
    for (let k = 0; k < cnt && p < pool.length; k++) items[pool[p++]].status = st;
  }
}
// 发布时间按状态回填：已售出的至少要再早 3 天，在售的近两天要有一批（让「今日新增」不为 0）
for (const it of items) {
  if (it.status === 4) it.publish = randTime(new Date('2026-08-20T09:00:00+08:00'), addDays(NOW, -3));
  else if (it.status === 3) it.publish = randTime(addDays(NOW, -10), addDays(NOW, -1));
  else if (chance(0.18)) it.publish = randTime(addHours(NOW, -40), addHours(NOW, -2));
  else it.publish = randTime(new Date('2026-08-20T09:00:00+08:00'), addHours(NOW, -2));
}

// ---------------- 生成订单 ----------------
const orders = [];
let orderId = 4001;
const buyersPool = studentIds;
function makeOrder(item, status) {
  const buyer = pick(buyersPool.filter((u) => u !== item.seller));
  const latest = new Date(Math.min(addDays(item.publish, 7).getTime(), addHours(NOW, -2).getTime()));
  const createTime = randTime(addHours(item.publish, 1), latest);
  const o = {
    id: orderId++, no: null, itemId: item.id, buyer, seller: item.seller,
    amount: item.price, status,
    payType: chance(0.15) ? 2 : 1, tradePlace: pick(LOCATIONS),
    createTime, payTime: null, finishTime: null, cancelTime: null, cancelReason: null,
    remark: chance(0.3) ? pick(['麻烦尽快发货，谢谢', '今天下午有空交易吗', '可以便宜一点点吗', '希望包装仔细一点']) : null,
  };
  o.no = `CM${ymdCompact(createTime)}${o.id}`;
  const clamp = (d) => (d.getTime() > NOW.getTime() ? new Date(NOW.getTime() - int(20, 90) * 60000) : d);
  if (status === 0) return o;
  o.payTime = addMin(createTime, int(3, 120));
  if (o.payTime > NOW) o.payTime = clamp(o.payTime);
  if (o.payTime < o.createTime) o.payTime = addMin(o.createTime, 5);
  if (status === 1) return o;
  if (status === 3) { o.cancelTime = clamp(addHours(createTime, int(1, 30))); o.cancelReason = pick(['联系不上卖家', '临时不需要了', '想换个成色更好的', '卖家说已经卖给别人了']); o.payTime = null; return o; }
  o.finishTime = clamp(addHours(o.payTime, int(12, 72)));
  if (o.finishTime <= o.payTime) o.finishTime = addHours(o.payTime, 2);
  if (status === 5) o.refundTime = clamp(addDays(o.finishTime, int(1, 4)));
  return o;
}

const sold = items.filter((i) => i.status === 4);
const reserved = items.filter((i) => i.status === 3);
const onSale = items.filter((i) => i.status === 2);
for (const it of sold) {
  const o = makeOrder(it, 2);
  orders.push(o);
  it.view = int(180, 900);
}
for (const it of reserved) {
  const o = makeOrder(it, chance(0.5) ? 0 : 1);
  orders.push(o);
  it.view = int(60, 320);
}
// 在售商品上：已取消 12 单、已退款 4 单
for (const it of shuffle(onSale).slice(0, 16)) {
  const canceled = orders.filter((o) => o.itemId === it.id).length === 0;
  if (!canceled) continue;
  const st = chance(0.75) ? 3 : 5;
  orders.push(makeOrder(it, st));
  it.view = int(30, 420);
}
for (const it of onSale) if (!it.view) it.view = int(15, 380);
// 让「今日」也有成交，避免大屏今日 GMV 为 0
const todays = orders.filter((o) => o.status === 2).slice(0, 3);
todays.forEach((o, idx) => {
  const it = items.find((i) => i.id === o.itemId);
  it.publish = addHours(NOW, -(32 + idx * 6));
  o.createTime = addHours(NOW, -(9 + idx * 2));
  o.payTime = addMin(o.createTime, 12 + idx * 5);
  o.finishTime = addHours(NOW, -(2 + idx));
  o.no = 'CM' + ymdCompact(o.createTime) + o.id;
});
for (const it of items) if (!it.view) it.view = int(8, 260);

// ---------------- 评论（仅已完成订单） ----------------
const FINISHED = orders.filter((o) => o.status === 2);
const COMMENT_TEXT = ['东西和描述一致，卖家人很好说话，交易顺利！', '成色比想象中好，性价比很高，推荐。', '包装很仔细，还送了个小礼物，感谢学长。', '约在图书馆门口，见面交易很放心。', '价格公道，东西没问题，下次还来。', '有一点小瑕疵，不过卖家提前说了，可以接受。', '发货很快，当天就拿到了。', '室友都说买得值，宿舍神器。'];
const comments = [];
let commentId = 5001;
for (const o of FINISHED) {
  if (!chance(0.85)) continue;
  const score = chance(0.85) ? 5 : 4;
  comments.push({
    id: commentId++, orderId: o.id, itemId: o.itemId, from: o.buyer, to: o.seller, score,
    content: pick(COMMENT_TEXT),
    reply: chance(0.35) ? pick(['谢谢支持，有问题随时找我～', '交易愉快，欢迎下次再来。', '感谢信任，祝学习顺利！']) : null,
    time: clampNow(addHours(o.finishTime, int(1, 48))),
  });
}

// ---------------- 收藏 ----------------
const favorites = [];
let favId = 6001;
const favable = items.filter((i) => i.status === 2 || i.status === 3 || i.status === 4);
const favPairs = new Set();
for (let k = 0; k < 520; k++) {
  const it = pick(favable);
  const u = pick(studentIds);
  const key = `${u}-${it.id}`;
  if (u === it.seller || favPairs.has(key)) continue;
  favPairs.add(key);
  const favFrom = new Date(Math.min(addDays(it.publish, 0.1).getTime(), NOW.getTime() - 3600000));
  favorites.push({ id: favId++, user: u, item: it.id, time: randTime(favFrom, NOW) });
}

// ---------------- 举报 ----------------
const REASONS_R = [['illegal', '疑似售卖违禁物品', '商品描述里出现了违规物品，请核实'], ['fake', '商品信息与实物严重不符', '图片是网图，卖家说没货了还挂着'], ['price', '价格明显异常，疑似诈骗', '要求先转账再发货，不走平台'], ['harass', '卖家言语骚扰', '沟通中多次被辱骂'], ['other', '重复发布同一商品', '同一个商品挂了三次']];
const reports = [];
let reportId = 7001;
const reportTargets = shuffle(items.filter((i) => [2, 1, 6].includes(i.status))).slice(0, 14);
reportTargets.forEach((it, idx) => {
  const [rt, title, content] = pick(REASONS_R);
  const handled = it.status === 6 ? 1 : idx < 5 ? (chance(0.5) ? 2 : 1) : 0;
  reports.push({
    id: reportId++, item: it.id, reporter: pick(studentIds.filter((u) => u !== it.seller)), reason: rt,
    content: `${title}。${content}。`, status: handled,
    handleUser: handled ? pick([2, 3]) : null,
    handleRemark: handled === 1 ? '举报成立，商品已下架处理' : handled === 2 ? '经核实内容正常，举报不成立' : null,
    handleTime: handled ? addHours(it.publish, int(6, 60)) : null,
    time: clampNow(addHours(it.publish, int(2, 72))),
  });
});

// ---------------- 消息 ----------------
const messages = [];
let msgId = 8001;
const MSG_T = [
  [2, (it) => `订单已完成：${it.title.slice(0, 18)}`, '买家已确认收货，货款已结算。记得给卖家一个评价哦～'],
  [2, (it) => `您有一笔新订单：${it.title.slice(0, 18)}`, '买家已下单，请尽快确认并约定交付时间。'],
  [3, (it) => `商品审核通过：${it.title.slice(0, 18)}`, '您的商品已通过审核并上架，快去看看吧。'],
  [3, (it) => `商品审核未通过：${it.title.slice(0, 18)}`, '未通过原因：图片不清晰/描述含违规词，请修改后重新提交。'],
  [1, () => '校园易站交易安全提醒', '请勿脱离平台私下转账，见面交易请选择白天、人多的地方。'],
  [4, (it) => `AI 已为「${it.title.slice(0, 14)}」生成文案`, '一键发布文案已生成，可在发布页直接使用。'],
];
for (const it of shuffle(items).slice(0, 34)) {
  const [type, titleFn, content] = pick(MSG_T);
  messages.push({ id: msgId++, user: pick([...studentIds, 2]), type, title: titleFn(it), content, bizId: it.id, read: chance(0.55) ? 1 : 0, time: clampNow(addHours(it.publish, int(1, 120))) });
}

// ---------------- AI 调用日志 ----------------
const aiLogs = [];
let aiId = 9001;
const AISCENES = [['copywrite', 'ITEM_COPYWRITE', 0.38], ['chat', 'SERVICE_CHAT', 0.24], ['price', 'ITEM_PRICE', 0.16], ['audit', 'ITEM_AUDIT', 0.16], ['recommend', 'RECOMMEND', 0.06]];
for (let k = 0; k < 580; k++) {
  let r = rnd(), acc = 0, scene = AISCENES[0];
  for (const s of AISCENES) { acc += s[2]; if (r <= acc) { scene = s; break; } }
  const roll = rnd();
  const provider = roll < 0.72 ? 'remote' : roll < 0.88 ? 'local' : 'rule';
  const status = rolloutStatus();
  const pTok = int(120, 900), cTok = int(40, 480);
  aiLogs.push({
    id: aiId++, user: pick(studentIds), scene: scene[0], tpl: scene[1],
    provider, model: provider === 'remote' ? 'deepseek-chat' : provider === 'local' ? 'qwen2.5:7b-instruct' : 'template',
    summary: pick(['一键发布文案生成', '智能客服问答', '二手估价建议', '商品内容审核', '相似商品推荐']) + ` / item ${pick(items).id}`,
    pTok, cTok, total: pTok + cTok,
    cost: provider === 'rule' ? int(2, 20) : provider === 'local' ? int(400, 2600) : int(700, 4200),
    status, err: status === 1 ? pick(['connect timeout', 'HTTP 429 rate limit', 'invalid json response']) : null,
    time: randTime(new Date('2026-09-25T00:00:00+08:00'), NOW),
  });
}
function rolloutStatus() { const r = rnd(); return r < 0.88 ? 0 : r < 0.98 ? 2 : 1; }

// ---------------- 输出 SQL ----------------
const L = [];
L.push('-- ============================================================');
L.push('-- 05-rich-data.sql  「像真实在跑」的演示数据（生成器 tools/gen-rich-seed.mjs）');
L.push(`-- 生成时间：${fmt(NOW)}    商品 ${items.length} / 订单 ${orders.length} / 评论 ${comments.length} / 收藏 ${favorites.length}`);
L.push('-- 重复执行安全：会先清理业务表与统计表，再插入。');
L.push('-- ============================================================');
L.push('SET NAMES utf8mb4;');
L.push('');
L.push('-- ---------- 0. 清理自测残留 + 业务表 ----------');
L.push("DELETE FROM sys_user_role WHERE user_id IN (SELECT user_id FROM sys_user WHERE username LIKE 'selftest%');");
L.push("DELETE FROM sys_user WHERE username LIKE 'selftest%';");
L.push("DELETE FROM sys_role WHERE role_key LIKE 'selftest%';");
L.push("DELETE FROM sys_dict_data WHERE dict_type LIKE 'selftest%';");
L.push("DELETE FROM sys_dict_type WHERE dict_type LIKE 'selftest%';");
L.push("DELETE FROM biz_category WHERE category_name LIKE '自测%';");
L.push("-- 自测可能把演示账号停用过，这里恢复");
L.push("UPDATE sys_user SET status = '0' WHERE username IN ('admin','operator','auditor','student01','student02','student03');");
L.push("-- 角色也一并恢复为启用（早期前端开关 bug 会把「运营管理员/审核专员/普通用户」写成停用）");
L.push("UPDATE sys_role SET status = '0' WHERE role_id IN (1,2,3,4);");
for (const t of ['biz_comment', 'biz_favorite', 'biz_report', 'biz_message', 'biz_order', 'biz_item', 'ai_call_log', 'ai_recommend', 'stat_overview_daily', 'stat_category_daily', 'stat_funnel_daily', 'stat_user_rfm']) L.push(`DELETE FROM ${t};`);
L.push('');

L.push('-- ---------- 1. 学生用户（student04 ~ student17，密码均为 admin123） ----------');
L.push('DELETE FROM sys_user_role WHERE user_id BETWEEN 21 AND 34;');
L.push('DELETE FROM sys_user WHERE user_id BETWEEN 21 AND 34;');
L.push('INSERT INTO sys_user (user_id, dept_id, username, nickname, user_type, password, phone, email, avatar, gender, student_no, credit_score, status, remark, del_flag, create_time, update_time) VALUES');
L.push(users.map((u) => `(${u.id}, ${u.deptId}, ${q(u.username)}, ${q(u.nickname)}, '01', ${q(PWD)}, ${q(u.phone)}, ${q(u.username + '@campus.edu.cn')}, ${q(u.avatar)}, ${int(1, 2)}, ${q(u.studentNo)}, ${u.credit}, 0, ${q(u.major + ' 在读')}, '0', ${q(fmt(u.createTime))}, ${q(fmt(u.createTime))})`).join(',\n') + ';');
L.push('');
L.push('INSERT INTO sys_user_role (user_id, role_id) VALUES');
L.push(users.map((u) => `(${u.id}, 4)`).join(',\n') + ';');
L.push('');

L.push('-- ---------- 2. 二级分类（补齐分类树） ----------');
L.push('DELETE FROM biz_category WHERE category_id IN (' + SUBCATS.map((c) => c[0]).join(', ') + ');');
L.push('INSERT INTO biz_category (category_id, parent_id, category_name, icon, order_num, status, del_flag, create_time) VALUES');
L.push(SUBCATS.map(([id, pid, name, ord]) => `(${id}, ${pid}, ${q(name)}, NULL, ${ord}, 0, '0', ${q(fmt(NOW))})`).join(',\n') + ';');
L.push('');

L.push('-- ---------- 3. 商品 ----------');
L.push('INSERT INTO biz_item (item_id, item_no, seller_id, category_id, title, summary, description, price, original_price, item_condition, trade_type, cover_url, images, tags, location, contact_type, ai_generated, view_count, favorite_count, status, audit_remark, publish_time, del_flag, create_time, update_time) VALUES');
L.push(items.map((it) => {
  const imgs = JSON.stringify(it.images);
  const tags = JSON.stringify([...it.tags, pick(['自提', '可小刀', '急出', '校园'])]);
  const remark = it.status === 6 ? q('含违规内容，已下架') : it.status === 5 ? q('用户自行下架') : it.status === 1 ? 'NULL' : 'NULL';
  const createT = addHours(it.publish, -int(1, 8));
  return `(${it.id}, ${q('CM' + ymdCompact(it.publish) + it.id)}, ${it.seller}, ${it.cid}, ${q(it.title)}, ${q(it.title)}, ${q(it.desc)}, ${money(it.price)}, ${money(it.original)}, ${it.cond}, ${it.trade}, ${q(it.images[0])}, ${q(imgs)}, ${q(tags)}, ${q(it.location)}, 0, ${it.aiGen}, ${it.view}, 0, ${it.status}, ${remark}, ${q(fmt(it.publish))}, '0', ${q(fmt(createT))}, ${q(fmt(it.publish))})`;
}).join(',\n') + ';');
L.push('');

L.push('-- ---------- 4. 订单 ----------');
L.push('INSERT INTO biz_order (order_id, order_no, item_id, buyer_id, seller_id, amount, status, pay_type, trade_place, buyer_remark, cancel_reason, create_time, pay_time, finish_time, cancel_time, update_time) VALUES');
L.push(orders.map((o) => `(${o.id}, ${q(o.no)}, ${o.itemId}, ${o.buyer}, ${o.seller}, ${money(o.amount)}, ${o.status}, ${o.payType}, ${q(o.tradePlace)}, ${q(o.remark)}, ${q(o.cancelReason)}, ${q(fmt(o.createTime))}, ${o.payTime ? q(fmt(o.payTime)) : 'NULL'}, ${o.finishTime ? q(fmt(o.finishTime)) : 'NULL'}, ${o.cancelTime ? q(fmt(o.cancelTime)) : 'NULL'}, ${q(fmt(o.finishTime || o.payTime || o.createTime))})`).join(',\n') + ';');
L.push('');

L.push('-- ---------- 5. 评价 ----------');
L.push('INSERT INTO biz_comment (comment_id, order_id, item_id, from_user_id, to_user_id, score, content, images, reply_content, status, create_time) VALUES');
L.push(comments.map((c) => `(${c.id}, ${c.orderId}, ${c.itemId}, ${c.from}, ${c.to}, ${c.score}, ${q(c.content)}, NULL, ${q(c.reply)}, 0, ${q(fmt(c.time))})`).join(',\n') + ';');
L.push('');

L.push('-- ---------- 6. 收藏 ----------');
L.push('INSERT INTO biz_favorite (favorite_id, user_id, item_id, create_time) VALUES');
L.push(favorites.map((f) => `(${f.id}, ${f.user}, ${f.item}, ${q(fmt(f.time))})`).join(',\n') + ';');
L.push('');
L.push('-- 收藏数回填');
L.push('UPDATE biz_item i SET favorite_count = (SELECT COUNT(*) FROM biz_favorite f WHERE f.item_id = i.item_id);');
L.push('');

L.push('-- ---------- 7. 举报 ----------');
L.push('INSERT INTO biz_report (report_id, item_id, reporter_id, reason_type, content, images, status, handle_user_id, handle_remark, handle_time, create_time) VALUES');
L.push(reports.map((r) => `(${r.id}, ${r.item}, ${r.reporter}, ${q(r.reason)}, ${q(r.content)}, NULL, ${r.status}, ${r.handleUser ?? 'NULL'}, ${q(r.handleRemark)}, ${r.handleTime ? q(fmt(r.handleTime)) : 'NULL'}, ${q(fmt(r.time))})`).join(',\n') + ';');
L.push('');

L.push('-- ---------- 8. 消息 ----------');
L.push('INSERT INTO biz_message (message_id, user_id, msg_type, title, content, biz_id, is_read, create_time) VALUES');
L.push(messages.map((m) => `(${m.id}, ${m.user}, ${m.type}, ${q(m.title)}, ${q(m.content)}, ${m.bizId}, ${m.read}, ${q(fmt(m.time))})`).join(',\n') + ';');
L.push('');

L.push('-- ---------- 9. AI 调用日志 ----------');
L.push('INSERT INTO ai_call_log (id, user_id, scene, template_code, provider, model, request_summary, prompt_tokens, completion_tokens, total_tokens, cost_ms, status, error_msg, create_time) VALUES');
L.push(aiLogs.map((a) => `(${a.id}, ${a.user}, ${q(a.scene)}, ${q(a.tpl)}, ${q(a.provider)}, ${q(a.model)}, ${q(a.summary)}, ${a.pTok}, ${a.cTok}, ${a.total}, ${a.cost}, ${a.status}, ${q(a.err)}, ${q(fmt(a.time))})`).join(',\n') + ';');
L.push('');
L.push('-- ---------- 10. 统计表：调用分析服务刷新 ----------');
L.push('--   curl -X POST "http://127.0.0.1:9203/analysis/refresh?days=90"');
L.push('');

fs.writeFileSync(OUT, L.join('\n'), 'utf8');

// ---------------- 控制台摘要 ----------------
const gmv = orders.filter((o) => o.status === 2).reduce((s, o) => s + o.amount, 0);
const byStatus = {};
for (const o of orders) byStatus[o.status] = (byStatus[o.status] || 0) + 1;
console.log(JSON.stringify({
  file: OUT, bytes: fs.statSync(OUT).size,
  users: users.length, items: items.length, orders: orders.length, comments: comments.length,
  favorites: favorites.length, reports: reports.length, messages: messages.length, aiLogs: aiLogs.length,
  gmv: gmv.toFixed(2), orderStatus: byStatus,
  itemStatus: items.reduce((m, i) => (m[i.status] = (m[i.status] || 0) + 1, m), {}),
}, null, 2));
