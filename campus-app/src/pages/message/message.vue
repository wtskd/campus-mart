<!-- 消息中心：系统/订单/审核通知 + AI 智能客服 -->
<template>
  <view class="page">
    <!-- 顶部分段 -->
    <view class="seg">
      <view class="seg-item" :class="{ active: tab === 'message' }" @tap="switchTab('message')">
        消息
        <text v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</text>
      </view>
      <view class="seg-item" :class="{ active: tab === 'ai' }" @tap="switchTab('ai')">AI 客服</view>
    </view>

    <!-- 消息列表 -->
    <block v-if="tab === 'message'">
      <view v-if="unreadCount > 0" class="read-all" @tap="readAll">
        <text>全部标为已读</text>
      </view>

      <view v-if="loading && !list.length" class="skeleton-wrap">
        <skeleton-box :rows="4" mode="row" />
      </view>

      <view v-else-if="list.length" class="list">
        <view
          v-for="m in list"
          :key="m.messageId"
          class="msg-item"
          :class="{ unread: !Number(m.isRead) }"
          @tap="openMessage(m)"
        >
          <view class="icon-box" :style="{ background: iconStyle(m).bg }">
            <text class="icon">{{ iconStyle(m).icon }}</text>
          </view>
          <view class="body">
            <view class="head">
              <text class="title">{{ m.title || msgTypeInfo(m.msgType).text }}</text>
              <text class="time">{{ timeAgo(m.createTime) }}</text>
            </view>
            <text class="content">{{ m.content }}</text>
          </view>
          <text v-if="!Number(m.isRead)" class="dot"></text>
        </view>
        <view class="load-more">
          <text v-if="loadingMore">加载中…</text>
          <text v-else-if="finished">— 没有更多了 —</text>
          <text v-else>上拉加载更多</text>
        </view>
      </view>

      <empty-state
        v-else
        icon="📭"
        text="暂无消息"
        sub="订单、审核等通知都会出现在这里"
        action-text="问问 AI 客服"
        @action="switchTab('ai')"
      />
    </block>

    <!-- AI 客服 -->
    <view v-else class="ai-wrap">
      <ai-chat
        title="校园易站 AI 客服"
        height="calc(100vh - 200rpx)"
        welcome="你好呀，我是校园易站 AI 客服 🤖 可以帮你了解发布流程、交易安全、订单处理等问题。"
        :suggestions="aiSuggestions"
        placeholder="输入你的问题，如：怎么发布闲置？"
      />
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import skeletonBox from '@/components/skeleton-box.vue'
import emptyState from '@/components/empty-state.vue'
import aiChat from '@/components/ai-chat.vue'
import { getMyMessages, getUnreadCount, readAllMessages, readMessage } from '@/api/message'
import { timeAgo, msgTypeInfo } from '@/utils/format'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const tab = ref('message')
const list = ref([])
const unreadCount = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const finished = ref(false)
const current = ref(1)
const size = 15

const aiSuggestions = [
  '怎么发布二手闲置？',
  '平台的交易流程是怎样的？',
  '订单可以取消吗？',
  '如何避免交易纠纷？'
]

const ICONS = {
  1: { icon: '📢', bg: '#e8f1ff' },
  2: { icon: '🧾', bg: '#fff5e6' },
  3: { icon: '💬', bg: '#e9f9f0' },
  4: { icon: '🤖', bg: '#f3eeff' }
}

const FALLBACK_ICON = { icon: '🔔', bg: '#eef0f3' }

function iconStyle(m) {
  return ICONS[Number(m.msgType)] || FALLBACK_ICON
}

async function loadUnread() {
  try {
    const res = await getUnreadCount()
    // 兼容返回 {count} 或直接数字
    unreadCount.value = typeof res === 'number' ? res : (res && (res.count || res.unreadCount)) || 0
  } catch (e) {
    unreadCount.value = 0
  }
}

async function loadMessages(reset = true) {
  if (reset) {
    current.value = 1
    finished.value = false
    loading.value = true
  } else {
    if (finished.value || loadingMore.value) return
    loadingMore.value = true
  }
  try {
    const res = await getMyMessages({ current: current.value, size })
    const records = (res && res.records) || []
    list.value = reset ? records : list.value.concat(records)
    const total = (res && res.total) || 0
    finished.value = records.length < size || list.value.length >= total
    current.value = reset ? 2 : current.value + 1
  } catch (e) {
    if (reset) list.value = []
    finished.value = true
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function switchTab(value) {
  tab.value = value
  if (value === 'message') {
    loadMessages(true)
    loadUnread()
  }
}

function openMessage(m) {
  if (!Number(m.isRead)) {
    readMessage(m.messageId)
      .then(() => {
        m.isRead = 1
        loadUnread()
      })
      .catch(() => {})
  }
  // 订单类消息跳订单详情，审核类跳我的发布，AI 类切到 AI 客服
  const type = Number(m.msgType)
  if (type === 2 && m.bizId) {
    uni.navigateTo({ url: `/pages/order/detail?orderId=${m.bizId}` })
  } else if (type === 3) {
    uni.navigateTo({ url: '/pages/item/my' })
  } else if (type === 4) {
    tab.value = 'ai'
  } else if (m.bizId) {
    uni.navigateTo({ url: `/pages/item/detail?itemId=${m.bizId}` })
  }
}

async function readAll() {
  try {
    await readAllMessages()
    list.value.forEach((m) => {
      m.isRead = 1
    })
    unreadCount.value = 0
    uni.showToast({ title: '已全部标为已读', icon: 'none' })
  } catch (e) {
    /* 已统一 toast */
  }
}

onShow(() => {
  userStore.initFromStorage()
  if (!userStore.requireLogin()) return
  const pending = uni.getStorageSync('cm_message_tab')
  if (pending) {
    uni.removeStorageSync('cm_message_tab')
    tab.value = 'ai'
    loadUnread()
    return
  }
  loadMessages(true)
  loadUnread()
})

onPullDownRefresh(async () => {
  await Promise.all([loadMessages(true), loadUnread()])
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (tab.value === 'message' && !finished.value) loadMessages(false)
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $cm-page-bg;
  display: flex;
  flex-direction: column;
}
.seg {
  display: flex;
  background: #fff;
  .seg-item {
    flex: 1;
    text-align: center;
    padding: 26rpx 0;
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
        width: 60rpx;
        height: 6rpx;
        background: $cm-primary;
        border-radius: 4rpx;
      }
    }
    .badge {
      display: inline-block;
      min-width: 32rpx;
      height: 32rpx;
      line-height: 32rpx;
      padding: 0 8rpx;
      margin-left: 8rpx;
      background: $cm-danger;
      color: #fff;
      font-size: 18rpx;
      border-radius: 999rpx;
      vertical-align: top;
    }
  }
}
.read-all {
  display: flex;
  justify-content: flex-end;
  padding: 16rpx 24rpx 0;
  font-size: 23rpx;
  color: $cm-primary;
}
.skeleton-wrap {
  padding: 24rpx;
}
.list {
  padding: 16rpx 24rpx;
}
.msg-item {
  display: flex;
  align-items: flex-start;
  padding: 24rpx;
  background: #fff;
  border-radius: $cm-radius;
  margin-bottom: 16rpx;
  position: relative;
  .icon-box {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
  }
  .icon {
    font-size: 38rpx;
  }
  .body {
    flex: 1;
    margin-left: 18rpx;
    overflow: hidden;
  }
  .head {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .title {
    font-size: 27rpx;
    color: $cm-text;
    font-weight: 500;
  }
  .time {
    font-size: 20rpx;
    color: $cm-text-4;
    flex: none;
    margin-left: 12rpx;
  }
  .content {
    display: block;
    margin-top: 8rpx;
    font-size: 24rpx;
    color: $cm-text-2;
    line-height: 36rpx;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .dot {
    position: absolute;
    right: 18rpx;
    bottom: 26rpx;
    width: 14rpx;
    height: 14rpx;
    background: $cm-danger;
    border-radius: 50%;
  }
}
.load-more {
  padding: 24rpx 0 40rpx;
  text-align: center;
  font-size: 22rpx;
  color: $cm-text-4;
}
.ai-wrap {
  flex: 1;
  overflow: hidden;
}
</style>
