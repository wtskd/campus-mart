<template>
  <!-- 字典下拉：按 dictType 拉取字典数据，内置本地缓存 -->
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :style="{ width }"
    :disabled="disabled"
    @update:model-value="onChange"
  >
    <el-option
      v-for="item in options"
      :key="item.dictValue"
      :label="item.dictLabel"
      :value="item.dictValue"
    />
  </el-select>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { loadDict } from '@/utils/dict'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  dictType: { type: String, required: true },
  placeholder: { type: String, default: '请选择' },
  width: { type: String, default: '100%' },
  clearable: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'change'])
const options = ref([])

async function loadOptions() {
  if (!props.dictType) return
  // 通过 utils/dict 统一缓存加载，多组件共享同一份 Promise
  const data = await loadDict(props.dictType)
  options.value = (data || []).filter((item) => item.status !== 1)
}

function onChange(value) {
  emit('update:modelValue', value)
  emit('change', value)
}

onMounted(loadOptions)
watch(() => props.dictType, loadOptions)
</script>
