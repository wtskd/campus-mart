<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input v-model="query.userId" placeholder="接收人 ID" clearable style="width: 150px" />
        <el-select v-model="query.msgType" placeholder="消息类型" clearable style="width: 140px">
          <el-option v-for="item in msgTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.isRead" placeholder="已读状态" clearable style="width: 120px">
          <el-option label="未读" value="0" />
          <el-option label="已读" value="1" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">消息列表</span>
        <span class="tip">共 {{ total }} 条（系统 / 订单 / 审核 / AI 通知）</span>
        <el-button size="small" :icon="Refresh" @click="getList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="messageId" label="ID" width="90" />
        <el-table-column label="接收人" width="160">
          <template #default="{ row }">
            <span>{{ row.receiverNickname || ('用户 ' + row.userId) }}</span>
            <span class="sub-id">#{{ row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.msgType)" size="small">{{ row.msgTypeText || typeText(row.msgType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="Number(row.isRead) === 1 ? 'info' : 'warning'" size="small">
              {{ Number(row.isRead) === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发送时间" width="170" />
      </el-table>

      <Pagination :total="total" :page="query.current" :limit="query.size" @pagination="handlePagination" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { useTable } from '@/composables/useTable'
import { getMessagePage } from '@/api/item'

const msgTypes = [
  { label: '系统通知', value: 1 },
  { label: '订单消息', value: 2 },
  { label: '审核通知', value: 3 },
  { label: 'AI 通知', value: 4 }
]

const { loading, list, total, query, getList, handleSearch, handlePagination } = useTable(
  getMessagePage,
  { userId: '', msgType: '', isRead: '' },
  { immediate: false }
)

function handleReset() {
  query.userId = ''
  query.msgType = ''
  query.isRead = ''
  query.current = 1
  getList()
}

function typeText(type) {
  const hit = msgTypes.find((item) => item.value === Number(type))
  return hit ? hit.label : '其他'
}

function typeTag(type) {
  return { 1: 'info', 2: 'success', 3: 'warning', 4: 'primary' }[Number(type)] || 'info'
}

onMounted(getList)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.title {
  font-size: 15px;
  font-weight: 600;
}

.tip {
  color: #909399;
  font-size: 12px;
  margin-right: auto;
}

.sub-id {
  color: #a8abb2;
  font-size: 12px;
  margin-left: 4px;
}
</style>
