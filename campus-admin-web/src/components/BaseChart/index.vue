<template>
  <!-- ECharts 通用容器：负责初始化、自适应、销毁 -->
  <div ref="chartRef" class="base-chart" :style="{ height, width }"></div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, default: () => ({}) },
  height: { type: String, default: '320px' },
  width: { type: String, default: '100%' }
})

const chartRef = ref(null)
let chart = null
let observer = null

function render() {
  if (!chart) return
  // notMerge=true：切换维度时避免残留旧 series
  chart.setOption(props.option || {}, true)
}

function resize() {
  chart?.resize()
}

onMounted(async () => {
  await nextTick()
  chart = echarts.init(chartRef.value)
  render()
  // 监听容器尺寸变化（侧边栏折叠、窗口缩放、大屏适配都能自适应）
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(resize)
    observer.observe(chartRef.value)
  }
  window.addEventListener('resize', resize)
})

watch(
  () => props.option,
  () => render(),
  { deep: true }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  observer?.disconnect()
  chart?.dispose()
  chart = null
})

defineExpose({ resize, getInstance: () => chart })
</script>

<style scoped>
.base-chart {
  min-height: 120px;
}
</style>
