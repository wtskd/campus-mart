<template>
  <div class="dashboard page-container">
    <!-- 顶部指标卡：GMV / 订单 / 新增用户 / 在售商品 + 环比箭头 -->
    <el-row :gutter="12" class="metric-row">
      <el-col v-for="card in metricCards" :key="card.key" :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="metric-card" v-loading="loading.overview">
          <div class="metric-head">
            <span class="metric-label">{{ card.label }}</span>
            <el-tag v-if="card.growth !== null && card.growth !== undefined" size="small" :type="growthType(card.growth)">
              <el-icon><component :is="growthType(card.growth) === 'danger' ? 'Bottom' : 'Top'" /></el-icon>
              {{ Math.abs(card.growth).toFixed(1) }}%
            </el-tag>
          </div>
          <div class="metric-value">{{ card.prefix }}{{ formatNumber(card.value) }}</div>
          <div class="metric-desc">{{ card.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :span="16">
        <el-card shadow="never" class="chart-card" v-loading="loading.trend">
          <div class="chart-head">
            <span class="chart-title">交易趋势</span>
            <div class="chart-actions">
              <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
                <el-radio-button :value="7">近 7 天</el-radio-button>
                <el-radio-button :value="30">近 30 天</el-radio-button>
              </el-radio-group>
              <el-button size="small" :icon="Refresh" :loading="refreshing" @click="handleRefresh">
                重算统计
              </el-button>
            </div>
          </div>
          <BaseChart :option="trendOption" height="330px" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="chart-card" v-loading="loading.category">
          <div class="chart-head"><span class="chart-title">分类成交占比</span></div>
          <BaseChart :option="categoryOption" height="330px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :span="8">
        <el-card shadow="never" class="chart-card" v-loading="loading.funnel">
          <div class="chart-head"><span class="chart-title">转化漏斗</span><span class="sub">近 30 天</span></div>
          <BaseChart :option="funnelOption" height="260px" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="chart-card" v-loading="loading.hot">
          <div class="chart-head"><span class="chart-title">热门商品榜</span></div>
          <el-table :data="hotItems" size="small" :show-header="false" v-loading="loading.hot">
            <el-table-column type="index" width="42" />
            <el-table-column prop="title" show-overflow-tooltip />
            <el-table-column label="浏览量" width="86" align="right">
              <template #default="{ row }">{{ formatNumber(row.viewCount) }}</template>
            </el-table-column>
            <el-table-column label="GMV" width="90" align="right">
              <template #default="{ row }">
                <span class="money">{{ formatMoney(row.gmv) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="chart-card" v-loading="loading.ai">
          <div class="chart-head"><span class="chart-title">AI 使用量</span><span class="sub">近 30 天</span></div>
          <BaseChart :option="aiOption" height="260px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card" v-loading="loading.rfm">
          <div class="chart-head"><span class="chart-title">RFM 用户分层</span></div>
          <BaseChart :option="rfmOption" height="280px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card" v-loading="loading.seller">
          <div class="chart-head"><span class="chart-title">卖家成交排行</span><span class="sub">Top 10</span></div>
          <el-table :data="sellerRank" size="small" v-loading="loading.seller">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="nickname" label="卖家">
              <template #default="{ row }">{{ row.nickname || row.sellerName || row.username || ('用户 ' + row.userId) }}</template>
            </el-table-column>
            <el-table-column prop="orderCount" label="成交订单" width="100" align="right" />
            <el-table-column label="成交额" width="120" align="right">
              <template #default="{ row }">{{ formatMoney(row.gmv ?? row.amount) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import BaseChart from '@/components/BaseChart/index.vue'
import {
  exportStat,
  getAiUsage,
  getCategoryAnalysis,
  getFunnel,
  getHotItems,
  getOverview,
  getRfm,
  getSellerRank,
  getTrend,
  refreshStat
} from '@/api/analysis'
import { formatMoney, formatNumber } from '@/utils/format'
import { downloadBlob } from '@/utils/request'

const loading = reactive({ overview: false, trend: false, category: false, funnel: false, hot: false, ai: false, rfm: false, seller: false })
const refreshing = ref(false)
const trendDays = ref(30)

const overview = ref({})
const trend = ref([])
const categories = ref([])
const funnel = ref({})
const hotItems = ref([])
const aiUsage = ref({})
const rfm = ref([])
const sellerRank = ref([])

/** 指标卡从概览接口计算；环比字段契约给了 gmvGrowth / orderGrowth，其余用 trend 自行计算 */
const metricCards = computed(() => {
  const o = overview.value || {}
  return [
    {
      key: 'gmv',
      label: '今日 GMV',
      value: o.todayGmv ?? 0,
      growth: numberOrNull(o.gmvGrowth),
      prefix: '¥',
      desc: `累计 GMV ${formatMoney(o.totalGmv)}`
    },
    {
      key: 'order',
      label: '今日订单',
      value: o.orderCount ?? 0,
      growth: numberOrNull(o.orderGrowth),
      prefix: '',
      desc: `完成 ${o.finishCount ?? 0} / 取消 ${o.cancelCount ?? 0}`
    },
    {
      key: 'user',
      label: '新增用户',
      value: o.newUserCount ?? trend.value[trend.value.length - 1]?.newUserCount ?? 0,
      growth: growthOf('newUserCount'),
      prefix: '',
      desc: `平台用户 ${formatNumber(o.userCount)}`
    },
    {
      key: 'item',
      label: '在售商品',
      value: o.onSaleCount ?? 0,
      growth: growthOf('itemPublishCount'),
      prefix: '',
      desc: `商品总量 ${formatNumber(o.itemCount)} / AI 调用 ${formatNumber(o.aiCallCount)}`
    }
  ]
})

function numberOrNull(value) {
  if (value === null || value === undefined || value === '') return null
  return Number(value)
}

/** 用趋势数组最后一天 vs 前一天计算环比（接口未提供环比字段时的兜底） */
function growthOf(field) {
  const rows = trend.value || []
  if (rows.length < 2) return null
  const last = Number(rows[rows.length - 1]?.[field] ?? 0)
  const prev = Number(rows[rows.length - 2]?.[field] ?? 0)
  if (!prev) return null
  return ((last - prev) / prev) * 100
}

function growthType(value) {
  return Number(value) >= 0 ? 'success' : 'danger'
}

async function loadOverview() {
  loading.overview = true
  try {
    // 字段别名归一在 @/api/analysis 里统一下发，这里直接使用
    overview.value = (await getOverview()) || {}
  } catch (error) {
    overview.value = {}
  } finally {
    loading.overview = false
  }
}

async function loadTrend() {
  loading.trend = true
  try {
    const rows = (await getTrend(trendDays.value)) || []
    trend.value = Array.isArray(rows) ? rows : []
  } catch (error) {
    trend.value = []
  } finally {
    loading.trend = false
  }
}

async function loadCategory() {
  loading.category = true
  try {
    categories.value = (await getCategoryAnalysis()) || []
  } catch (error) {
    categories.value = []
  } finally {
    loading.category = false
  }
}

async function loadFunnel() {
  loading.funnel = true
  try {
    funnel.value = (await getFunnel(30)) || {}
  } catch (error) {
    funnel.value = {}
  } finally {
    loading.funnel = false
  }
}

async function loadHot() {
  loading.hot = true
  try {
    hotItems.value = (await getHotItems(8)) || []
  } catch (error) {
    hotItems.value = []
  } finally {
    loading.hot = false
  }
}

async function loadAi() {
  loading.ai = true
  try {
    aiUsage.value = (await getAiUsage()) || {}
  } catch (error) {
    aiUsage.value = {}
  } finally {
    loading.ai = false
  }
}

async function loadRfm() {
  loading.rfm = true
  try {
    const rows = (await getRfm()) || []
    rfm.value = Array.isArray(rows) ? rows : []
  } catch (error) {
    rfm.value = []
  } finally {
    loading.rfm = false
  }
}

async function loadSeller() {
  loading.seller = true
  try {
    sellerRank.value = (await getSellerRank(10)) || []
  } catch (error) {
    sellerRank.value = []
  } finally {
    loading.seller = false
  }
}

async function handleRefresh() {
  refreshing.value = true
  try {
    await refreshStat()
    ElMessage.success('统计表已重算')
    loadOverview()
    loadTrend()
    loadCategory()
  } catch (error) {
    // 拦截器已提示
  } finally {
    refreshing.value = false
  }
}

/** 手动导出 CSV（契约 /analysis/export 返回 blob） */
async function handleExport() {
  try {
    const blob = await exportStat('gmv', trendDays.value)
    downloadBlob(blob, `gmv-${trendDays.value}d.csv`)
  } catch (error) {
    ElMessage.error('导出失败：' + (error?.message || '接口异常'))
  }
}

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['GMV', '订单数', '新增用户'], right: 10 },
  grid: { left: 56, right: 24, top: 44, bottom: 30 },
  xAxis: { type: 'category', boundaryGap: false, data: trend.value.map((item) => String(item.statDate || '').slice(5)) },
  yAxis: [
    { type: 'value', name: 'GMV', axisLabel: { formatter: (value) => (value >= 10000 ? value / 10000 + 'w' : value) } },
    { type: 'value', name: '数量', splitLine: { show: false } }
  ],
  series: [
    {
      name: 'GMV',
      type: 'line',
      smooth: true,
      yAxisIndex: 0,
      symbolSize: 5,
      areaStyle: { opacity: 0.18 },
      itemStyle: { color: '#5470c6' },
      data: trend.value.map((item) => item.gmv ?? 0)
    },
    {
      name: '订单数',
      type: 'line',
      smooth: true,
      yAxisIndex: 1,
      symbolSize: 4,
      itemStyle: { color: '#91cc75' },
      data: trend.value.map((item) => item.orderCount ?? 0)
    },
    {
      name: '新增用户',
      type: 'line',
      smooth: true,
      yAxisIndex: 1,
      symbolSize: 4,
      itemStyle: { color: '#fac858' },
      data: trend.value.map((item) => item.newUserCount ?? 0)
    }
  ]
}))

const categoryOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>订单 {c} 单（{d}%）' },
  legend: { type: 'scroll', bottom: 0, textStyle: { fontSize: 11 } },
  series: [
    {
      name: '分类成交',
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 },
      data: categories.value.map((item) => ({
        name: item.categoryName || ('分类' + item.categoryId),
        value: item.orderCount ?? item.gmv ?? 0
      }))
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
        left: '10%',
        width: '80%',
        top: 6,
        bottom: 6,
        label: { position: 'inside', formatter: '{b} {c}' },
        itemStyle: { borderColor: '#fff', borderWidth: 1 },
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

const SCENE_LABELS = { copywrite: '文案生成', chat: '智能客服', price: '智能估价', audit: '内容审核', recommend: '个性推荐' }

const aiOption = computed(() => {
  const usage = aiUsage.value || {}
  const byDay = Array.isArray(usage.byDay) ? usage.byDay : []
  const scenes = Array.isArray(usage.scenes) ? usage.scenes : []
  // 后端当前只给「按场景」统计，没有按天；优先按天，缺失时退化为场景分布柱状图
  const byDayMode = byDay.length > 0
  const labels = byDayMode
    ? byDay.map((item) => String(item.date || '').slice(5))
    : scenes.map((item) => SCENE_LABELS[item.scene] || item.scene || '-')
  const values = byDayMode
    ? byDay.map((item) => Number(item.count ?? item.callCount ?? item.requestCount ?? 0))
    : scenes.map((item) => Number(item.callCount ?? 0))
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 24, bottom: 24 },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11, interval: 0 } },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        barWidth: 8,
        itemStyle: {
          borderRadius: [3, 3, 0, 0],
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#7c4dff' }, { offset: 1, color: '#c3a6ff' }] }
        },
        data: values
      }
    ]
  }
})

const rfmOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 60, right: 20, top: 24, bottom: 30 },
  xAxis: { type: 'category', data: rfm.value.map((item) => item.segment || '-'), axisLabel: { interval: 0, fontSize: 11, rotate: rfm.value.length > 5 ? 20 : 0 } },
  yAxis: { type: 'value', name: '人数' },
  series: [
    {
      type: 'bar',
      barWidth: '46%',
      label: { show: true, position: 'top', fontSize: 11 },
      itemStyle: { borderRadius: [4, 4, 0, 0], color: '#36cfc9' },
      data: rfm.value.map((item) => item.userCount ?? 0)
    }
  ]
}))

onMounted(() => {
  loadOverview()
  loadTrend()
  loadCategory()
  loadFunnel()
  loadHot()
  loadAi()
  loadRfm()
  loadSeller()
})
</script>

<style scoped>
.metric-row,
.chart-card,
.chart-row {
  margin-bottom: 12px;
}

.metric-card {
  border: none;
  border-radius: 10px;
  background: linear-gradient(180deg, #f7faff 0%, #ffffff 70%);
}

.metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.metric-label {
  color: #8a94a6;
  font-size: 13px;
}

.metric-value {
  margin: 10px 0 4px;
  font-size: 28px;
  font-weight: 700;
  color: #1f2d3d;
  letter-spacing: 0.5px;
}

.metric-desc {
  color: #a3adc2;
  font-size: 12px;
}

.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2d3d;
}

.chart-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sub {
  color: #a3adc2;
  font-size: 12px;
}

.money {
  color: #f56c6c;
}

.el-row + .el-row {
  margin-top: 0;
}
</style>
