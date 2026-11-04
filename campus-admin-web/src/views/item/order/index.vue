<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input v-model="query.orderNo" placeholder="订单号" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <el-input v-model="query.buyerId" placeholder="买家 ID" clearable style="width: 130px" @keyup.enter="handleSearch" />
        <el-input v-model="query.sellerId" placeholder="卖家 ID" clearable style="width: 130px" @keyup.enter="handleSearch" />
        <el-select v-model="query.status" placeholder="订单状态" clearable style="width: 140px">
          <el-option v-for="(item, value) in STATUS_MAP" :key="value" :label="item.label" :value="Number(value)" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">订单管理</span>
        <span class="sub">成交额合计：<b class="money">¥{{ formatMoney(summary.amount) }}</b>（当前页）</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="orderNo" label="订单号" width="200">
          <template #default="{ row }"><span class="mono">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="商品 / 买卖双方" min-width="220">
          <template #default="{ row }">
            <div>{{ row.itemTitle || ('商品 ' + row.itemId) }}</div>
            <div class="sub-line">买家 {{ row.buyerId }} ｜ 卖家 {{ row.sellerId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }"><span class="money">¥{{ formatMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column label="支付方式" width="100" align="center">
          <template #default="{ row }">{{ PAY_TYPE_MAP[row.payType] || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_MAP[row.status]?.type || 'info'" size="small">
              {{ STATUS_MAP[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tradePlace" label="交易地点" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.tradePlace || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
            <el-button
              v-hasPerm="'item:order:status'"
              :disabled="[2, 3, 5].includes(row.status)"
              link
              type="warning"
              @click="openStatus(row)"
            >
              改状态
            </el-button>
            <el-button
              v-hasPerm="'item:order:refund'"
              :disabled="[3, 5].includes(row.status)"
              link
              type="danger"
              @click="handleRefund(row)"
            >
              退款
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

    <!-- 改状态 -->
    <el-dialog v-model="statusVisible" title="修改订单状态" width="440px">
      <el-form label-width="90px">
        <el-form-item label="订单号"><span class="mono">{{ statusRow.orderNo }}</span></el-form-item>
        <el-form-item label="当前状态">
          <el-tag size="small">{{ STATUS_MAP[statusRow.status]?.label || statusRow.status }}</el-tag>
        </el-form-item>
        <el-form-item label="目标状态">
          <el-select v-model="targetStatus" style="width: 100%">
            <el-option
              v-for="(item, value) in STATUS_MAP"
              :key="value"
              :label="item.label"
              :value="Number(value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="statusRemark" type="textarea" :rows="2" placeholder="修改原因（会记入日志）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitStatus">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ STATUS_MAP[detail.status]?.label || detail.status }}</el-descriptions-item>
        <el-descriptions-item label="商品">{{ detail.itemTitle || detail.itemId }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ formatMoney(detail.amount) }}</el-descriptions-item>
        <el-descriptions-item label="买家 ID">{{ detail.buyerId }}</el-descriptions-item>
        <el-descriptions-item label="卖家 ID">{{ detail.sellerId }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ PAY_TYPE_MAP[detail.payType] || '-' }}</el-descriptions-item>
        <el-descriptions-item label="交易地点">{{ detail.tradePlace || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ detail.payTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ detail.finishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="取消原因">{{ detail.cancelReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="买家备注" :span="2">{{ detail.buyerRemark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getOrderPage, refundOrder, updateOrderStatus } from '@/api/item'
import { formatMoney } from '@/utils/format'

// 与 biz_order.status 对齐：0待确认 1待交付 2已完成 3已取消 4退款中 5已退款
const STATUS_MAP = {
  0: { label: '待确认', type: 'warning' },
  1: { label: '待交付', type: 'primary' },
  2: { label: '已完成', type: 'success' },
  3: { label: '已取消', type: 'info' },
  4: { label: '退款中', type: 'danger' },
  5: { label: '已退款', type: 'danger' }
}

const PAY_TYPE_MAP = { 0: '线下', 1: '模拟在线' }

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, orderNo: '', buyerId: '', sellerId: '', status: undefined })

const summary = computed(() => ({
  amount: (list.value || []).reduce((sum, row) => sum + Number(row.amount || 0), 0)
}))

async function loadList() {
  loading.value = true
  try {
    const page = await getOrderPage({ ...query })
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
  Object.assign(query, { current: 1, size: 10, orderNo: '', buyerId: '', sellerId: '', status: undefined })
  loadList()
}

// 改状态
const statusVisible = ref(false)
const statusRow = ref({})
const targetStatus = ref(0)
const statusRemark = ref('')

function openStatus(row) {
  statusRow.value = row
  targetStatus.value = row.status
  statusRemark.value = ''
  statusVisible.value = true
}

async function submitStatus() {
  submitting.value = true
  try {
    await updateOrderStatus({ orderId: statusRow.value.orderId, status: targetStatus.value, remark: statusRemark.value })
    ElMessage.success('状态已更新')
    statusVisible.value = false
    loadList()
  } catch (error) {
    // 拦截器已提示（后端会校验非法流转并返回业务错误）
  } finally {
    submitting.value = false
  }
}

function handleRefund(row) {
  ElMessageBox.prompt(`对订单 ${row.orderNo} 发起退款，请填写原因`, '退款', {
    inputPlaceholder: '如买家申请退款 / 商品与描述不符',
    inputValidator: (value) => (value && value.trim() ? true : '请填写退款原因')
  })
    .then(async ({ value }) => {
      await refundOrder({ orderId: row.orderId, reason: value })
      ElMessage.success('退款申请已处理')
      loadList()
    })
    .catch(() => {})
}

const detailVisible = ref(false)
const detail = ref({})

function showDetail(row) {
  detail.value = { ...row }
  detailVisible.value = true
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

.sub {
  color: #909399;
  font-size: 13px;
}

.sub-line {
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
}

.money {
  color: #f56c6c;
  font-family: Consolas, monospace;
}

.mono {
  font-family: Consolas, monospace;
  font-size: 12px;
}
</style>
