<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input
          v-model="query.title"
          placeholder="操作模块"
          clearable
          style="width: 170px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.operName"
          placeholder="操作人"
          clearable
          style="width: 150px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.businessType" placeholder="操作类型" clearable style="width: 140px">
          <el-option
            v-for="item in businessTypes"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="query.status" placeholder="操作状态" clearable style="width: 140px">
          <el-option label="成功" value="0" />
          <el-option label="失败" value="1" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <div>
          <span class="title">操作日志</span>
          <span class="tip">记录后台关键操作，便于答辩演示「可观测性」</span>
        </div>
        <div>
          <el-button size="small" type="danger" :icon="Delete" :disabled="!selection.length" @click="handleDelete">
            删除
          </el-button>
          <el-button size="small" type="warning" :icon="Delete" @click="handleClean">清空</el-button>
          <el-button size="small" :icon="Refresh" @click="getList">刷新</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column prop="operId" label="日志编号" width="90" align="center" />
        <el-table-column prop="title" label="操作模块" width="130" show-overflow-tooltip />
        <el-table-column label="操作类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="businessTypeTag(row.businessType)" size="small">
              {{ businessTypeText(row.businessType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operName" label="操作人" width="110" />
        <el-table-column prop="deptName" label="部门" width="140" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="请求方式" width="100" align="center" />
        <el-table-column prop="operUrl" label="请求地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operIp" label="IP" width="140" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="String(row.status) === '1' ? 'danger' : 'success'" size="small">
              {{ String(row.status) === '1' ? '失败' : '成功' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="100" align="center" />
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

    <el-dialog v-model="detailVisible" title="操作日志详情" width="720px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="操作模块">{{ detail.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ businessTypeText(detail.businessType) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.operName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detail.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detail.requestMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ detail.operIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求地址" :span="2">{{ detail.operUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="后端方法" :span="2">{{ detail.method || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ String(detail.status) === '1' ? '失败' : '成功' }}
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costTime ?? '-' }} ms</el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ detail.operTime || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.operParam" label="请求参数" :span="2">
          <pre class="code-block">{{ detail.operParam }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.jsonResult" label="返回结果" :span="2">
          <pre class="code-block">{{ detail.jsonResult }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="错误信息" :span="2">
          <span class="error-text">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Delete, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination/index.vue'
import { useTable } from '@/composables/useTable'
import { cleanOperLog, deleteOperLogs, getOperLogPage } from '@/api/system'

/**
 * 字段对照（后端 SysOperLog 实体）：
 * title=操作模块、businessType=操作类型、operName=操作人、costTime=耗时
 * 之前页面用了 module/operType/operator/costMs 这些不存在的字段，表格全空、筛选也不生效。
 */
const businessTypes = [
  { value: '1', label: '新增' },
  { value: '2', label: '修改' },
  { value: '3', label: '删除' },
  { value: '4', label: '查询' },
  { value: '5', label: '导出' },
  { value: '9', label: '清空' }
]

const { loading, list, total, query, getList, handleSearch, resetQuery, handlePagination } = useTable(
  getOperLogPage,
  { title: '', operName: '', businessType: '', status: '' }
)

function businessTypeText(value) {
  const key = String(value ?? '')
  return businessTypes.find((item) => item.value === key)?.label || (key ? `类型 ${key}` : '-')
}

function businessTypeTag(value) {
  return { 1: 'success', 2: 'primary', 3: 'danger', 4: 'info', 5: 'warning', 9: 'warning' }[String(value)] || 'info'
}

const detailVisible = ref(false)
const detail = ref({})

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

// ---------------- 删除 / 清空 ----------------
const selection = ref([])

function handleSelectionChange(rows) {
  selection.value = rows
}

async function handleDelete() {
  if (!selection.value.length) return
  const ids = selection.value.map((row) => row.operId).join(',')
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selection.value.length} 条日志？`, '提示', { type: 'warning' })
    await deleteOperLogs(ids)
    ElMessage.success('删除成功')
    getList()
  } catch (error) {
    // 取消或不通过时静默
  }
}

async function handleClean() {
  try {
    await ElMessageBox.confirm('确认清空全部操作日志？该操作不可恢复。', '提示', { type: 'warning' })
    await cleanOperLog()
    ElMessage.success('清空成功')
    query.current = 1
    getList()
  } catch (error) {
    // 取消时静默
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
}

.title {
  font-weight: 600;
}

.tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.code-block {
  margin: 0;
  max-height: 180px;
  overflow: auto;
  padding: 8px 10px;
  background: #f6f8fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.error-text {
  color: #f56c6c;
}
</style>
