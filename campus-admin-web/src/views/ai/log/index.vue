<template>
  <div class="page-container">
    <!-- 消耗统计 -->
    <el-row :gutter="12" class="stat-row">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">总调用次数</div>
          <div class="stat-value">{{ stat.totalCalls ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">成功调用</div>
          <div class="stat-value success">{{ stat.successCalls ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">平均耗时</div>
          <div class="stat-value">{{ stat.avgCostMs ?? '-' }} <span class="unit">ms</span></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">累计 Tokens</div>
          <div class="stat-value warning">{{ formatNumber(stat.totalTokens) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="chart-row">
      <el-col :span="12">
        <el-card shadow="never">
          <div class="card-title">各场景调用次数</div>
          <BaseChart :option="sceneChartOption" height="240px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <div class="card-title">近 15 天调用趋势</div>
          <BaseChart :option="dayChartOption" height="240px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 日志明细 -->
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-select v-model="query.scene" placeholder="场景" clearable style="width: 130px">
          <el-option v-for="(label, value) in SCENE_MAP" :key="value" :label="label" :value="value" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
          <el-option label="成功" value="0" />
          <el-option label="失败" value="1" />
          <el-option label="降级兜底" value="2" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 360px"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">AI 调用日志（provider / 耗时 / 降级标记）</span>
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column label="场景" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ SCENE_MAP[row.scene] || row.scene }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="provider" label="provider" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="providerTag(row.provider)">{{ row.provider }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="模型" width="140" show-overflow-tooltip />
        <el-table-column label="Tokens" width="110" align="right">
          <template #default="{ row }">{{ formatNumber(row.totalTokens) }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">
            <span :class="{ slow: Number(row.costMs) > 3000 }">{{ row.costMs }}ms</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestSummary" label="请求摘要" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="调用时间" width="170" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        v-model:page="query.current"
        v-model:limit="query.size"
        :total="total"
        @pagination="loadList"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="调用详情" width="640px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="日志 ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="用户 ID">{{ detail.userId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="场景">{{ SCENE_MAP[detail.scene] || detail.scene }}</el-descriptions-item>
        <el-descriptions-item label="模板">{{ detail.templateCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="provider">{{ detail.provider }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ detail.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="prompt tokens">{{ detail.promptTokens }}</el-descriptions-item>
        <el-descriptions-item label="completion tokens">{{ detail.completionTokens }}</el-descriptions-item>
        <el-descriptions-item label="总耗时">{{ detail.costMs }}ms</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="调用时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="请求摘要" :span="2">{{ detail.requestSummary || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="错误信息" :span="2">
          <span class="error-text">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import BaseChart from '@/components/BaseChart/index.vue'
import { getAiLogPage, getAiStatOverview } from '@/api/ai'

const SCENE_MAP = {
  copywrite: '商品文案',
  chat: '智能客服',
  audit: '内容审核',
  price: '智能估价'
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref([])
const query = reactive({ current: 1, size: 10, scene: '', status: '', beginTime: undefined, endTime: undefined })

const stat = ref({})
const sceneStat = ref([])
const dayStat = ref([])

async function loadStat() {
  try {
    const data = await getAiStatOverview()
    stat.value = data || {}
    sceneStat.value = data?.byScene || []
    dayStat.value = data?.byDay || []
  } catch (error) {
    stat.value = {}
  }
}

async function loadList() {
  loading.value = true
  try {
    const page = await getAiLogPage({ ...query })
    list.value = page.records || []
    total.value = page.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function loadAll() {
  loadStat()
  loadList()
}

function handleSearch() {
  query.current = 1
  query.beginTime = dateRange.value?.[0]
  query.endTime = dateRange.value?.[1]
  loadList()
}

function handleReset() {
  dateRange.value = []
  Object.assign(query, { current: 1, size: 10, scene: '', status: '', beginTime: undefined, endTime: undefined })
  loadList()
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString()
}

function providerTag(provider) {
  return { remote: 'primary', local: 'success', mock: 'warning', rule: 'info' }[provider] || 'info'
}

function statusTag(status) {
  return { '0': 'success', '1': 'danger', '2': 'warning' }[String(status)] || 'info'
}

function statusText(status) {
  return { '0': '成功', '1': '失败', '2': '降级兜底' }[String(status)] || status
}

// 场景调用次数柱状图
const sceneChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 30, bottom: 30 },
  xAxis: {
    type: 'category',
    data: sceneStat.value.map((item) => SCENE_MAP[item.scene] || item.scene)
  },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'bar',
      barWidth: 28,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: '#5b8ff9' },
            { offset: 1, color: '#9ecbff' }
          ]
        }
      },
      data: sceneStat.value.map((item) => item.count ?? item.requestCount ?? 0)
    }
  ]
}))

// 近 15 天趋势折线图
const dayChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 30, bottom: 30 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: dayStat.value.map((item) => String(item.date || '').slice(5))
  },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'line',
      smooth: true,
      symbolSize: 6,
      areaStyle: { opacity: 0.18 },
      lineStyle: { width: 2 },
      data: dayStat.value.map((item) => item.count ?? item.requestCount ?? 0)
    }
  ]
}))

// 详情弹窗
const detailVisible = ref(false)
const detail = ref({})

function showDetail(row) {
  detail.value = { ...row }
  detailVisible.value = true
}

onMounted(loadAll)
</script>

<style scoped>
.stat-row,
.chart-row {
  margin-bottom: 12px;
}

.stat-card {
  text-align: left;
}

.stat-label {
  color: #909399;
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 650;
  color: #303133;
}

.stat-value.success {
  color: #3f8600;
}

.stat-value.warning {
  color: #d48806;
}

.unit {
  font-size: 13px;
  color: #909399;
}

.card-title {
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
}

.title {
  font-size: 15px;
  font-weight: 600;
}

.slow {
  color: #e6a23c;
}

.error-text {
  color: #f56c6c;
}
</style>
