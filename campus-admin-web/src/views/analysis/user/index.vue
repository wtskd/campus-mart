<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <span class="filter-label">留存窗口</span>
        <el-radio-group v-model="days" @change="loadRetention">
          <el-radio-button :value="7">7 天</el-radio-button>
          <el-radio-button :value="14">14 天</el-radio-button>
          <el-radio-button :value="30">30 天</el-radio-button>
        </el-radio-group>
        <span class="tip">RFM 分层来自 stat_user_rfm，对应 用户分层 / 运营触达 两类运营动作</span>
      </div>
    </el-card>

    <el-row :gutter="12" class="card-row">
      <el-col :span="6">
        <el-card shadow="never" class="kpi-card" v-loading="loading.rfm">
          <div class="kpi-label">分层用户总量</div>
          <div class="kpi-value">{{ formatNumber(rfmTotal) }}</div>
          <div class="kpi-desc">共 {{ rfm.length }} 个分层</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="kpi-card" v-loading="loading.rfm">
          <div class="kpi-label">重要价值用户</div>
          <div class="kpi-value">{{ formatNumber(segmentCount('重要价值')) }}</div>
          <div class="kpi-desc">高频高客单，重点维护</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="kpi-card" v-loading="loading.retention">
          <div class="kpi-label">平均留存率</div>
          <div class="kpi-value">{{ avgRetention }}%</div>
          <div class="kpi-desc">近 {{ days }} 天加权平均</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="kpi-card" v-loading="loading.trend">
          <div class="kpi-label">新增用户（{{ days }} 天）</div>
          <div class="kpi-value">{{ formatNumber(newUserSum) }}</div>
          <div class="kpi-desc">日均 {{ formatNumber(Math.round(newUserSum / days)) }} 人</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :span="10">
        <el-card shadow="never" v-loading="loading.rfm">
          <div class="chart-head"><span class="chart-title">RFM 用户分层</span></div>
          <BaseChart :option="rfmOption" height="330px" />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never" v-loading="loading.retention">
          <div class="chart-head"><span class="chart-title">活跃与留存</span><span class="sub">留存率 = 当日活跃且非新增 / 历史活跃</span></div>
          <BaseChart :option="retentionOption" height="330px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :span="24">
        <el-card shadow="never" v-loading="loading.trend">
          <div class="chart-head"><span class="chart-title">新增用户趋势</span></div>
          <BaseChart :option="newUserOption" height="260px" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" v-loading="loading.rfm">
      <div class="chart-head"><span class="chart-title">分层明细</span></div>
      <el-table :data="rfm" border size="small">
        <el-table-column prop="segment" label="用户分层" />
        <el-table-column label="用户数" align="right">
          <template #default="{ row }">{{ formatNumber(row.userCount) }}</template>
        </el-table-column>
        <el-table-column label="占比" align="right">
          <template #default="{ row }">{{ ratioText(row) }}</template>
        </el-table-column>
        <el-table-column label="平均消费金额" align="right">
          <template #default="{ row }">{{ formatMoney(row.avgMonetary) }}</template>
        </el-table-column>
        <el-table-column label="运营建议">
          <template #default="{ row }">{{ SUGGEST_MAP[row.segment] || '保持常规运营' }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import BaseChart from '@/components/BaseChart/index.vue'
import { getRetention, getRfm, getTrend } from '@/api/analysis'
import { formatMoney, formatNumber } from '@/utils/format'

const SUGGEST_MAP = {
  重要价值: '专属客服 + 交易优先展示',
  重要保持: '差异化权益，防止流失',
  重要发展: '提升交易频次（优惠券/活动）',
  重要挽留: '召回短信 + 高价值商品推荐',
  一般价值: '常规运营，低成本触达',
  流失: '召回活动 / 减少营销投入'
}

const days = ref(7)
const loading = reactive({ rfm: false, retention: false, trend: false })
const rfm = ref([])
const retention = ref([])
const trend = ref([])

const rfmTotal = computed(() => rfm.value.reduce((sum, row) => sum + Number(row.userCount || 0), 0))
const avgRetention = computed(() => {
  // 末尾「今天」的留存率是 null（次日数据还没产生），求平均时要跳过
  const valid = retention.value.filter((row) => row.retentionRate !== null && row.retentionRate !== undefined)
  if (!valid.length) return '0.00'
  const sum = valid.reduce((acc, row) => acc + Number(row.retentionRate || 0), 0)
  return (sum / valid.length).toFixed(2)
})
const newUserSum = computed(() => trend.value.reduce((sum, row) => sum + Number(row.newUserCount || 0), 0))

function segmentCount(name) {
  // 后端分层名是「重要价值客户 / 重要保持客户 …」，用前缀匹配，避免少写「客户」两字永远取到 0
  const row = rfm.value.find((item) => String(item.segment || '').startsWith(name))
  return Number(row?.userCount ?? 0)
}

function ratioText(row) {
  if (row.userRatio !== null && row.userRatio !== undefined) {
    const ratio = Number(row.userRatio)
    return `${(ratio <= 1 ? ratio * 100 : ratio).toFixed(2)}%`
  }
  return rfmTotal.value ? `${((Number(row.userCount || 0) / rfmTotal.value) * 100).toFixed(2)}%` : '-'
}

async function loadRfm() {
  loading.rfm = true
  try {
    rfm.value = (await getRfm()) || []
  } catch (error) {
    rfm.value = []
  } finally {
    loading.rfm = false
  }
}

async function loadRetention() {
  loading.retention = true
  try {
    retention.value = (await getRetention(days.value)) || []
  } catch (error) {
    retention.value = []
  } finally {
    loading.retention = false
  }
}

async function loadTrend() {
  loading.trend = true
  try {
    trend.value = (await getTrend(days.value)) || []
  } catch (error) {
    trend.value = []
  } finally {
    loading.trend = false
  }
}

const rfmOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: (params) => `${params.name}<br/>用户数：${params.value} 人（${params.percent}%）`
  },
  // 名字很长（「重要价值客户」等 9 个分层），放右侧纵向图例，避免底部横向图例挤成 1/2 分页、
  // 饼图外圈标签互相压字；图例里去掉「客户」两个字，读起来更清爽
  legend: {
    type: 'scroll',
    orient: 'vertical',
    right: 6,
    top: 'middle',
    itemWidth: 9,
    itemHeight: 9,
    itemGap: 7,
    textStyle: { fontSize: 11, color: '#606266' },
    formatter: (name) => String(name).replace('客户', '')
  },
  title: {
    text: String(rfmTotal.value),
    subtext: '分层用户',
    left: '34%',
    top: '38%',
    textAlign: 'center',
    textStyle: { fontSize: 24, fontWeight: 600, color: '#303133' },
    subtextStyle: { fontSize: 11, color: '#909399' }
  },
  series: [
    {
      type: 'pie',
      radius: ['48%', '68%'],
      center: ['34%', '50%'],
      // 分到的人数可能为 0（该分层无人），给个最小角度避免画成一条细线；标签默认不显示，hover 时再出现
      minAngle: 3,
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      labelLine: { show: false },
      emphasis: {
        scale: true,
        scaleSize: 6,
        label: {
          show: true,
          fontSize: 12,
          fontWeight: 600,
          formatter: '{b}\n{c} 人 · {d}%'
        }
      },
      data: rfm.value.map((item) => ({ name: item.segment || '-', value: Number(item.userCount || 0) }))
    }
  ]
}))

const retentionOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['活跃用户', '新增用户', '留存率'], right: 10 },
  grid: { left: 52, right: 52, top: 44, bottom: 30 },
  xAxis: { type: 'category', data: retention.value.map((item) => String(item.date || '').slice(5)) },
  yAxis: [
    { type: 'value', name: '人数' },
    { type: 'value', name: '留存率(%)', max: 100, splitLine: { show: false } }
  ],
  series: [
    { name: '活跃用户', type: 'bar', barWidth: 10, itemStyle: { color: '#5b8ff9' }, data: retention.value.map((item) => item.activeUser ?? 0) },
    { name: '新增用户', type: 'bar', barWidth: 10, itemStyle: { color: '#5ad8a6' }, data: retention.value.map((item) => item.newUser ?? 0) },
    {
      name: '留存率',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      itemStyle: { color: '#f6bd16' },
      data: retention.value.map((item) => (item.retentionRate == null ? null : Number(item.retentionRate)))
    }
  ]
}))

const newUserOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 52, right: 24, top: 20, bottom: 30 },
  xAxis: { type: 'category', boundaryGap: false, data: trend.value.map((item) => String(item.statDate || '').slice(5)) },
  yAxis: { type: 'value', name: '新增用户' },
  series: [
    {
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.24 },
      itemStyle: { color: '#ff9845' },
      data: trend.value.map((item) => item.newUserCount ?? 0)
    }
  ]
}))

onMounted(() => {
  loadRfm()
  loadRetention()
  loadTrend()
})
</script>

<style scoped>
.filter-label {
  margin-right: 8px;
  color: #606266;
}

.tip {
  margin-left: 12px;
  color: #a3adc2;
  font-size: 12px;
}

.card-row,
.el-row {
  margin-bottom: 12px;
}

.kpi-card {
  border: none;
  border-radius: 10px;
  background: linear-gradient(180deg, #f7faff 0%, #ffffff 70%);
}

.kpi-label {
  color: #8a94a6;
  font-size: 13px;
}

.kpi-value {
  margin: 8px 0 2px;
  font-size: 26px;
  font-weight: 700;
  color: #1f2d3d;
}

.kpi-desc {
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

.sub {
  color: #a3adc2;
  font-size: 12px;
}

.page-card {
  margin-bottom: 12px;
}
</style>
