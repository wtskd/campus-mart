<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input v-model="query.keyword" placeholder="商品标题 / 编号" clearable style="width: 220px" @keyup.enter="handleSearch" />
        <el-select v-model="query.categoryId" placeholder="分类" clearable filterable style="width: 150px">
          <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
          <el-option v-for="(item, value) in STATUS_MAP" :key="value" :label="item.label" :value="Number(value)" />
        </el-select>
        <el-input v-model="query.sellerId" placeholder="卖家 ID" clearable style="width: 130px" @keyup.enter="handleSearch" />
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">商品管理（审核 / 违规下架）</span>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="封面" width="86">
          <template #default="{ row }">
            <el-image
              :src="row.coverUrl"
              :preview-src-list="parseImages(row)"
              fit="cover"
              style="width: 56px; height: 56px; border-radius: 4px"
              preview-teleported
            >
              <template #error>
                <div class="img-placeholder">无图</div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="230">
          <template #default="{ row }">
            <div class="item-title" @click="openDetail(row)">{{ row.title }}</div>
            <div class="item-sub">编号 {{ row.itemNo }} ｜ 卖家 {{ row.sellerId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="110" align="right">
          <template #default="{ row }">
            <span class="money">¥{{ formatMoney(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="110">
          <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_MAP[row.status]?.type || 'info'" size="small">
              {{ STATUS_MAP[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="浏览/收藏" width="110" align="center">
          <template #default="{ row }">{{ formatNumber(row.viewCount) }} / {{ formatNumber(row.favoriteCount) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 1"
              v-hasPerm="'item:item:audit'"
              link
              type="success"
              @click="openAudit(row)"
            >
              审核
            </el-button>
            <el-button
              v-if="[1, 2, 3].includes(row.status)"
              v-hasPerm="'item:item:status'"
              link
              type="danger"
              @click="forceOffline(row)"
            >
              强制下架
            </el-button>
            <el-button
              v-hasPerm="'item:item:remove'"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
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

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="商品详情" size="560px">
      <div v-loading="detailLoading" class="detail">
        <h3>{{ detail.title }}</h3>
        <div class="detail-meta">
          <el-tag :type="STATUS_MAP[detail.status]?.type || 'info'" size="small">
            {{ STATUS_MAP[detail.status]?.label || '-' }}
          </el-tag>
          <span class="mono">编号 {{ detail.itemNo }}</span>
          <span class="mono">卖家 ID {{ detail.sellerId }}</span>
        </div>
        <el-descriptions :column="2" border size="small" class="detail-desc">
          <el-descriptions-item label="售价">¥{{ formatMoney(detail.price) }}</el-descriptions-item>
          <el-descriptions-item label="原价">¥{{ formatMoney(detail.originalPrice) }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ categoryName(detail.categoryId) }}</el-descriptions-item>
          <el-descriptions-item label="成色">{{ detail.itemCondition }} 成</el-descriptions-item>
          <el-descriptions-item label="交易方式">{{ TRADE_TYPE_MAP[detail.tradeType] || '-' }}</el-descriptions-item>
          <el-descriptions-item label="交易地点">{{ detail.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="浏览">{{ formatNumber(detail.viewCount) }}</el-descriptions-item>
          <el-descriptions-item label="收藏">{{ formatNumber(detail.favoriteCount) }}</el-descriptions-item>
          <el-descriptions-item label="发布时间" :span="2">{{ detail.publishTime || detail.createTime }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.auditRemark" label="审核意见" :span="2">
            <span class="error-text">{{ detail.auditRemark }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            <div class="desc-text">{{ detail.description || '（无描述）' }}</div>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="parseImages(detail).length" class="detail-images">
          <el-image
            v-for="(url, index) in parseImages(detail)"
            :key="index"
            :src="url"
            :preview-src-list="parseImages(detail)"
            :initial-index="index"
            fit="cover"
            class="detail-image"
            preview-teleported
          />
        </div>
      </div>
    </el-drawer>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" title="商品审核" width="480px">
      <el-form label-width="90px">
        <el-form-item label="商品">{{ auditRow.title }}</el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.pass">
            <el-radio :value="true">审核通过（上架在售）</el-radio>
            <el-radio :value="false">违规驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" placeholder="驳回时请填写原因，会通知卖家" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditing" @click="submitAudit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { auditItem, deleteItem, getCategoryList, getItem, getItemPage, updateItemStatus } from '@/api/item'
import { formatMoney, formatNumber } from '@/utils/format'

const STATUS_MAP = {
  0: { label: '草稿', type: 'info' },
  1: { label: '待审核', type: 'warning' },
  2: { label: '在售', type: 'success' },
  3: { label: '已预订', type: 'primary' },
  4: { label: '已售出', type: 'info' },
  5: { label: '下架', type: 'info' },
  6: { label: '违规', type: 'danger' }
}

const TRADE_TYPE_MAP = { 1: '出售', 2: '求购', 3: '交换' }

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '', categoryId: undefined, status: undefined, sellerId: '' })
const categories = ref([])

const categoryOptions = computed(() => categories.value)
function categoryName(id) {
  return categories.value.find((item) => String(item.categoryId) === String(id))?.categoryName || `分类${id}`
}

async function loadCategories() {
  try {
    categories.value = (await getCategoryList()) || []
  } catch (error) {
    categories.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const page = await getItemPage({ ...query })
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
  Object.assign(query, { current: 1, size: 10, keyword: '', categoryId: undefined, status: undefined, sellerId: '' })
  loadList()
}

function parseImages(row) {
  const images = row?.images
  if (!images) return row?.coverUrl ? [row.coverUrl] : []
  if (Array.isArray(images)) return images
  try {
    const parsed = JSON.parse(images)
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    return row?.coverUrl ? [row.coverUrl] : []
  }
}

// ---------------- 详情 ----------------
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({})

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = { ...row }
  try {
    const data = await getItem(row.itemId)
    if (data) detail.value = { ...row, ...data }
  } catch (error) {
    // 前台详情接口对下架商品不可见时保留列表行数据
  } finally {
    detailLoading.value = false
  }
}

// ---------------- 审核 ----------------
const auditVisible = ref(false)
const auditing = ref(false)
const auditRow = ref({})
const auditForm = reactive({ pass: true, auditRemark: '' })

function openAudit(row) {
  auditRow.value = row
  auditForm.pass = true
  auditForm.auditRemark = ''
  auditVisible.value = true
}

async function submitAudit() {
  if (!auditForm.pass && !auditForm.auditRemark) {
    ElMessage.warning('驳回时请填写审核意见')
    return
  }
  auditing.value = true
  try {
    await auditItem({ itemId: auditRow.value.itemId, pass: auditForm.pass, auditRemark: auditForm.auditRemark })
    ElMessage.success(auditForm.pass ? '已审核通过' : '已驳回')
    auditVisible.value = false
    loadList()
  } catch (error) {
    // 拦截器已提示
  } finally {
    auditing.value = false
  }
}

// ---------------- 强制下架 / 删除 ----------------
function forceOffline(row) {
  ElMessageBox.prompt(`强制下架「${row.title}」，请填写原因（会写入审核意见）`, '强制下架', {
    inputPlaceholder: '如：疑似违禁品、虚假信息',
    inputValidator: (value) => (value && value.trim() ? true : '原因不能为空')
  })
    .then(async ({ value }) => {
      await updateItemStatus({ itemId: row.itemId, status: 5, remark: value })
      ElMessage.success('已强制下架')
      loadList()
    })
    .catch(() => {})
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除商品「${row.title}」吗？（逻辑删除，管理员可恢复）`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteItem(row.itemId)
      ElMessage.success('删除成功')
      loadList()
    })
    .catch(() => {})
}

onMounted(() => {
  loadCategories()
  loadList()
})
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

.item-title {
  cursor: pointer;
  color: #1f6feb;
  font-weight: 500;
}

.item-title:hover {
  text-decoration: underline;
}

.item-sub {
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
}

.money {
  color: #f56c6c;
  font-family: Consolas, monospace;
}

.img-placeholder {
  display: flex;
  width: 56px;
  height: 56px;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 12px;
  border-radius: 4px;
}

.detail h3 {
  margin: 0 0 8px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}

.mono {
  font-family: Consolas, monospace;
  font-size: 12px;
  color: #909399;
}

.detail-desc {
  margin-bottom: 12px;
}

.desc-text {
  white-space: pre-wrap;
  line-height: 1.7;
}

.detail-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-image {
  width: 96px;
  height: 96px;
  border-radius: 4px;
}

.error-text {
  color: #f56c6c;
}
</style>
