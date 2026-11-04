<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input v-model="query.itemId" placeholder="商品 ID" clearable style="width: 130px" @keyup.enter="handleSearch" />
        <DictSelect v-model="query.reasonType" dict-type="report_reason" placeholder="举报原因" width="150px" />
        <el-select v-model="query.status" placeholder="处理状态" clearable style="width: 140px">
          <el-option label="待处理" :value="0" />
          <el-option label="已处理" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">举报处理</span>
        <el-tag type="danger" effect="plain">待处理 {{ pendingCount }} 条</el-tag>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="reportId" label="ID" width="80" />
        <el-table-column label="举报原因" width="130" align="center">
          <template #default="{ row }">
            <DictTag dict-type="report_reason" :value="row.reasonType" />
          </template>
        </el-table-column>
        <el-table-column label="补充说明" min-width="260">
          <template #default="{ row }">
            <div class="content">{{ row.content || '-' }}</div>
            <div v-if="parseImages(row.images).length" class="images">
              <el-image
                v-for="(url, index) in parseImages(row.images)"
                :key="index"
                :src="url"
                :preview-src-list="parseImages(row.images)"
                fit="cover"
                style="width: 44px; height: 44px; border-radius: 4px"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="关联" width="170">
          <template #default="{ row }">
            <div>商品 {{ row.itemId }}</div>
            <div class="sub-line">举报人 {{ row.reporterId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_MAP[Number(row.status)]?.type || 'info'" size="small">
              {{ STATUS_MAP[Number(row.status)]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="举报时间" width="170" />
        <el-table-column label="处理结果" min-width="180">
          <template #default="{ row }">
            <div class="sub-line">{{ row.handleRemark || '-' }}</div>
            <div class="sub-line">{{ row.handleTime || '' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-hasPerm="'item:report:handle'"
              :disabled="Number(row.status) !== 0"
              link
              type="primary"
              @click="openHandle(row)"
            >
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.current" v-model:limit="query.size" :total="total" @pagination="loadList" />
    </el-card>

    <el-dialog v-model="handleVisible" title="处理举报" width="520px">
      <el-form label-width="90px">
        <el-form-item label="商品 ID">{{ current.reportId }} / 商品 {{ current.itemId }}</el-form-item>
        <el-form-item label="处理结果">
          <el-radio-group v-model="handleForm.status">
            <el-radio :value="1">确认违规（已处理）</el-radio>
            <el-radio :value="2">驳回（举报不成立）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input
            v-model="handleForm.handleRemark"
            type="textarea"
            :rows="3"
            placeholder="填写处理结论，举报人可在消息中心看到"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import DictSelect from '@/components/DictSelect/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import { getReportPage, handleReport } from '@/api/item'

const STATUS_MAP = {
  0: { label: '待处理', type: 'warning' },
  1: { label: '已处理', type: 'success' },
  2: { label: '已驳回', type: 'info' }
}

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, itemId: '', reasonType: undefined, status: undefined })

const pendingCount = computed(() => (list.value || []).filter((row) => Number(row.status) === 0).length)

async function loadList() {
  loading.value = true
  try {
    const page = await getReportPage({ ...query })
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
  Object.assign(query, { current: 1, size: 10, itemId: '', reasonType: undefined, status: undefined })
  loadList()
}

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

const handleVisible = ref(false)
const current = ref({})
const handleForm = reactive({ status: 1, handleRemark: '' })

function openHandle(row) {
  current.value = row
  handleForm.status = 1
  handleForm.handleRemark = ''
  handleVisible.value = true
}

async function submitHandle() {
  submitting.value = true
  try {
    await handleReport({
      reportId: current.value.reportId,
      status: handleForm.status,
      handleRemark: handleForm.handleRemark
    })
    ElMessage.success('处理完成')
    handleVisible.value = false
    loadList()
  } catch (error) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
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
