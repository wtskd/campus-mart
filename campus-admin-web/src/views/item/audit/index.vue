<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input v-model="query.keyword" placeholder="商品标题 / 编号" clearable style="width: 240px" @keyup.enter="handleSearch" />
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>
      <el-alert
        title="审核队列：只展示 status=1（待审核）的商品。通过后商品进入在售状态；违规将写入审核意见并锁定商品。"
        type="warning"
        :closable="false"
        show-icon
      />
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">待审核商品（{{ total }}）</span>
        <div>
          <el-button :disabled="!selection.length" type="danger" plain @click="handleBatch(false)">
            批量驳回（{{ selection.length }}）
          </el-button>
          <el-button :disabled="!selection.length" type="success" @click="handleBatch(true)">
            批量通过（{{ selection.length }}）
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column label="封面" width="80">
          <template #default="{ row }">
            <el-image :src="row.coverUrl" fit="cover" style="width: 52px; height: 52px; border-radius: 4px" />
          </template>
        </el-table-column>
        <el-table-column label="商品信息" min-width="260">
          <template #default="{ row }">
            <div class="title-line">{{ row.title }}</div>
            <div class="sub-line">{{ row.itemNo }} ｜ 卖家 {{ row.sellerId }} ｜ {{ row.categoryId ? '分类 ' + row.categoryId : '' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="100" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.price) }}</template>
        </el-table-column>
        <el-table-column label="成色" width="90" align="center">
          <template #default="{ row }">{{ row.itemCondition }} 成</template>
        </el-table-column>
        <el-table-column label="AI 生成" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="String(row.aiGenerated) === '1'" size="small" type="warning">AI</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'item:item:audit'" link type="success" @click="openAudit(row, true)">通过</el-button>
            <el-button v-hasPerm="'item:item:audit'" link type="danger" @click="openAudit(row, false)">违规</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        v-model:page="query.current"
        v-model:limit="query.size"
        :total="total"
        @pagination="loadList"
      />
    </el-card>

    <el-dialog v-model="auditVisible" :title="auditForm.pass ? '审核通过' : '判定违规'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="商品">{{ auditRow.title }}</el-form-item>
        <el-form-item label="审核意见">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="3"
            :placeholder="auditForm.pass ? '可填写备注（选填）' : '请填写违规原因，卖家可在消息中心看到'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button :type="auditForm.pass ? 'success' : 'danger'" :loading="submitting" @click="submitAudit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { auditItem, getItemPage } from '@/api/item'
import { formatMoney } from '@/utils/format'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const selection = ref([])
const query = reactive({ current: 1, size: 10, keyword: '', status: 1 })

async function loadList() {
  loading.value = true
  try {
    // 固定 status=1：待审核
    const page = await getItemPage({ ...query, status: 1 })
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

function onSelectionChange(rows) {
  selection.value = rows
}

const auditVisible = ref(false)
const auditRow = ref({})
const auditForm = reactive({ pass: true, auditRemark: '' })

function openAudit(row, pass) {
  auditRow.value = row
  auditForm.pass = pass
  auditForm.auditRemark = ''
  auditVisible.value = true
}

async function submitAudit() {
  if (!auditForm.pass && !auditForm.auditRemark.trim()) {
    ElMessage.warning('判定违规时必须填写原因')
    return
  }
  submitting.value = true
  try {
    await auditItem({ itemId: auditRow.value.itemId, pass: auditForm.pass, auditRemark: auditForm.auditRemark })
    ElMessage.success(auditForm.pass ? '已通过' : '已驳回')
    auditVisible.value = false
    loadList()
  } catch (error) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

/** 批量审核：逐条调用（后端 `.../item/audit` 是单条接口，契约未提供批量接口） */
async function handleBatch(pass) {
  const rows = selection.value
  if (!rows.length) return
  let ok = 0
  for (const row of rows) {
    try {
      await auditItem({ itemId: row.itemId, pass, auditRemark: pass ? '批量审核通过' : '批量判定违规' })
      ok += 1
    } catch (error) {
      // 单条失败继续
    }
  }
  ElMessage.success(`处理完成：成功 ${ok} / ${rows.length}`)
  loadList()
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

.title-line {
  font-weight: 500;
}

.sub-line {
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
}
</style>
