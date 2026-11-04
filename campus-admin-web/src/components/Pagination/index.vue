<template>
  <!-- 通用分页：current / size 与接口契约保持一致 -->
  <div v-show="total > 0" class="pagination-container">
    <el-pagination
      v-model:current-page="current"
      v-model:page-size="limit"
      :background="true"
      :page-sizes="pageSizes"
      :layout="layout"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  limit: { type: Number, default: 10 },
  pageSizes: { type: Array, default: () => [10, 20, 50, 100] },
  layout: { type: String, default: 'total, sizes, prev, pager, next, jumper' }
})

const emit = defineEmits(['update:page', 'update:limit', 'pagination'])

const current = computed({
  get: () => props.page,
  set: (value) => emit('update:page', value)
})

const limit = computed({
  get: () => props.limit,
  set: (value) => emit('update:limit', value)
})

function handleSizeChange(size) {
  emit('pagination', { page: 1, limit: size })
}

function handleCurrentChange(page) {
  emit('pagination', { page, limit: limit.value })
}
</script>

<style scoped>
.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}
</style>
