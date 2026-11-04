<!-- 我的发布：按状态筛选 + 编辑 / 下架 -->
<template>
  <view class="page">
    <scroll-view class="tabs" scroll-x>
      <view
        v-for="t in tabs"
        :key="t.value"
        class="tab"
        :class="{ active: activeTab === t.value }"
        @tap="changeTab(t.value)"
      >{{ t.label }}</view>
    </scroll-view>

    <view v-if="loading && !items.length" class="skeleton-wrap">
      <skeleton-box :rows="4" mode="row" />
    </view>

    <view v-else-if="items.length" class="list">
      <view v-for="item in items" :key="item.itemId" class="card">
        <item-card :item="item" mode="row" show-status @select="goDetail" />
        <view v-if="Number(item.status) === 6 && item.auditRemark" class="audit-remark">
          违规原因：{{ item.auditRemark }}
        </view>
        <view class="actions">
          <text class="view-btn" @tap="goDetail(item)">查看</text>
          <text v-if="canEdit(item)" class="action-btn" @tap="editItem(item)">编辑</text>
          <text v-if="canOffShelf(item)" class="action-btn" @tap="offShelf(item)">下架</text>
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
      icon="📤"
      text="还没有发布商品"
      sub="把闲置物品挂上来，让它找到新主人"
      action-text="去发布"
      @action="goPublish"
    />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import itemCard from '@/components/item-card.vue'
import emptyState from '@/components/empty-state.vue'
import skeletonBox from '@/components/skeleton-box.vue'
import { getMyItems, offShelfItem } from '@/api/item'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const tabs = [
  { label: '全部', value: '' },
  { label: '待审核', value: 1 },
  { label: '在售', value: 2 },
  { label: '已预订', value: 3 },
  { label: '已售出', value: 4 },
  { label: '已下架', value: 5 },
  { label: '违规', value: 6 }
]

const activeTab = ref('')
const items = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const finished = ref(false)
const current = ref(1)
const size = 10

function canEdit(item) {
  return [1, 2, 5].includes(Number(item.status))
}

function canOffShelf(item) {
  return [2, 3].includes(Number(item.status))
}

async function loadItems(reset = true) {
  if (reset) {
    current.value = 1
    finished.value = false
    loading.value = true
  } else {
    if (finished.value || loadingMore.value) return
    loadingMore.value = true
  }
  try {
    const params = { current: current.value, size }
    if (activeTab.value !== '') params.status = activeTab.value
    const res = await getMyItems(params)
    const records = (res && res.records) || []
    items.value = reset ? records : items.value.concat(records)
    const total = (res && res.total) || 0
    finished.value = records.length < size || items.value.length >= total
    current.value = reset ? 2 : current.value + 1
  } catch (e) {
    if (reset) items.value = []
    finished.value = true
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function changeTab(value) {
  activeTab.value = value
  loadItems(true)
}

function goDetail(item) {
  uni.navigateTo({ url: `/pages/item/detail?itemId=${item.itemId}` })
}

function editItem(item) {
  uni.setStorageSync('pendingEditItemId', String(item.itemId))
  uni.switchTab({ url: '/pages/item/publish' })
}

function offShelf(item) {
  uni.showModal({
    title: '确认下架',
    content: `确定下架「${item.title}」吗？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await offShelfItem(item.itemId)
        uni.showToast({ title: '已下架', icon: 'none' })
        loadItems(true)
      } catch (e) {
        /* 已统一 toast */
      }
    }
  })
}

function goPublish() {
  uni.switchTab({ url: '/pages/item/publish' })
}

onShow(() => {
  userStore.initFromStorage()
  if (!userStore.requireLogin()) return
  loadItems(true)
})

onPullDownRefresh(async () => {
  await loadItems(true)
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (!finished.value) loadItems(false)
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $cm-page-bg;
}
.tabs {
  white-space: nowrap;
  background: #fff;
  padding: 16rpx 12rpx;
  position: sticky;
  top: 0;
  z-index: 10;
  .tab {
    display: inline-block;
    padding: 10rpx 24rpx;
    margin: 0 8rpx;
    border-radius: 999rpx;
    font-size: 24rpx;
    color: $cm-text-2;
    &.active {
      background: $cm-primary;
      color: #fff;
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
  overflow: hidden;
}
.audit-remark {
  margin: 0 20rpx;
  padding: 12rpx 16rpx;
  background: #fff1f0;
  color: $cm-danger;
  font-size: 22rpx;
  border-radius: $cm-radius-sm;
}
.actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 16rpx 20rpx;
  border-top: 1rpx solid $cm-border;
  .view-btn {
    font-size: 24rpx;
    color: $cm-text-2;
    padding: 8rpx 24rpx;
  }
  .action-btn {
    margin-left: 16rpx;
    padding: 8rpx 26rpx;
    border: 1rpx solid $cm-primary;
    color: $cm-primary;
    border-radius: 999rpx;
    font-size: 24rpx;
  }
}
.load-more {
  padding: 24rpx 0 40rpx;
  text-align: center;
  font-size: 22rpx;
  color: $cm-text-4;
}
</style>
