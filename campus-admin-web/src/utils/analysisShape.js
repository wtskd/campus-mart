/**
 * 分析域响应归一化（纯函数，无 Vue/axios 依赖）
 *
 * 后端 campus-analysis 的 DTO 与页面渲染结构不一致：趋势/RFM/留存都包了一层对象，
 * 概览字段用 todayXxx/totalXxx 命名，AI 统计只有 scenes[]。
 * 统一在这里转成页面好用的结构，同时便于 Node 侧（tools/check-admin-shapes.mjs）直接 import 做契约校验。
 */

export const toArray = (data, key) => {
  if (Array.isArray(data)) return data
  const list = data?.[key]
  return Array.isArray(list) ? list : []
}

/** 趋势点统一：statDate + 数字字段 */
export function normalizeTrend(data) {
  return toArray(data, 'points').map((item) => ({
    ...item,
    statDate: item.statDate || item.date || '',
    gmv: Number(item.gmv ?? 0),
    orderCount: Number(item.orderCount ?? 0),
    finishOrderCount: Number(item.finishOrderCount ?? 0),
    newUserCount: Number(item.newUserCount ?? 0),
    newItemCount: Number(item.newItemCount ?? item.itemPublishCount ?? 0)
  }))
}

/** RFM 分层：后端 { segments[] } */
export function normalizeRfm(data) {
  return toArray(data, 'segments').map((item) => ({
    ...item,
    userCount: Number(item.userCount ?? 0),
    monetary: Number(item.monetary ?? 0),
    avgMonetary: Number(item.avgMonetary ?? 0),
    userRatio: Number(item.userRatio ?? 0),
    monetaryRatio: Number(item.monetaryRatio ?? 0)
  }))
}

/** AI 消耗：后端只有 scenes[]，补出 byScene / byDay 两个页面通用结构 */
export function normalizeAiUsage(data) {
  const usage = data || {}
  const scenes = Array.isArray(usage.scenes) ? usage.scenes : []
  return {
    ...usage,
    byScene: Array.isArray(usage.byScene)
      ? usage.byScene
      : scenes.map((item) => ({ scene: item.scene, count: Number(item.callCount ?? 0) })),
    byDay: Array.isArray(usage.byDay) ? usage.byDay : []
  }
}

/** 留存：后端 { points[] }，点里是 activeUsers/retained1/retained3/retained7 */
export function normalizeRetention(data) {
  const rows = toArray(data, 'points')
  return rows.map((item, index) => {
    const active = Number(item.activeUsers ?? item.activeUser ?? 0)
    const retained = Number(item.retained1 ?? item.retained ?? 0)
    // 最后一天（今天）还没到「次日」，后端 retained1=0 会把留存率算成 0%，
    // 画到折线图上就是末尾突然掉到 0 的假象；这里置 null 让折线在该点断开
    const immature = index === rows.length - 1 && retained === 0
    const rate =
      item.rate1 != null
        ? Number(item.rate1) * 100
        : item.retentionRate != null
          ? Number(item.retentionRate)
          : active > 0
            ? (retained / active) * 100
            : 0
    return {
      ...item,
      date: item.date || item.statDate || '',
      activeUser: active,
      newUser: Number(item.newUser ?? 0),
      retentionRate: immature ? null : Number(rate.toFixed(2))
    }
  })
}

/** 概览卡片：后端 todayXxx / totalXxx 命名 → 页面卡片口径 */
export function normalizeOverview(data) {
  const d = data || {}
  return {
    ...d,
    todayGmv: Number(d.todayGmv ?? 0),
    totalGmv: Number(d.totalGmv ?? 0),
    orderCount: Number(d.todayOrderCount ?? d.orderCount ?? 0),
    finishCount: Number(d.todayFinishOrderCount ?? d.finishCount ?? 0),
    cancelCount: Number(d.todayCancelCount ?? d.cancelCount ?? 0),
    newUserCount: Number(d.todayNewUserCount ?? d.newUserCount ?? 0),
    userCount: Number(d.totalUserCount ?? d.userCount ?? 0),
    itemCount: Number(d.totalItemCount ?? d.itemCount ?? 0),
    onSaleCount: Number(d.onSaleItemCount ?? d.onSaleCount ?? 0),
    aiCallCount: Number(d.todayAiCallCount ?? d.aiCallCount ?? 0),
    gmvGrowth: d.gmvCompare != null ? Number(d.gmvCompare) * 100 : d.gmvGrowth,
    orderGrowth: d.orderCompare != null ? Number(d.orderCompare) * 100 : d.orderGrowth
  }
}

/** 大屏聚合：DashboardDTO 的 singular 字段名 → 页面复数命名 */
export function normalizeScreen(data) {
  const d = data || {}
  return {
    ...d,
    overview: normalizeOverview(d.overview),
    trend: normalizeTrend(d.trend),
    trend30: normalizeTrend(d.trend30),
    category: toArray(d.category, 'list'),
    rfm: normalizeRfm(d.rfm),
    aiUsage: normalizeAiUsage(d.aiUsage),
    hotItems: toArray(d.hotItem || d.hotItems, 'list'),
    recentOrders: toArray(d.recentOrder || d.recentOrders, 'list')
  }
}
