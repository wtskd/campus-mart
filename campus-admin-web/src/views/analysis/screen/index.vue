<template>
  <div ref="screenRef" class="screen-root">
    <!-- 1920x1080 设计稿，等比缩放适配任意显示器 -->
    <div class="screen" :style="{ transform: `scale(${scale})` }">
      <header class="screen-header">
        <div class="header-side left">
          <span class="dot" :class="apiOk ? 'ok' : 'bad'"></span>
          <span class="side-text">{{ apiOk ? '网关连接正常' : '等待网关连接…' }}</span>
          <span class="side-text">｜数据更新：{{ updatedAt || '--:--:--' }}</span>
          <div class="exit-btn" title="返回后台首页（Esc）" @click="exitScreen">
            <el-icon><Back /></el-icon>
            <span>退出大屏</span>
          </div>
        </div>
        <h1 class="header-title">校园易站 · 运营数据大屏</h1>
        <div class="header-side right">
          <span class="side-text">{{ today }}</span>
          <span class="clock">{{ clock }}</span>
          <div class="fs-btn" :title="isFullscreen ? '退出全屏（Esc）' : '全屏展示'" @click="toggleFullscreen">
            <el-icon><FullScreen /></el-icon>
            <span>{{ isFullscreen ? '退出全屏（Esc）' : '全屏' }}</span>
          </div>
        </div>
      </header>

      <main class="screen-body">
        <!-- 左列 -->
        <section class="col col-left">
          <div class="panel">
            <div class="panel-title"><i></i>交易趋势（近 30 天）</div>
            <BaseChart :option="trendOption" height="300px" />
          </div>
          <div class="panel">
            <div class="panel-title"><i></i>分类成交占比</div>
            <BaseChart :option="categoryOption" height="300px" />
          </div>
          <div class="panel">
            <div class="panel-title"><i></i>转化漏斗（浏览→成交）</div>
            <BaseChart :option="funnelOption" height="280px" />
          </div>
        </section>

        <!-- 中列 -->
        <section class="col col-center">
          <div class="kpi-row">
            <div v-for="item in kpis" :key="item.label" class="kpi">
              <div class="kpi-label">{{ item.label }}</div>
              <div class="kpi-value">{{ item.prefix }}{{ item.value }}</div>
              <div class="kpi-sub">{{ item.sub }}</div>
            </div>
          </div>

          <div class="panel center-panel">
            <div class="panel-title"><i></i>GMV 与订单双轴趋势</div>
            <BaseChart :option="centerTrendOption" height="360px" />
          </div>

          <div class="panel">
            <div class="panel-title"><i></i>实时成交订单</div>
            <div class="order-list">
              <div class="order-head">
                <span>订单号</span><span>商品</span><span>买家</span><span>金额</span><span>状态</span>
              </div>
              <div v-for="order in recentOrders" :key="order.orderId || order.orderNo" class="order-row">
                <span class="mono">{{ order.orderNo || order.orderId }}</span>
                <span class="ellipsis">{{ order.itemTitle || order.title || '-' }}</span>
                <span>{{ order.buyerName || order.buyerNickname || '-' }}</span>
                <span class="money">¥{{ formatMoney(order.amount ?? order.payAmount) }}</span>
                <span>
                  <!-- /analysis/screen 的 recentOrder 只回成交单（无 status 字段），默认按「已完成」展示 -->
                  <el-tag size="small" :type="orderStatusType(order.status ?? 2)">{{ orderStatusText(order.status ?? 2) }}</el-tag>
                </span>
              </div>
              <div v-if="!recentOrders.length" class="empty">暂无数据（等待后端 /analysis/screen 返回）</div>
            </div>
          </div>
        </section>

        <!-- 右列 -->
        <section class="col col-right">
          <div class="panel">
            <div class="panel-title"><i></i>RFM 用户分层</div>
            <BaseChart :option="rfmOption" height="280px" />
          </div>
          <div class="panel">
            <div class="panel-title"><i></i>AI 使用量（近 30 天）</div>
            <BaseChart :option="aiOption" height="260px" />
          </div>
          <div class="panel">
            <div class="panel-title"><i></i>热门商品 TOP 8</div>
            <div class="hot-list">
              <div v-for="(item, index) in hotItems" :key="item.itemId || index" class="hot-row">
                <span class="rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
                <span class="ellipsis">{{ item.title }}</span>
                <span class="hot-gmv">¥{{ formatMoney(item.gmv) }}</span>
              </div>
              <div v-if="!hotItems.length" class="empty">暂无数据</div>
            </div>
          </div>
        </section>
      </main>

      <footer class="screen-footer">
        <span>CampusMart 企业实习结课项目 · Vue3 + ECharts 数据可视化</span>
        <span>数据来源：campus-analysis 统计表（T+1 预聚合） · 每 60 秒自动刷新</span>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Back, FullScreen } from '@element-plus/icons-vue'
import BaseChart from '@/components/BaseChart/index.vue'
import { ElMessage } from 'element-plus'
import {
  getAiUsage,
  getCategoryAnalysis,
  getFunnel,
  getHotItems,
  getOverview,
  getRfm,
  getScreenData,
  getTrend
} from '@/api/analysis'
import { formatMoney, formatNumber } from '@/utils/format'

/** 数据大屏：深色科技风，1920×1080 设计稿 + 等比缩放 */
const router = useRouter()
const screenRef = ref(null)
const scale = ref(1)
const apiOk = ref(false)
const updatedAt = ref('')
const clock = ref('')
const today = ref('')

const overview = ref({})
const trend = ref([])
const categories = ref([])
const funnel = ref({})
const rfm = ref([])
const aiUsage = ref({})
const hotItems = ref([])
const recentOrders = ref([])

let scaleTimer = null
let clockTimer = null
let refreshTimer = null

function recalcScale() {
  const w = window.innerWidth
  const h = window.innerHeight
  scale.value = Math.min(w / 1920, h / 1080)
}

function updateClock() {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  clock.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  today.value = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} 星期${'日一二三四五六'[now.getDay()]}`
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    screenRef.value?.requestFullscreen?.().catch(() => {
      ElMessage.warning('浏览器拒绝了全屏请求，可按 F11 进入全屏')
    })
  } else {
    document.exitFullscreen?.().catch(() => {})
  }
}

/** 大屏数据加载：优先聚合接口，缺字段时用各子接口补齐，后端未启动也能显示空态 */
async function loadScreen() {
  let data = null
  try {
    data = await getScreenData()
    apiOk.value = true
  } catch (error) {
    apiOk.value = false
    data = null
  }
  overview.value = data?.overview || {}
  trend.value = data?.trend || []
  categories.value = data?.category || []
  funnel.value = data?.funnel || {}
  rfm.value = data?.rfm || []
  aiUsage.value = data?.aiUsage || {}
  hotItems.value = data?.hotItems || []
  recentOrders.value = data?.recentOrders || data?.orders || []

  // 聚合接口字段不全（或后端暂未实现 /screen）时，逐个补齐
  const tasks = []
  if (!trend.value.length) tasks.push(getTrend(30).then((res) => (trend.value = res || [])).catch(() => {}))
  if (!categories.value.length) tasks.push(getCategoryAnalysis().then((res) => (categories.value = res || [])).catch(() => {}))
  if (!funnel.value || !funnel.value.viewCount) tasks.push(getFunnel(30).then((res) => (funnel.value = res || {})).catch(() => {}))
  if (!rfm.value.length) tasks.push(getRfm().then((res) => (rfm.value = res || [])).catch(() => {}))
  if (!hotItems.value.length) tasks.push(getHotItems(8).then((res) => (hotItems.value = res || [])).catch(() => {}))
  if (!aiUsage.value?.byDay?.length) tasks.push(getAiUsage().then((res) => (aiUsage.value = res || {})).catch(() => {}))
  if (!overview.value || !overview.value.todayGmv) tasks.push(getOverview().then((res) => (overview.value = res || {})).catch(() => {}))
  await Promise.all(tasks)

  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  updatedAt.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

const kpis = computed(() => {
  const o = overview.value || {}
  return [
    { label: '今日 GMV', prefix: '¥', value: formatMoney(o.todayGmv), sub: `累计 ${formatMoney(o.totalGmv)}` },
    { label: '今日订单', prefix: '', value: formatNumber(o.orderCount), sub: `完成 ${formatNumber(o.finishCount)}` },
    { label: '平台用户', prefix: '', value: formatNumber(o.userCount), sub: `在售商品 ${formatNumber(o.onSaleCount)}` },
    { label: 'AI 调用', prefix: '', value: formatNumber(o.aiCallCount), sub: '累计调用次数' }
  ]
})

const axisCommon = {
  axisLine: { lineStyle: { color: 'rgba(120,170,255,.35)' } },
  axisLabel: { color: '#9fc0ff', fontSize: 11 },
  splitLine: { lineStyle: { color: 'rgba(120,170,255,.12)' } }
}

const trendOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'axis' },
  grid: { left: 52, right: 20, top: 30, bottom: 28 },
  xAxis: { type: 'category', boundaryGap: false, data: trend.value.map((i) => String(i.statDate || '').slice(5)), ...axisCommon },
  yAxis: { type: 'value', ...axisCommon },
  series: [
    {
      name: 'GMV',
      type: 'line',
      smooth: true,
      symbolSize: 5,
      showSymbol: false,
      lineStyle: { width: 2, color: '#37e6ff' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(55,230,255,.45)' }, { offset: 1, color: 'rgba(55,230,255,.02)' }]
        }
      },
      data: trend.value.map((i) => i.gmv ?? 0)
    }
  ]
}))

const centerTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['GMV', '订单数'], right: 16, textStyle: { color: '#9fc0ff' } },
  grid: { left: 60, right: 56, top: 44, bottom: 30 },
  xAxis: { type: 'category', boundaryGap: false, data: trend.value.map((i) => String(i.statDate || '').slice(5)), ...axisCommon },
  yAxis: [
    { type: 'value', name: 'GMV', nameTextStyle: { color: '#9fc0ff' }, ...axisCommon },
    { type: 'value', name: '订单', nameTextStyle: { color: '#9fc0ff' }, splitLine: { show: false }, axisLabel: { color: '#9fc0ff' }, axisLine: { lineStyle: { color: 'rgba(120,170,255,.35)' } } }
  ],
  series: [
    {
      name: 'GMV',
      type: 'line',
      smooth: true,
      yAxisIndex: 0,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2.5, color: '#ffd666' },
      itemStyle: { color: '#ffd666' },
      data: trend.value.map((i) => i.gmv ?? 0)
    },
    {
      name: '订单数',
      type: 'bar',
      yAxisIndex: 1,
      barWidth: 10,
      itemStyle: {
        borderRadius: [3, 3, 0, 0],
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#7c4dff' }, { offset: 1, color: 'rgba(124,77,255,.15)' }] }
      },
      data: trend.value.map((i) => i.orderCount ?? 0)
    }
  ]
}))

const COLOR_POOL = ['#37e6ff', '#7c4dff', '#ffd666', '#36cfc9', '#ff9c6e', '#69b1ff', '#b37feb', '#95de64']

const categoryOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>订单 {c}（{d}%）' },
  legend: { type: 'scroll', bottom: 0, textStyle: { color: '#9fc0ff', fontSize: 11 }, pageTextStyle: { color: '#9fc0ff' } },
  series: [
    {
      type: 'pie',
      radius: ['40%', '66%'],
      center: ['50%', '44%'],
      color: COLOR_POOL,
      label: { color: '#cfe3ff', fontSize: 11, formatter: '{b}\n{d}%' },
      itemStyle: { borderColor: 'rgba(5,10,24,.9)', borderWidth: 2 },
      data: categories.value.map((i) => ({ name: i.categoryName || ('分类' + i.categoryId), value: i.orderCount ?? i.gmv ?? 0 }))
    }
  ]
}))

const funnelOption = computed(() => {
  const f = funnel.value || {}
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}' },
    series: [
      {
        type: 'funnel',
        left: '8%',
        width: '84%',
        top: 8,
        bottom: 8,
        minSize: '18%',
        sort: 'descending',
        gap: 2,
        label: { color: '#dceaff', fontSize: 11, formatter: '{b} {c}' },
        itemStyle: { borderColor: 'rgba(5,10,24,.9)', borderWidth: 1 },
        color: ['#37e6ff', '#69b1ff', '#7c4dff', '#ffd666'],
        data: [
          { name: '浏览', value: f.viewCount ?? 0 },
          { name: '收藏', value: f.favoriteCount ?? 0 },
          { name: '下单', value: f.orderCount ?? 0 },
          { name: '成交', value: f.finishCount ?? 0 }
        ]
      }
    ]
  }
})

const rfmOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 人（{d}%）' },
  legend: {
    type: 'scroll',
    orient: 'vertical',
    right: 4,
    top: 'middle',
    itemWidth: 8,
    itemHeight: 8,
    itemGap: 6,
    textStyle: { color: '#9fc0ff', fontSize: 11 },
    formatter: (name) => String(name).replace('客户', '')
  },
  series: [
    {
      type: 'pie',
      radius: ['40%', '64%'],
      center: ['36%', '50%'],
      color: COLOR_POOL,
      minAngle: 3,
      avoidLabelOverlap: true,
      // 九个分层名放进标签会互相压字，统一收到右侧图例 + hover 提示
      label: { show: false },
      labelLine: { show: false },
      emphasis: {
        scale: true,
        scaleSize: 5,
        label: { show: true, color: '#cfe3ff', fontSize: 11, formatter: '{b}\n{c}人' }
      },
      itemStyle: { borderColor: 'rgba(5,10,24,.9)', borderWidth: 2 },
      data: rfm.value.map((i) => ({ name: i.segment || '-', value: Number(i.userCount || 0) }))
    }
  ]
}))

const SCENE_LABELS = {
  copywrite: '文案生成',
  chat: '智能客服',
  price: '智能估价',
  audit: '内容审核',
  recommend: '个性推荐'
}

const aiOption = computed(() => {
  // 聚合接口目前只给「按场景」维度（byScene），按天维度为空时退化为场景柱状图，
  // 避免大屏上出现一张空白面板
  const byDay = aiUsage.value?.byDay || []
  const byScene = aiUsage.value?.byScene || []
  const useDay = byDay.length > 0
  const rows = useDay ? byDay : byScene
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 16, top: 26, bottom: 24 },
    xAxis: {
      type: 'category',
      data: rows.map((i) => (useDay ? String(i.date || '').slice(5) : SCENE_LABELS[i.scene] || i.scene || '-')),
      ...axisCommon
    },
    yAxis: { type: 'value', ...axisCommon },
    series: [
      {
        type: 'bar',
        barWidth: 14,
        itemStyle: {
          borderRadius: [3, 3, 0, 0],
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#36cfc9' }, { offset: 1, color: 'rgba(54,207,201,.15)' }] }
        },
        data: rows.map((i) => Number(i.count ?? i.requestCount ?? i.callCount ?? 0))
      }
    ]
  }
})

/** 订单状态与后端 OrderStatus 枚举保持一致：0 待确认 1 待交付 2 已完成 3 已取消 4 退款中 5 已退款 */
function orderStatusType(status) {
  return { '0': 'info', '1': 'warning', '2': 'success', '3': 'info', '4': 'danger', '5': 'danger' }[String(status)] || 'info'
}

function orderStatusText(status) {
  return { '0': '待确认', '1': '待交付', '2': '已完成', '3': '已取消', '4': '退款中', '5': '已退款' }[String(status)] || String(status)
}

/** 全屏状态：由 fullscreenchange 事件同步，保证按 Esc / F11 退出后按钮文字也能还原 */
const isFullscreen = ref(false)

function syncFullscreen() {
  isFullscreen.value = Boolean(document.fullscreenElement)
}

function onKeydown(event) {
  if (event.key !== 'Escape') return
  // 第一下 Esc：先退出浏览器全屏；已经不在全屏时：退出大屏回到后台首页
  if (document.fullscreenElement) {
    document.exitFullscreen?.().catch(() => {})
  } else {
    exitScreen()
  }
}

/** 退出大屏：不在全屏时就回到后台首页（数据概览），在全屏时先解除全屏再返回 */
function exitScreen() {
  const go = () => router.push('/dashboard').catch(() => router.replace('/dashboard'))
  if (document.fullscreenElement) {
    document
      .exitFullscreen?.()
      .catch(() => {})
      .finally(go)
  } else {
    go()
  }
}

onMounted(() => {
  recalcScale()
  updateClock()
  loadScreen()
  clockTimer = setInterval(updateClock, 1000)
  refreshTimer = setInterval(loadScreen, 60000)
  window.addEventListener('resize', recalcScale)
  document.addEventListener('fullscreenchange', syncFullscreen)
  window.addEventListener('keydown', onKeydown)
  syncFullscreen()
})

onBeforeUnmount(() => {
  clearInterval(clockTimer)
  clearInterval(refreshTimer)
  clearTimeout(scaleTimer)
  window.removeEventListener('resize', recalcScale)
  document.removeEventListener('fullscreenchange', syncFullscreen)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.screen-root {
  position: fixed;
  inset: 0;
  background: radial-gradient(1200px 600px at 50% -10%, #0f2a58 0%, #050a18 55%, #03060f 100%);
  overflow: hidden;
  z-index: 3000;
}

/* 细网格背景 */
.screen-root::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(80, 140, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(80, 140, 255, 0.06) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
}

.screen {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 1920px;
  height: 1080px;
  margin-left: -960px;
  margin-top: -540px;
  transform-origin: center center;
  display: flex;
  flex-direction: column;
  padding: 10px 18px 6px;
  box-sizing: border-box;
  color: #dceaff;
}

.screen-header {
  position: relative;
  height: 74px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(70, 140, 255, 0.25);
}

.screen-header::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -2px;
  width: 480px;
  height: 3px;
  margin-left: -240px;
  background: linear-gradient(90deg, transparent, #37e6ff, transparent);
  box-shadow: 0 0 18px #37e6ff;
}

.header-title {
  margin: 0;
  font-size: 34px;
  letter-spacing: 6px;
  font-weight: 700;
  background: linear-gradient(180deg, #ffffff 10%, #6fd3ff 90%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 22px rgba(80, 190, 255, 0.35);
}

.header-side {
  position: absolute;
  top: 26px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #8fb6e8;
}

.header-side.left {
  left: 8px;
}

.header-side.right {
  right: 8px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

/* 退出大屏：左侧「返回后台」按钮，和右侧全屏按钮同一套描边样式 */
.exit-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-left: 14px;
  padding: 4px 10px;
  border: 1px solid rgba(55, 230, 255, 0.35);
  border-radius: 4px;
  color: #8fb6e8;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.exit-btn:hover {
  color: #37e6ff;
  border-color: #37e6ff;
  background: rgba(55, 230, 255, 0.08);
}

.dot.ok {
  background: #52c41a;
  box-shadow: 0 0 8px #52c41a;
}

.dot.bad {
  background: #ff4d4f;
  box-shadow: 0 0 8px #ff4d4f;
}

.clock {
  font-family: Consolas, monospace;
  font-size: 18px;
  color: #37e6ff;
  letter-spacing: 1px;
}

.fs-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-left: 14px;
  padding: 4px 10px;
  border: 1px solid rgba(55, 230, 255, 0.35);
  border-radius: 4px;
  color: #8fb6e8;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.fs-btn:hover {
  color: #37e6ff;
  border-color: #37e6ff;
  background: rgba(55, 230, 255, 0.08);
}

.screen-body {
  flex: 1;
  display: grid;
  grid-template-columns: 460px 1fr 460px;
  gap: 12px;
  padding: 12px 0 6px;
  min-height: 0;
}

.col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.panel {
  position: relative;
  flex: 1;
  min-height: 0;
  padding: 8px 10px 6px;
  background: linear-gradient(180deg, rgba(18, 40, 84, 0.55) 0%, rgba(8, 18, 40, 0.55) 100%);
  border: 1px solid rgba(70, 140, 255, 0.28);
  border-radius: 6px;
  box-shadow: inset 0 0 24px rgba(50, 130, 255, 0.08);
}

.panel::before,
.panel::after {
  content: '';
  position: absolute;
  width: 10px;
  height: 10px;
  border-color: #37e6ff;
  border-style: solid;
}

.panel::before {
  left: -1px;
  top: -1px;
  border-width: 2px 0 0 2px;
}

.panel::after {
  right: -1px;
  bottom: -1px;
  border-width: 0 2px 2px 0;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #bfe3ff;
  letter-spacing: 1px;
  padding: 2px 0 4px;
}

.panel-title i {
  width: 4px;
  height: 14px;
  background: linear-gradient(180deg, #37e6ff, #7c4dff);
  border-radius: 2px;
  box-shadow: 0 0 8px rgba(55, 230, 255, 0.7);
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.kpi {
  padding: 12px 14px;
  border-radius: 6px;
  background: linear-gradient(180deg, rgba(30, 70, 140, 0.45), rgba(10, 22, 50, 0.6));
  border: 1px solid rgba(70, 140, 255, 0.3);
}

.kpi-label {
  font-size: 13px;
  color: #8fb6e8;
}

.kpi-value {
  margin: 8px 0 4px;
  font-size: 30px;
  font-weight: 700;
  color: #ffd666;
  font-family: Consolas, monospace;
  text-shadow: 0 0 16px rgba(255, 214, 102, 0.4);
}

.kpi-sub {
  font-size: 12px;
  color: #6f93c4;
}

.center-panel {
  flex: 1.2;
}

.order-list,
.hot-list {
  height: calc(100% - 30px);
  overflow: hidden;
  font-size: 12px;
}

.order-head,
.order-row {
  display: grid;
  grid-template-columns: 1.1fr 2.2fr 0.9fr 0.9fr 0.8fr;
  gap: 8px;
  padding: 6px 6px;
  align-items: center;
}

.order-head {
  color: #6f93c4;
  border-bottom: 1px solid rgba(70, 140, 255, 0.25);
}

.order-row {
  color: #cfe3ff;
  border-bottom: 1px dashed rgba(70, 140, 255, 0.12);
}

.mono {
  font-family: Consolas, monospace;
  color: #7fd6ff;
}

.money {
  color: #ffd666;
  font-family: Consolas, monospace;
}

.ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.hot-row {
  display: grid;
  grid-template-columns: 26px 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 7px 4px;
  border-bottom: 1px dashed rgba(70, 140, 255, 0.14);
  color: #dceaff;
}

.rank {
  display: inline-flex;
  width: 20px;
  height: 20px;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  background: rgba(70, 140, 255, 0.25);
  color: #9fc0ff;
  font-size: 12px;
  font-weight: 700;
}

.rank-1 {
  background: linear-gradient(135deg, #ffd666, #ff9c6e);
  color: #3a2500;
}

.rank-2 {
  background: linear-gradient(135deg, #d9e4f5, #a9bfdd);
  color: #22304a;
}

.rank-3 {
  background: linear-gradient(135deg, #ffb27a, #d97742);
  color: #3a1b00;
}

.hot-gmv {
  color: #ffd666;
  font-family: Consolas, monospace;
}

.empty {
  padding: 24px 0;
  text-align: center;
  color: #5b7aa8;
}

.screen-footer {
  display: flex;
  justify-content: space-between;
  padding: 4px 6px 0;
  font-size: 12px;
  color: #5b7aa8;
  border-top: 1px solid rgba(70, 140, 255, 0.18);
}
</style>
