/**
 * 通用格式化工具
 */

/** 空值占位 */
export const EMPTY = '-'

/** 时间：后端返回 "2026-10-27 10:20:30" 或 ISO 字符串，统一截断到秒展示 */
export function formatDateTime(value, empty = EMPTY) {
  if (!value) return empty
  return String(value).replace('T', ' ').slice(0, 19)
}

/** 只展示日期部分 */
export function formatDate(value, empty = EMPTY) {
  if (!value) return empty
  return String(value).replace('T', ' ').slice(0, 10)
}

/** 金额：1280.5 -> ¥1,280.50 */
export function formatMoney(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return '¥0.00'
  return `¥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

/** 金额（不带符号）：用于图表、表格 */
export function formatAmount(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return '0.00'
  return num.toFixed(2)
}

/** 数字千分位 */
export function formatNumber(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return '0'
  return num.toLocaleString('zh-CN')
}

/** 环比：0.12 -> { text: '+12.00%', type: 'up' } */
export function formatCompare(value) {
  if (value == null || Number.isNaN(Number(value))) return { text: '0.00%', type: 'flat', rate: 0 }
  const rate = Number(value)
  return {
    text: `${rate >= 0 ? '+' : ''}${(rate * 100).toFixed(2)}%`,
    type: rate > 0 ? 'up' : rate < 0 ? 'down' : 'flat',
    rate
  }
}

/** 耗时毫秒展示 */
export function formatCost(ms) {
  const num = Number(ms)
  if (!Number.isFinite(num)) return '-'
  return num >= 1000 ? `${(num / 1000).toFixed(2)}s` : `${Math.round(num)}ms`
}
