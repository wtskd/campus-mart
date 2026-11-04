<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input v-model="query.keyword" placeholder="评价内容" clearable style="width: 220px" @keyup.enter="handleSearch" />
        <el-input v-model="query.itemId" placeholder="商品 ID" clearable style="width: 130px" @keyup.enter="handleSearch" />
        <DictSelect v-model="query.status" dict-type="sys_normal_disable" placeholder="状态" width="140px" />
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">商品评价</span>
        <span class="sub">隐藏后买家端不再展示该评价</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="commentId" label="ID" width="80" />
        <el-table-column label="评价内容" min-width="320">
          <template #default="{ row }">
            <div class="content">{{ row.content || '（未填写文字）' }}</div>
            <div v-if="parseImages(row.images).length" class="images">
              <el-image
                v-for="(url, index) in parseImages(row.images)"
                :key="index"
                :src="url"
                :preview-src-list="parseImages(row.images)"
                :initial-index="index"
                fit="cover"
                style="width: 48px; height: 48px; border-radius: 4px"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="140" align="center">
          <template #default="{ row }">
            <el-rate :model-value="Number(row.score) || 0" disabled />
          </template>
        </el-table-column>
        <el-table-column label="评价关系" min-width="160">
          <template #default="{ row }">
            <div>评价人 {{ row.fromUserId }}</div>
            <div class="sub-line">被评价人 {{ row.toUserId }} ｜ 订单 {{ row.orderId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="String(row.status) === '1' ? 'info' : 'success'" size="small">
              {{ String(row.status) === '1' ? '已隐藏' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评价时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-hasPerm="'item:comment:audit'"
              link
              :type="String(row.status) === '1' ? 'success' : 'warning'"
              @click="toggleStatus(row)"
            >
              {{ String(row.status) === '1' ? '恢复展示' : '隐藏' }}
            </el-button>
            <el-button v-hasPerm="'item:comment:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.current" v-model:limit="query.size" :total="total" @pagination="loadList" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import DictSelect from '@/components/DictSelect/index.vue'
import { auditComment, deleteComment, getCommentPage } from '@/api/item'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '', itemId: '', status: undefined })

async function loadList() {
  loading.value = true
  try {
    const page = await getCommentPage({ ...query })
    list.value = page.records || []
    total.value = page.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.current = 1
  loadList()
}

function handleReset() {
  Object.assign(query, { current: 1, size: 10, keyword: '', itemId: '', status: undefined })
  loadList()
}

/** 后端 images 为 JSON 数组（Long→String 序列化后可能是字符串） */
function parseImages(images) {
  if (!images) return []
  if (Array.isArray(images)) return images
  try {
    const parsed = JSON.parse(images)
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    return String(images).startsWith('http') ? [images] : []
  }
}

async function toggleStatus(row) {
  const next = String(row.status) === '1' ? '0' : '1'
  try {
    await auditComment({ commentId: row.commentId, status: next })
    ElMessage.success(next === '1' ? '已隐藏' : '已恢复展示')
    loadList()
  } catch (error) {
    // 拦截器已提示
  }
}

function handleDelete(row) {
  ElMessageBox.confirm('确认删除该评价？删除后不可恢复', '提示', { type: 'warning' })
    .then(async () => {
      await deleteComment(row.commentId)
      ElMessage.success('删除成功')
      loadList()
    })
    .catch(() => {})
}

onMounted(loadList)
</script>

<style scoped>
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

.sub,
.sub-line {
  color: #909399;
  font-size: 12px;
}

.content {
  line-height: 1.5;
  word-break: break-all;
}

.images {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}
</style>
