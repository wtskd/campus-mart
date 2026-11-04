/**
 * 格式化工具（契约 v1.1：字段名 = 数据库列名驼峰）
 */
import config from '@/config'

/** 金额格式化：1234.5 → ¥1,234.50 */
export function formatPrice(value) {
  const num = Number(value)
  if (!isFinite(num)) return '¥0.00'
  const fixed = num.toFixed(2)
  const [int, dec] = fixed.split('.')
  return '¥' + int.replace(/\B(?=(\d{3})+(?!\d))/g, ',') + '.' + dec
}

/** 纯数字金额（不带符号）：1234.5 → 1,234.50 */
export function formatAmount(value) {
  return formatPrice(value).replace('¥', '')
}

/** 数量格式化：1234 → 1.2k；12345 → 1.2万 */
export function formatCount(value) {
  const num = Number(value || 0)
  if (num >= 10000) return (num / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  if (num >= 1000) return (num / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  return String(num)
}

/** 商品封面：coverUrl 优先，其次 images[0]（统一补全图片域名） */
export function coverOf(item) {
  if (!item) return ''
  if (item.coverUrl) return mediaUrl(item.coverUrl)
  if (Array.isArray(item.images) && item.images.length) return mediaUrl(item.images[0])
  return ''
}

/** 商品图集：images 优先，否则回退封面（统一补全图片域名） */
export function imagesOf(item) {
  if (!item) return []
  if (Array.isArray(item.images) && item.images.length) return item.images.map((u) => mediaUrl(u))
  const cover = coverOf(item)
  return cover ? [cover] : []
}

/** 'yyyy-MM-dd HH:mm:ss' → Date（兼容 iOS 需要把 - 换成 /） */
export function parseTime(time) {
  if (!time) return null
  if (time instanceof Date) return time
  if (typeof time === 'number') return new Date(time)
  const str = String(time).replace(/-/g, '/')
  const d = new Date(str)
  return isNaN(d.getTime()) ? null : d
}

function pad(n) {
  return n < 10 ? '0' + n : String(n)
}

/** 完整时间 yyyy-MM-dd HH:mm */
export function formatDateTime(time) {
  const d = parseTime(time)
  if (!d) return ''
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 相对时间：刚刚 / 5分钟前 / 3小时前 / 昨天 / 10-27 */
export function timeAgo(time) {
  const d = parseTime(time)
  if (!d) return ''
  const diff = Date.now() - d.getTime()
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 24 * 60 * 60 * 1000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 48 * 60 * 60 * 1000) return '昨天'
  if (diff < 30 * 24 * 60 * 60 * 1000) return Math.floor(diff / 86400000) + '天前'
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 成色文案：9 → 9成新 */
export function conditionText(value) {
  const num = Number(value)
  if (!num) return ''
  if (num >= 10) return '全新'
  return num + '成新'
}

/** 商品状态（tinyint 0-6） */
export function itemStatusInfo(status) {
  const s = Number(status)
  return config.itemStatus[s] || { text: '未知', color: '#9aa1ab' }
}

/** 订单状态（tinyint 0-5） */
export function orderStatusInfo(status) {
  const s = Number(status)
  return config.orderStatus[s] || { text: '未知', color: '#9aa1ab' }
}

/** 消息类型 */
export function msgTypeInfo(type) {
  return config.msgType[Number(type)] || { text: '消息', icon: '📩', color: '#9aa1ab' }
}

/** 交易方式 */
export function tradeTypeText(type) {
  return config.tradeType[Number(type)] || '均可'
}

/** 手机号简单校验 */
export function isPhone(value) {
  return /^1[3-9]\d{9}$/.test(String(value || ''))
}

/** 防抖 */
export function debounce(fn, delay = 300) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), delay)
  }
}

/**
 * 图片地址补全
 * 后端返回相对路径（如 /upload/demo/item-01.png）：
 *  - H5：走 vite proxy（/upload → http://localhost:8081），保持相对路径
 *  - 小程序/App：拼上网关地址
 */
export function mediaUrl(url) {
  if (!url) return ''
  if (/^https?:/.test(url)) return url
  // 本地静态资源（/static/**）直接使用，不做域名拼接
  if (url.indexOf('/static/') === 0) return url
  if (url.charAt(0) === '/') {
    // #ifdef H5
    return url
    // #endif
    // #ifndef H5
    return config.baseUrl.replace(/\/api$/, '') + url
    // #endif
  }
  return url
}

/** 空值兜底 */
export function orDash(value, dash = '-') {
  return value === undefined || value === null || value === '' ? dash : value
}
