<!-- 我的订单：我买到的 / 我卖出的 + 状态筛选 -->
<template>
  <view class="page">
    <!-- 角色 tab -->
    <view class="role-tabs">
      <view class="role-tab" :class="{ active: role === 'buy' }" @tap="changeRole('buy')">我买到的</view>
      <view class="role-tab" :class="{ active: role === 'sell' }" @tap="changeRole('sell')">我卖出的</view>
    </view>

    <!-- 状态筛选 -->
    <scroll-view class="status-tabs" scroll-x>
      <view
        v-for="s in statusTabs"
        :key="s.value"
        class="status-tab"
        :class="{ active: activeStatus === s.value }"
        @tap="changeStatus(s.value)"
      >{{ s.label }}</view>
    </scroll-view>

    <view v-if="loading && !orders.length" class="skeleton-wrap">
      <skeleton-box :rows="4" mode="row" />
    </view>

    <view v-else-if="orders.length" class="list">
      <view v-for="o in orders" :key="o.orderId" class="card" @tap="goDetail(o)">
        <view class="card-head">
          <text class="order-no">订单号 {{ o.orderNo }}</text>
          <text class="status" :style="{ color: statusInfo(o).color }">{{ statusInfo(o).text }}</text>
        </view>
        <view class="goods">
          <image class="cover" :src="mediaUrl(o.itemCover || o.itemCoverUrl)" mode="aspectFill" lazy-load />
          <view class="goods-info">
            <text class="title">{{ o.itemTitle }}</text>
            <text class="time">{{ formatDateTime(o.createTime) }}</text>
            <text class="counterpart">
              {{ role === 'buy' ? '卖家' : '买家' }}：{{ role === 'buy' ? o.sellerNickname : o.buyerNickname }}
            </text>
          </view>
          <text class="amount">{{ formatPrice(o.amount) }}</text>
        </view>
        <view class="actions" @tap.stop>
          <text v-if="canCancel(o)" class="btn ghost" @tap="cancel(o)">取消订单</text>
          <text v-if="canPay(o)" class="btn primary" @tap="pay(o)">付款</text>
          <text v-if="canDeliver(o)" class="btn primary" @tap="deliver(o)">确认交付</text>
          <text v-if="canConfirm(o)" class="btn primary" @tap="confirm(o)">确认收货</text>
          <text v-if="canComment(o)" class="btn plain" @tap="goDetail(o)">去评价</text>
          <text v-else-if="hasComment(o)" class="btn plain done" @tap="goDetail(o)">已评价</text>
          <text class="btn plain" @tap="goDetail(o)">查看详情</text>
        </view>
      </view>
      <view class="load-more">
        <text v-if="loadingMore">加载中…</text>
        <text v-else-if="finished">— 没有更多了 —</text>
        <text v-else>上拉加载更多</text>
      </view>
    </view>

    <empty-state
      v-else
      icon="🧾"
      :text="role === 'buy' ? '还没有买到的订单' : '还没有卖出的订单'"
      :sub="role === 'buy' ? '去首页看看有没有心仪的好物' : '发布闲置，等买家下单'"
      :action-text="role === 'buy' ? '去逛逛' : '去发布'"
      @action="goAction"
    />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import skeletonBox from '@/components/skeleton-box.vue'
import emptyState from '@/components/empty-state.vue'
import { cancelOrder, confirmOrder, deliverOrder, getMyOrders, payOrder } from '@/api/order'
import { getCommentList } from '@/api/comment'
import { formatPrice, formatDateTime, orderStatusInfo, mediaUrl } from '@/utils/format'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const role = ref('buy')
const activeStatus = ref('')
const orders = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const finished = ref(false)
const current = ref(1)
const size = 10

const statusTabs = [
  { label: '全部', value: '' },
  { label: '待确认', value: 0 },
  { label: '待交付', value: 1 },
  { label: '已完成', value: 2 },
  { label: '已取消', value: 3 }
]

function statusInfo(order) {
  return orderStatusInfo(order.status)
}

function canPay(o) {
  return role.value === 'buy' && Number(o.status) === 0
}
function canDeliver(o) {
  return role.value === 'sell' && [0, 1].includes(Number(o.status))
}
function canConfirm(o) {
  return role.value === 'buy' && Number(o.status) === 1
}
function canCancel(o) {
  return [0, 1].includes(Number(o.status))
}
function canComment(o) {
  return role.value === 'buy' && Number(o.status) === 2 && !o.commented
}
/** 已完成且已评价：按钮变「已评价」，点进详情可查看评价内容 */
function hasComment(o) {
  return role.value === 'buy' && Number(o.status) === 2 && Boolean(o.commented)
}

async function loadOrders(reset = true) {
  if (reset) {
    current.value = 1
    finished.value = false
    loading.value = true
  } else {
    if (finished.value || loadingMore.value) return
    loadingMore.value = true
  }
  try {
    const params = { role: role.value, current: current.value, size }
    if (activeStatus.value !== '') params.status = activeStatus.value
    const res = await getMyOrders(params)
    const records = (res && res.records) || []
    orders.value = reset ? records : orders.value.concat(records)
    const total = (res && res.total) || 0
    finished.value = records.length < size || orders.value.length >= total
    current.value = reset ? 2 : current.value + 1
    await fillCommented(records)
  } catch (e) {
    if (reset) orders.value = []
    finished.value = true
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

/**
 * 评价状态兜底：新后端会在 OrderVO 里直接回填 commented，旧后端没有时
 * 逐个查已完成订单的评论列表，找 orderId 匹配的那条，把按钮切成「已评价」。
 * 只处理本页「已完成 + 我买到的 + commented 为空」的订单，最多 10 条。
 */
async function fillCommented(records) {
  if (role.value !== 'buy') return
  const todo = records.filter((o) => Number(o.status) === 2 && o.commented === undefined)
  if (!todo.length) return
  await Promise.all(
    todo.map(async (o) => {
      try {
        const res = await getCommentList(o.itemId, { current: 1, size: 50 })
        const mine = ((res && res.records) || []).find(
          (c) => String(c.orderId) === String(o.orderId)
        )
        const target = orders.value.find((x) => String(x.orderId) === String(o.orderId))
        if (mine && target) {
          target.commented = true
          target.commentScore = mine.score
          target.commentContent = mine.content
          target.commentTime = mine.createTime
          target.commentReply = mine.replyContent
        }
      } catch (e) {
        /* 兜底失败忽略，不影响订单列表 */
      }
    })
  )
}

function changeRole(value) {
  if (role.value === value) return
  role.value = value
  loadOrders(true)
}

function changeStatus(value) {
  activeStatus.value = value
  loadOrders(true)
}

function goDetail(o) {
  uni.navigateTo({ url: `/pages/order/detail?orderId=${o.orderId}` })
}

function refreshOne() {
  loadOrders(true)
}

async function pay(o) {
  try {
    await payOrder(o.orderId)
    uni.showToast({ title: '付款成功', icon: 'none' })
    refreshOne()
  } catch (e) {
    /* 已统一 toast */
  }
}

async function deliver(o) {
  try {
    await deliverOrder(o.orderId)
    uni.showToast({ title: '已确认交付', icon: 'none' })
    refreshOne()
  } catch (e) {
    /* 已统一 toast */
  }
}

async function confirm(o) {
  uni.showModal({
    title: '确认收货',
    content: '确认已收到商品吗？确认后交易完成',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await confirmOrder(o.orderId)
        uni.showToast({ title: '交易完成', icon: 'none' })
        refreshOne()
      } catch (e) {
        /* 已统一 toast */
      }
    }
  })
}

function cancel(o) {
  uni.showModal({
    title: '取消订单',
    content: '取消后商品将恢复在售，确定取消吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await cancelOrder(o.orderId, '买家取消')
        uni.showToast({ title: '订单已取消', icon: 'none' })
        refreshOne()
      } catch (e) {
        /* 已统一 toast */
      }
    }
  })
}

function goAction() {
  if (role.value === 'buy') uni.switchTab({ url: '/pages/index/index' })
  else uni.switchTab({ url: '/pages/item/publish' })
}

onLoad((options) => {
  if (options && options.role === 'sell') role.value = 'sell'
  if (options && options.status !== undefined && options.status !== '') {
    activeStatus.value = Number(options.status)
  }
})

onShow(() => {
  userStore.initFromStorage()
  if (!userStore.requireLogin()) return
  loadOrders(true)
})

onPullDownRefresh(async () => {
  await loadOrders(true)
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (!finished.value) loadOrders(false)
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $cm-page-bg;
}
.role-tabs {
  display: flex;
  background: #fff;
  .role-tab {
    flex: 1;
    text-align: center;
    padding: 24rpx 0;
    font-size: 28rpx;
    color: $cm-text-2;
    position: relative;
    &.active {
      color: $cm-primary;
      font-weight: 600;
      &::after {
        content: '';
        position: absolute;
        left: 50%;
        bottom: 6rpx;
        transform: translateX(-50%);
        width: 56rpx;
        height: 6rpx;
        background: $cm-primary;
        border-radius: 4rpx;
      }
    }
  }
}
.status-tabs {
  white-space: nowrap;
  background: #fff;
  padding: 8rpx 12rpx 16rpx;
  .status-tab {
    display: inline-block;
    padding: 8rpx 24rpx;
    margin: 0 8rpx;
    border-radius: 999rpx;
    font-size: 23rpx;
    color: $cm-text-2;
    background: $cm-page-bg;
    &.active {
      background: $cm-primary-light;
      color: $cm-primary;
      font-weight: 600;
    }
  }
}
.skeleton-wrap {
  padding: 24rpx;
}
.list {
  padding: 16rpx 24rpx;
}
.card {
  background: #fff;
  border-radius: $cm-radius;
  margin-bottom: 20rpx;
  padding: 20rpx;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  .order-no {
    font-size: 21rpx;
    color: $cm-text-3;
  }
  .status {
    font-size: 24rpx;
    font-weight: 600;
  }
}
.goods {
  display: flex;
  margin-top: 16rpx;
  .cover {
    width: 150rpx;
    height: 150rpx;
    border-radius: $cm-radius-sm;
    background: #eef0f3;
    flex: none;
  }
  .goods-info {
    flex: 1;
    margin-left: 16rpx;
    display: flex;
    flex-direction: column;
  }
  .title {
    font-size: 26rpx;
    color: $cm-text;
    line-height: 38rpx;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .time {
    margin-top: 8rpx;
    font-size: 20rpx;
    color: $cm-text-4;
  }
  .counterpart {
    margin-top: auto;
    font-size: 21rpx;
    color: $cm-text-3;
  }
  .amount {
    margin-left: 12rpx;
    align-self: flex-end;
    color: $cm-danger;
    font-size: 28rpx;
    font-weight: 600;
  }
}
.actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid $cm-border;
  .btn {
    margin-left: 14rpx;
    padding: 8rpx 26rpx;
    border-radius: 999rpx;
    font-size: 23rpx;
    &.primary {
      background: $cm-primary;
      color: #fff;
    }
    &.ghost {
      border: 1rpx solid $cm-border;
      color: $cm-text-2;
    }
    &.plain {
      border: 1rpx solid $cm-primary;
      color: $cm-primary;
      &.done {
        border-color: $cm-border;
        color: $cm-text-4;
      }
    }
  }
}
.load-more {
  padding: 24rpx 0 40rpx;
  text-align: center;
  font-size: 22rpx;
  color: $cm-text-4;
}
</style>
