<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <span class="filter-label">数据范围</span>
        <el-radio-group v-model="days" @change="loadAll">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
        <el-button type="primary" :icon="Download" :loading="exporting" @click="handleExport">导出 CSV</el-button>
        <span class="tip">数据来源：stat_* 预聚合表，每日 02:00 自动重算，可在首页手动「重算统计」</span>
      </div>
    </el-card>

    <el-row :gutter="12">
      <el-col :span="16">
        <el-card shadow="never" v-loading="loading.trend">
          <div class="chart-head">
            <span class="chart-title">GMV / 订单趋势</span>
            <span class="sub">成交口径：order_status = 2（已完成）</span>
          </div>
          <BaseChart :option="trendOption" height="320px" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" v-loading="loading.category">
          <div class="chart-head"><span class="chart-title">分类成交占比</span></div>
          <BaseChart :option="categoryOption" height="320px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :span="12">
        <el-card shadow="never" v-loading="loading.hot">
          <div class="chart-head"><span class="chart-title">热门商品 Top 10</span></div>
          <el-table :data="hotItems" size="small">
            <el-table-column type="index" label="#" width="46" />
            <el-table-column prop="title" label="商品" show-overflow-tooltip />
            <el-table-column prop="viewCount" label="浏览" width="80" align="right" />
            <el-table-column prop="favoriteCount" label="收藏" width="80" align="right" />
            <el-table-column label="成交" width="90" align="right">
              <template #default="{ row }">{{ formatNumber(row.orderCount) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" v-loading="loading.seller">
          <div class="chart-head"><span class="chart-title">卖家成交排行</span></div>
          <el-table :data="sellerRank" size="small">
            <el-table-column label="排名" width="60">
              <template #default="{ $index }">
                <span class="rank" :class="'rank-' + ($index + 1)">{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="卖家">
              <template #default="{ row }">{{ row.nickname || row.sellerName || ('用户 ' + row.userId) }}</template>
            </el-table-column>
            <el-table-column prop="orderCount" label="成交单" width="90" align="right" />
            <el-table-column label="成交额" width="120" align="right">
              <template #default="{ row }"><span class="money">{{ formatMoney(row.gmv ?? row.amount) }}</span></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :span="12">
        <el-card shadow="never" v-loading="loading.ai">
          <div class="chart-head"><span class="chart-title">AI 调用场景分布</span></div>
          <BaseChart :option="aiSceneOption" height="280px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" v-loading="loading.category">
          <div class="chart-head"><span class="chart-title">分类 GMV 排名</span></div>
          <BaseChart :option="categoryBarOption" height="280px" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import BaseChart from '@/components/BaseChart/index.vue'
import {
  exportStat,
  getAiUsage,
  getCategoryAnalysis,
  getHotItems,
  getSellerRank,
  getTrend
} from '@/api/analysis'
import { downloadBlob } from '@/utils/request'
import { formatMoney, formatNumber } from '@/utils/format'

const days = ref(30)
const exporting = ref(false)
const loading = reactive({ trend: false, category: false, hot: false, seller: false, ai: false })
const trend = ref([])
const categories = ref([])
const hotItems = ref([])
const sellerRank = ref([])
const aiUsage = ref({})

async function loadAll() {
  await Promise.all([loadTrend(), loadCategory(), loadHot(), loadSeller(), loadAi()])
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

async function loadHot() {
  loading.hot = true
  try {
    hotItems.value = (await getHotItems(10)) || []
  } catch (error) {
    hotItems.value = []
  } finally {
    loading.hot = false
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

async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportStat('gmv', days.value)
    downloadBlob(blob, `gmv-${days.value}d.csv`)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败：' + (error?.message || '接口异常'))
  } finally {
    exporting.value = false
  }
}

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['GMV', '订单数', '完成订单'], right: 10 },
  grid: { left: 56, right: 46, top: 44, bottom: 30 },
  xAxis: { type: 'category', boundaryGap: false, data: trend.value.map((item) => String(item.statDate || '').slice(5)) },
  yAxis: [
    { type: 'value', name: 'GMV' },
    { type: 'value', name: '订单', splitLine: { show: false } }
  ],
  series: [
    {
      name: 'GMV',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.2 },
      itemStyle: { color: '#5470c6' },
      data: trend.value.map((item) => item.gmv ?? 0)
    },
    {
      name: '订单数',
      type: 'line',
      smooth: true,
      yAxisIndex: 1,
      itemStyle: { color: '#91cc75' },
      data: trend.value.map((item) => item.orderCount ?? 0)
    },
    {
      name: '完成订单',
      type: 'line',
      smooth: true,
      yAxisIndex: 1,
      itemStyle: { color: '#fac858' },
      data: trend.value.map((item) => item.finishOrderCount ?? 0)
    }
  ]
}))

const categoryOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>GMV ¥{c}' },
  legend: { type: 'scroll', bottom: 0, textStyle: { fontSize: 11 } },
  series: [
    {
      type: 'pie',
      radius: ['40%', '66%'],
      center: ['50%', '44%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{d}%', fontSize: 11 },
      data: categories.value.map((item) => ({
        name: item.categoryName || ('分类' + item.categoryId),
        value: item.gmv ?? item.orderCount ?? 0
      }))
    }
  ]
}))

const categoryBarOption = computed(() => {
  const rows = [...categories.value].sort((a, b) => Number(b.gmv || 0) - Number(a.gmv || 0)).slice(0, 8)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 92, right: 30, top: 16, bottom: 24 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: rows.map((item) => item.categoryName || '-'), axisLabel: { fontSize: 11 } },
    series: [
      {
        type: 'bar',
        barWidth: 12,
        itemStyle: { borderRadius: [0, 6, 6, 0], color: '#5b8ff9' },
        data: rows.map((item) => item.gmv ?? 0)
      }
    ]
  }
})

const aiSceneOption = computed(() => {
  const byScene = (aiUsage.value || {}).byScene || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 46, right: 20, top: 24, bottom: 30 },
    xAxis: { type: 'category', data: byScene.map((item) => item.scene || '-') },
    yAxis: { type: 'value', name: '调用次数' },
    series: [
      {
        type: 'bar',
        barWidth: '42%',
        itemStyle: { borderRadius: [4, 4, 0, 0], color: '#7c4dff' },
        data: byScene.map((item) => item.count ?? item.totalCalls ?? 0)
      }
    ]
  }
})

onMounted(loadAll)
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

.money {
  color: #f56c6c;
}

.rank {
  display: inline-block;
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  border-radius: 50%;
  background: #f0f2f5;
  color: #909399;
  font-size: 12px;
}

.rank-1 {
  background: #fef0f0;
  color: #f56c6c;
  font-weight: 700;
}

.rank-2 {
  background: #fff7e6;
  color: #e6a23c;
  font-weight: 700;
}

.rank-3 {
  background: #f0f9eb;
  color: #67c23a;
  font-weight: 700;
}

.page-card {
  margin-bottom: 12px;
}
</style>
