import request from '@/utils/request'
import {
  normalizeAiUsage,
  normalizeOverview,
  normalizeRetention,
  normalizeRfm,
  normalizeScreen,
  normalizeTrend
} from '@/utils/analysisShape'

/**
 * 数据分析接口（campus-analysis，网关前缀 /api/analysis）
 * 契约 v1.1 §6
 */
const PREFIX = '/analysis'

/** 概览卡片：{ todayGmv, totalGmv, orderCount, finishCount, cancelCount, itemCount, onSaleCount, userCount, aiCallCount, gmvGrowth, orderGrowth } */
export const getOverview = () => request.get(`${PREFIX}/overview`).then(normalizeOverview)

/** 趋势：days=7|30 -> [{ statDate, gmv, orderCount, finishOrderCount, itemPublishCount, newUserCount, activeUserCount }] */
export const getTrend = (days = 30) =>
  request.get(`${PREFIX}/trend`, { params: { days } }).then(normalizeTrend)

/** 分类成交：[{ categoryId, categoryName, itemCount, orderCount, gmv }] */
export const getCategoryAnalysis = () => request.get(`${PREFIX}/category`)

/** 热门商品：[{ itemId, title, coverUrl, viewCount, favoriteCount, gmv }] */
export const getHotItems = (limit = 10) => request.get(`${PREFIX}/hot-item`, { params: { limit } })

/** 卖家成交排行 */
export const getSellerRank = (limit = 10) => request.get(`${PREFIX}/seller-rank`, { params: { limit } })

/** 转化漏斗：{ viewCount, favoriteCount, orderCount, finishCount, viewToOrderRate, orderToFinishRate } */
export const getFunnel = (days = 30) => request.get(`${PREFIX}/funnel`, { params: { days } })

/** RFM 分层：[{ segment, userCount, avgMonetary, userRatio }] */
export const getRfm = () => request.get(`${PREFIX}/rfm`).then(normalizeRfm)

/** 留存：[{ date, activeUser, newUser, retentionRate }] */
export const getRetention = (days = 7) =>
  request.get(`${PREFIX}/retention`, { params: { days } }).then(normalizeRetention)

/** AI 消耗：{ totalCalls, totalTokens, byScene, byDay, successRate } */
export const getAiUsage = () => request.get(`${PREFIX}/ai-usage`).then(normalizeAiUsage)

/** 大屏聚合（概览 + 趋势 + 分类 + 漏斗 + 热词 + 实时订单） */
export const getScreenData = () => request.get(`${PREFIX}/screen`).then(normalizeScreen)

/** 立即重算 T+1 预聚合表 */
export const refreshStat = () => request.post(`${PREFIX}/refresh`)

/** CSV 导出（返回 blob，页面用 downloadBlob 保存）；后端接口为 /analysis/export/order */
export const exportStat = (type = 'gmv', days = 30) => {
  const end = new Date()
  const start = new Date(end.getTime() - (Math.max(1, days) - 1) * 86400000)
  const fmt = (d) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  return request.get(`${PREFIX}/export/order`, {
    params: { startDate: fmt(start), endDate: fmt(end), type },
    responseType: 'blob'
  })
}

