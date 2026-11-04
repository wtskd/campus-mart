<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <span class="filter-label">统计周期</span>
        <el-radio-group v-model="days" @change="loadFunnel">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
        <span class="tip">漏斗来自 stat_funnel_daily；口径：浏览 → 收藏 → 下单 → 成交（status=2）</span>
      </div>
    </el-card>

    <el-row :gutter="12" class="card-row">
      <el-col :span="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">浏览量</div>
          <div class="kpi-value">{{ formatNumber(funnel.viewCount) }}</div>
          <div class="kpi-desc">商品详情页 PV 去重</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">浏览 → 下单转化率</div>
          <div class="kpi-value">{{ rate(funnel.viewToOrderRate) }}%</div>
          <div class="kpi-desc">下单 / 浏览</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">下单 → 成交转化率</div>
          <div class="kpi-value">{{ rate(funnel.orderToFinishRate) }}%</div>
          <div class="kpi-desc">成交 / 下单</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">整体成交转化率</div>
          <div class="kpi-value">{{ overallRate }}%</div>
          <div class="kpi-desc">成交 / 浏览</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :span="14">
        <el-card shadow="never" v-loading="loading">
          <div class="chart-head"><span class="chart-title">转化漏斗</span><span class="sub">近 {{ days }} 天</span></div>
          <BaseChart :option="funnelOption" height="380px" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" v-loading="loading">
          <div class="chart-head"><span class="chart-title">各环节流失</span></div>
          <BaseChart :option="lossOption" height="380px" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" v-loading="loading">
      <div class="chart-head"><span class="chart-title">环节拆解</span></div>
      <el-table :data="stages" border size="small">
        <el-table-column prop="name" label="环节" width="140" />
        <el-table-column label="人数/次数" align="right">
          <template #default="{ row }">{{ formatNumber(row.value) }}</template>
        </el-table-column>
        <el-table-column label="环节转化率" align="right">
          <template #default="{ row }">{{ row.rate }}%</template>
        </el-table-column>
        <el-table-column label="相对首环节" align="right">
          <template #default="{ row }">{{ row.relative }}%</template>
        </el-table-column>
        <el-table-column label="流失人数" align="right">
          <template #default="{ row }">{{ formatNumber(row.loss) }}</template>
        </el-table-column>
        <el-table-column label="说明">
          <template #default="{ row }">{{ row.desc }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import BaseChart from '@/components/BaseChart/index.vue'
import { getFunnel } from '@/api/analysis'
import { formatNumber } from '@/utils/format'

const days = ref(30)
const loading = ref(false)
const funnel = ref({})

const stages = computed(() => {
  const f = funnel.value || {}
  const raw = [
    { name: '浏览', value: Number(f.viewCount || 0), desc: '商品详情页访问' },
    { name: '收藏', value: Number(f.favoriteCount || 0), desc: '加入收藏' },
    { name: '下单', value: Number(f.orderCount || 0), desc: '创建订单' },
    { name: '成交', value: Number(f.finishCount || 0), desc: '订单完成（status=2）' }
  ]
  const first = raw[0]?.value || 0
  return raw.map((stage, index) => {
    const prev = index === 0 ? stage.value : raw[index - 1].value
    return {
      ...stage,
      rate: prev ? ((stage.value / prev) * 100).toFixed(2) : '0.00',
      relative: first ? ((stage.value / first) * 100).toFixed(2) : '0.00',
      loss: index === 0 ? 0 : Math.max(prev - stage.value, 0)
    }
  })
})

const overallRate = computed(() => {
  const view = Number(funnel.value.viewCount || 0)
  const finish = Number(funnel.value.finishCount || 0)
  return view ? ((finish / view) * 100).toFixed(2) : '0.00'
})

function rate(value) {
  return Number(value || 0).toFixed(2)
}

async function loadFunnel() {
  loading.value = true
  try {
    funnel.value = (await getFunnel(days.value)) || {}
  } catch (error) {
    funnel.value = {}
  } finally {
    loading.value = false
  }
}

const funnelOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c}' },
  series: [
    {
      type: 'funnel',
      left: '8%',
      width: '84%',
      top: 10,
      bottom: 10,
      minSize: '18%',
      sort: 'descending',
      gap: 4,
      label: { show: true, position: 'inside', formatter: '{b} {c}' },
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      emphasis: { label: { fontSize: 16, fontWeight: 'bold' } },
      data: stages.value.map((stage, index) => ({
        name: stage.name,
        value: stage.value,
        itemStyle: { color: ['#5b8ff9', '#61ddaa', '#f6bd16', '#ff6b6b'][index] }
      }))
    }
  ]
}))

const lossOption = computed(() => {
  const rows = stages.value.slice(1)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 72, right: 40, top: 24, bottom: 24 },
    xAxis: { type: 'value', name: '流失人数' },
    yAxis: { type: 'category', data: rows.map((row) => row.name), axisLabel: { fontSize: 12 } },
    series: [
      {
        type: 'bar',
        barWidth: 16,
        label: { show: true, position: 'right', formatter: '{c}' },
        itemStyle: { borderRadius: [0, 8, 8, 0], color: '#ff9c6e' },
        data: rows.map((row) => row.loss)
      }
    ]
  }
})

onMounted(loadFunnel)
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
