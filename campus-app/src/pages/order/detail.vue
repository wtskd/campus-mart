<!-- 订单详情：状态 + 进度 + 操作（付款/交付/确认收货/取消/评价） -->
<template>
  <view class="page">
    <view v-if="loading" class="skeleton-wrap">
      <skeleton-box :rows="3" mode="row" />
    </view>

    <block v-else-if="order">
      <!-- 状态头 -->
      <view class="status-head" :style="{ background: statusInfo.color + '14' }">
        <text class="status-text" :style="{ color: statusInfo.color }">{{ statusInfo.text }}</text>
        <text class="status-desc">{{ statusDesc }}</text>
      </view>

      <!-- 商品 -->
      <view class="card goods" @tap="goItem">
        <image class="cover" :src="mediaUrl(order.itemCover || order.itemCoverUrl)" mode="aspectFill" />
        <view class="goods-info">
          <text class="title">{{ order.itemTitle }}</text>
          <text class="amount">{{ formatPrice(order.amount) }}</text>
        </view>
        <text class="arrow">›</text>
      </view>

      <!-- 交易信息 -->
      <view class="card">
        <view class="row"><text class="label">订单号</text><text class="value">{{ order.orderNo }}</text></view>
        <view class="row">
          <text class="label">{{ isBuyer ? '卖家' : '买家' }}</text>
          <text class="value">{{ isBuyer ? order.sellerNickname : order.buyerNickname }}</text>
        </view>
        <view class="row"><text class="label">交易地点</text><text class="value">{{ order.tradePlace || '待约定' }}</text></view>
        <view class="row"><text class="label">交易方式</text><text class="value">{{ payTypeText }}</text></view>
        <view v-if="order.buyerRemark" class="row">
          <text class="label">买家留言</text><text class="value">{{ order.buyerRemark }}</text>
        </view>
        <view v-if="order.cancelReason" class="row">
          <text class="label">取消原因</text><text class="value">{{ order.cancelReason }}</text>
        </view>
      </view>

      <!-- 进度时间轴 -->
      <view class="card">
        <text class="card-title">订单进度</text>
        <view class="timeline">
          <view v-for="(step, i) in timeline" :key="i" class="step" :class="{ done: step.done }">
            <view class="dot"></view>
            <view class="step-body">
              <text class="step-title">{{ step.title }}</text>
              <text v-if="step.time" class="step-time">{{ step.time }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-space"></view>

      <!-- 操作栏 -->
      <view class="bottom-bar">
        <text class="btn ghost" @tap="copyOrderNo">复制订单号</text>
        <text v-if="canCancel" class="btn ghost" @tap="cancel">取消订单</text>
        <text v-if="canPay" class="btn primary" @tap="pay">去付款</text>
        <text v-if="canDeliver" class="btn primary" @tap="deliver">确认交付</text>
        <text v-if="canConfirm" class="btn primary" @tap="confirm">确认收货</text>
        <text v-if="canComment" class="btn primary" @tap="openComment">评价卖家</text>
        <text v-if="hasComment" class="btn ghost" @tap="commentViewVisible = true">已评价 · 查看</text>
      </view>

      <!-- 已评价：查看评价内容 -->
      <view v-if="commentViewVisible" class="mask" @tap="commentViewVisible = false">
        <view class="comment-panel" @tap.stop>
          <text class="panel-title">我的评价</text>
          <view class="stars">
            <text v-for="n in 5" :key="n" class="star" :class="{ active: n <= (order.commentScore || 0) }">★</text>
          </view>
          <text class="comment-view-text">{{ order.commentContent || '（未填写评价内容）' }}</text>
          <text v-if="order.commentTime" class="comment-view-time">{{ formatDateTime(order.commentTime) }}</text>
          <view v-if="order.commentReply" class="comment-reply">
            <text class="reply-label">卖家回复</text>
            <text class="reply-text">{{ order.commentReply }}</text>
          </view>
          <view class="panel-btns">
            <view class="cm-btn cm-btn-primary" @tap="commentViewVisible = false">知道了</view>
          </view>
        </view>
      </view>

      <!-- 评价弹窗 -->
      <view v-if="commentVisible" class="mask" @tap="commentVisible = false">
        <view class="comment-panel" @tap.stop>
          <text class="panel-title">评价这次交易</text>
          <view class="stars">
            <text
              v-for="n in 5"
              :key="n"
              class="star"
              :class="{ active: n <= score }"
              @tap="score = n"
            >★</text>
          </view>
          <textarea v-model="commentContent" class="comment-input" maxlength="200" placeholder="说说商品成色、卖家沟通体验…" />
          <view class="panel-btns">
            <view class="cm-btn cm-btn-ghost" @tap="commentVisible = false">取消</view>
            <view class="cm-btn cm-btn-primary" @tap="submitComment">提交评价</view>
          </view>
        </view>
      </view>
    </block>

    <empty-state v-else icon="🧾" text="订单不存在" action-text="返回订单列表" @action="goList" />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import skeletonBox from '@/components/skeleton-box.vue'
import emptyState from '@/components/empty-state.vue'
import {
  cancelOrder,
  confirmOrder,
  deliverOrder,
  getOrderDetail,
  payOrder
} from '@/api/order'
import { addComment, getCommentList } from '@/api/comment'
import { formatPrice, formatDateTime, orderStatusInfo, mediaUrl } from '@/utils/format'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const orderId = ref('')
const order = ref(null)
const loading = ref(true)
const commentVisible = ref(false)
const commentViewVisible = ref(false)
const commentContent = ref('')
const score = ref(5)

const statusInfo = computed(() => orderStatusInfo(order.value && order.value.status))
const isBuyer = computed(
  () => order.value && String(order.value.buyerId) === String(userStore.userId)
)
const statusDesc = computed(() => {
  const s = Number(order.value && order.value.status)
  const map = {
    0: '订单已提交，等待买家付款 / 卖家确认',
    1: '买家已付款，等待卖家交付或买家确认收货',
    2: '交易已完成，感谢使用校园易站',
    3: '订单已取消，商品已恢复在售',
    4: '退款处理中，请耐心等待',
    5: '退款已完成'
  }
  return map[s] || ''
})
const payTypeText = computed(() => {
  const t = order.value && order.value.payType
  if (t === undefined || t === null || t === '') return '线下交易'
  return Number(t) === 1 ? '平台交易' : '线下交易'
})
const canPay = computed(() => isBuyer.value && Number(order.value.status) === 0)
const canDeliver = computed(() => !isBuyer.value && [0, 1].includes(Number(order.value.status)))
const canConfirm = computed(() => isBuyer.value && Number(order.value.status) === 1)
const canCancel = computed(() => [0, 1].includes(Number(order.value.status)))
const canComment = computed(
  () => isBuyer.value && Number(order.value.status) === 2 && !order.value.commented
)
/** 已评价：展示「已评价 · 查看」入口（后端 OrderVO 回填 commented/commentScore/commentContent/commentTime/commentReply） */
const hasComment = computed(() => Boolean(order.value && order.value.commented))
const timeline = computed(() => {
  const o = order.value || {}
  const status = Number(o.status)
  const created = status >= 0
  return [
    { title: '提交订单', time: o.createTime ? formatDateTime(o.createTime) : '', done: created },
    { title: '买家付款', time: o.payTime ? formatDateTime(o.payTime) : '', done: status >= 1 || !!o.payTime },
    {
      title: status === 3 ? '订单取消' : '交易完成',
      time: status === 3 ? formatDateTime(o.cancelTime) : o.finishTime ? formatDateTime(o.finishTime) : '',
      done: status === 2 || status === 3
    }
  ]
})

async function loadOrder() {
  loading.value = true
  try {
    order.value = await getOrderDetail(orderId.value)
    await fillCommentFallback(order.value)
  } catch (e) {
    order.value = null
  } finally {
    loading.value = false
  }
}

/**
 * 评价状态兜底：新后端会在 OrderVO 里直接回填 commented/commentScore/... 等字段，
 * 旧后端没有时退化为「查该商品的评价列表，找 orderId 匹配的那条」，保证「已评价」入口与内容可见。
 */
async function fillCommentFallback(o) {
  if (!o || Number(o.status) !== 2 || o.commented) return
  try {
    const res = await getCommentList(o.itemId, { current: 1, size: 50 })
    const records = (res && res.records) || []
    const mine = records.find((c) => String(c.orderId) === String(o.orderId))
    if (mine) {
      o.commented = true
      o.commentScore = mine.score
      o.commentContent = mine.content
      o.commentTime = mine.createTime
      o.commentReply = mine.replyContent
    }
  } catch (e) {
    /* 兜底失败不影响主流程 */
  }
}

async function pay() {
  try {
    await payOrder(orderId.value)
    uni.showToast({ title: '付款成功', icon: 'none' })
    loadOrder()
  } catch (e) {
    /* 已统一 toast */
  }
}

async function deliver() {
  uni.showModal({
    title: '确认交付',
    content: '确认已将商品交给买家？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deliverOrder(orderId.value)
        uni.showToast({ title: '已确认交付', icon: 'none' })
        loadOrder()
      } catch (e) {
        /* 已统一 toast */
      }
    }
  })
}

async function confirm() {
  uni.showModal({
    title: '确认收货',
    content: '确认已收到商品？确认后交易完成',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await confirmOrder(orderId.value)
        uni.showToast({ title: '交易完成', icon: 'none' })
        loadOrder()
      } catch (e) {
        /* 已统一 toast */
      }
    }
  })
}

function cancel() {
  uni.showModal({
    title: '取消订单',
    content: '取消后商品将恢复在售，确定取消吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await cancelOrder(orderId.value, '用户取消')
        uni.showToast({ title: '订单已取消', icon: 'none' })
        loadOrder()
      } catch (e) {
        /* 已统一 toast */
      }
    }
  })
}

function openComment() {
  commentVisible.value = true
}

async function submitComment() {
  if (!score.value) {
    uni.showToast({ title: '请打分', icon: 'none' })
    return
  }
  if (!commentContent.value.trim()) {
    uni.showToast({ title: '请填写评价内容', icon: 'none' })
    return
  }
  try {
    await addComment({
      orderId: orderId.value,
      itemId: order.value.itemId,
      // 后端 CommentCreateDTO 的字段名是 rating（返回体 CommentVO 才叫 score），传错后端会报「评分不能为空」
      rating: score.value,
      content: commentContent.value.trim(),
      images: []
    })
    commentVisible.value = false
    commentContent.value = ''
    uni.showToast({ title: '评价成功', icon: 'none' })
    loadOrder()
  } catch (e) {
    /* 已统一 toast */
  }
}

function copyOrderNo() {
  uni.setClipboardData({
    data: (order.value && order.value.orderNo) || '',
    success: () => uni.showToast({ title: '已复制', icon: 'none' })
  })
}

function goItem() {
  if (!order.value || !order.value.itemId) return
  uni.navigateTo({ url: `/pages/item/detail?itemId=${order.value.itemId}` })
}

function goList() {
  uni.redirectTo({ url: '/pages/order/list?role=buy' })
}

onLoad((options) => {
  userStore.initFromStorage()
  orderId.value = (options && options.orderId) || ''
  if (!orderId.value) {
    loading.value = false
    return
  }
  loadOrder()
})

onShow(() => {
  if (orderId.value && order.value) loadOrder()
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $cm-page-bg;
}
.skeleton-wrap {
  padding: 24rpx;
}
.status-head {
  padding: 36rpx 24rpx;
  .status-text {
    display: block;
    font-size: 40rpx;
    font-weight: 600;
  }
  .status-desc {
    display: block;
    margin-top: 10rpx;
    font-size: 23rpx;
    color: $cm-text-2;
  }
}
.card {
  margin: 20rpx 24rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: $cm-radius;
}
.goods {
  display: flex;
  align-items: center;
  .cover {
    width: 150rpx;
    height: 150rpx;
    border-radius: $cm-radius-sm;
    background: #eef0f3;
    flex: none;
  }
  .goods-info {
    flex: 1;
    margin-left: 18rpx;
  }
  .title {
    font-size: 27rpx;
    color: $cm-text;
    line-height: 38rpx;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .amount {
    display: block;
    margin-top: 12rpx;
    color: $cm-danger;
    font-size: 30rpx;
    font-weight: 600;
  }
  .arrow {
    color: $cm-text-4;
    font-size: 30rpx;
    margin-left: 10rpx;
  }
}
.row {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
  font-size: 25rpx;
  .label {
    color: $cm-text-3;
    flex: none;
    width: 160rpx;
  }
  .value {
    flex: 1;
    text-align: right;
    color: $cm-text;
    word-break: break-all;
  }
}
.card-title {
  font-size: 27rpx;
  font-weight: 600;
  color: $cm-text;
}
.timeline {
  margin-top: 12rpx;
  .step {
    display: flex;
    padding: 14rpx 0;
    position: relative;
    &::before {
      content: '';
      position: absolute;
      left: 11rpx;
      top: 40rpx;
      bottom: -6rpx;
      width: 2rpx;
      background: $cm-border;
    }
    &:last-child::before {
      display: none;
    }
    .dot {
      width: 24rpx;
      height: 24rpx;
      border-radius: 50%;
      background: $cm-border;
      flex: none;
      margin-top: 6rpx;
    }
    &.done .dot {
      background: $cm-primary;
    }
    .step-body {
      margin-left: 20rpx;
    }
    .step-title {
      display: block;
      font-size: 25rpx;
      color: $cm-text-3;
    }
    &.done .step-title {
      color: $cm-text;
      font-weight: 500;
    }
    .step-time {
      display: block;
      margin-top: 4rpx;
      font-size: 20rpx;
      color: $cm-text-4;
    }
  }
}
.bottom-space {
  height: 140rpx;
}
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 16rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid $cm-border;
  .btn {
    margin-left: 16rpx;
    padding: 12rpx 30rpx;
    border-radius: 999rpx;
    font-size: 25rpx;
    &.primary {
      background: $cm-primary;
      color: #fff;
    }
    &.ghost {
      border: 1rpx solid $cm-border;
      color: $cm-text-2;
    }
  }
}
.mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99;
}
.comment-panel {
  width: 620rpx;
  background: #fff;
  border-radius: $cm-radius-lg;
  padding: 30rpx;
  .panel-title {
    display: block;
    font-size: 30rpx;
    font-weight: 600;
    color: $cm-text;
    text-align: center;
  }
  .stars {
    display: flex;
    justify-content: center;
    margin: 24rpx 0;
    .star {
      font-size: 52rpx;
      color: $cm-border;
      margin: 0 8rpx;
      &.active {
        color: #ff9f0a;
      }
    }
  }
  .comment-input {
    width: 100%;
    height: 180rpx;
    background: $cm-page-bg;
    border-radius: $cm-radius-sm;
    padding: 18rpx;
    box-sizing: border-box;
    font-size: 25rpx;
  }
  .comment-view-text {
    display: block;
    min-height: 80rpx;
    background: $cm-page-bg;
    border-radius: $cm-radius-sm;
    padding: 18rpx;
    box-sizing: border-box;
    font-size: 26rpx;
    color: $cm-text;
    line-height: 40rpx;
  }
  .comment-view-time {
    display: block;
    margin-top: 12rpx;
    font-size: 22rpx;
    color: $cm-text-4;
    text-align: right;
  }
  .comment-reply {
    margin-top: 18rpx;
    padding: 16rpx;
    background: rgba(43, 127, 255, 0.06);
    border-radius: $cm-radius-sm;
    .reply-label {
      display: block;
      font-size: 22rpx;
      color: $cm-primary;
      margin-bottom: 6rpx;
    }
    .reply-text {
      font-size: 24rpx;
      color: $cm-text-2;
      line-height: 36rpx;
    }
  }
  .panel-btns {
    display: flex;
    justify-content: space-between;
    margin-top: 24rpx;
    .cm-btn {
      width: 46%;
      height: 76rpx;
      line-height: 76rpx;
      font-size: 26rpx;
    }
  }
}
</style>
