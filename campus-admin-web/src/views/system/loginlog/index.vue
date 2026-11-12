<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input
          v-model="query.userName"
          placeholder="登录账号"
          clearable
          style="width: 170px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" placeholder="登录状态" clearable style="width: 140px">
          <el-option label="成功" value="0" />
          <el-option label="失败" value="1" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">登录日志</span>
        <span class="tip">记录后台登录轨迹（IP / 浏览器 / 操作系统）</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="infoId" label="编号" width="90" align="center" />
        <el-table-column prop="username" label="登录账号" width="130" />
        <el-table-column prop="ipaddr" label="登录 IP" width="140" />
        <el-table-column prop="browser" label="浏览器" width="150" />
        <el-table-column prop="os" label="操作系统" width="140" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">
              {{ row.status === 1 ? '失败' : '成功' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="msg" label="提示消息" min-width="160" />
        <el-table-column prop="loginTime" label="登录时间" width="170" />
      </el-table>

      <Pagination
        v-model:page="query.current"
        v-model:limit="query.size"
        :total="total"
        @pagination="handlePagination"
      />
    </el-card>
  </div>
</template>

<script setup>
import { Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { useTable } from '@/composables/useTable'
import { getLoginLogPage } from '@/api/system'

const { loading, list, total, query, getList, handleSearch, resetQuery, handlePagination } = useTable(
  getLoginLogPage,
  { userName: '', status: '' }
)
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
</style>
