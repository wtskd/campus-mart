<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input
          v-model="query.module"
          placeholder="操作模块"
          clearable
          style="width: 170px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.operator"
          placeholder="操作人"
          clearable
          style="width: 150px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" placeholder="操作状态" clearable style="width: 140px">
          <el-option label="成功" :value="0" />
          <el-option label="失败" :value="1" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">操作日志</span>
        <span class="tip">记录后台关键操作，便于答辩演示「可观测性」</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="operId" label="日志编号" width="90" align="center" />
        <el-table-column prop="module" label="操作模块" width="120" />
        <el-table-column prop="operType" label="操作类型" width="100" align="center" />
        <el-table-column prop="operator" label="操作人" width="110" />
        <el-table-column prop="deptName" label="部门" width="140" show-overflow-tooltip />
        <el-table-column prop="operUrl" label="请求地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operIp" label="IP" width="130" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">
              {{ row.status === 1 ? '失败' : '成功' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时(ms)" width="100" align="center" />
        <el-table-column prop="operTime" label="操作时间" width="170" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        v-model:page="query.current"
        v-model:limit="query.size"
        :total="total"
        @pagination="handlePagination"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="操作日志详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="操作模块">{{ detail.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ detail.operType }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.operator }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detail.deptName }}</el-descriptions-item>
        <el-descriptions-item label="请求地址">{{ detail.operUrl }}</el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ detail.operIp }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ detail.status === 1 ? '失败' : '成功' }}
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costMs }} ms</el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ detail.operTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="错误信息" :span="2">
          <span class="error-text">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { useTable } from '@/composables/useTable'
import { getOperLogPage } from '@/api/system'

const { loading, list, total, query, getList, handleSearch, resetQuery, handlePagination } = useTable(
  getOperLogPage,
  { module: '', operator: '', status: '' }
)

const detailVisible = ref(false)
const detail = ref({})

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
}

.tip {
  color: #909399;
  font-size: 12px;
}

.error-text {
  color: #f56c6c;
}
</style>
