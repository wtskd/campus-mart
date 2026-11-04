<template>
  <!-- 字典标签：根据 dictType + value 渲染彩色 tag -->
  <el-tag v-if="label" :type="tagType" size="small" effect="light">{{ label }}</el-tag>
  <span v-else>{{ value }}</span>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { loadDict } from '@/utils/dict'

const props = defineProps({
  dictType: { type: String, required: true },
  value: { type: [String, Number], default: '' }
})

const options = ref([])

const label = computed(() => {
  const item = options.value.find((opt) => String(opt.dictValue) === String(props.value))
  return item ? item.dictLabel : ''
})

const tagType = computed(() => {
  const item = options.value.find((opt) => String(opt.dictValue) === String(props.value))
  const listClass = item?.listClass
  const map = {
    primary: 'primary',
    success: 'success',
    info: 'info',
    warning: 'warning',
    danger: 'danger'
  }
  return map[listClass] || 'info'
})

async function loadOptions() {
  options.value = (await loadDict(props.dictType)) || []
}

onMounted(loadOptions)
</script>
