<!-- 我的收藏 -->
<template>
  <view class="page">
    <view v-if="loading && !items.length" class="skeleton-wrap">
      <skeleton-box :rows="4" mode="row" />
    </view>

    <view v-else-if="items.length" class="list">
      <view v-for="item in items" :key="item.itemId" class="card">
        <item-card :item="item" mode="row" show-status @select="goDetail" />
        <view class="actions">
          <text class="price">{{ formatPrice(item.price) }}</text>
          <text class="cancel-btn" @tap="cancelFavorite(item)">取消收藏</text>
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
      icon="💗"
      text="还没有收藏商品"
      sub="看到心动的好物，点个爱心收藏起来"
      action-text="去逛逛"
      @action="goHome"
    />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import itemCard from '@/components/item-card.vue'
import emptyState from '@/components/empty-state.vue'
import skeletonBox from '@/components/skeleton-box.vue'
import { getMyFavorites, removeFavorite } from '@/api/favorite'
import { formatPrice } from '@/utils/format'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const items = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const finished = ref(false)
const current = ref(1)
const size = 10

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
    const res = await getMyFavorites({ current: current.value, size })
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

function goDetail(item) {
  uni.navigateTo({ url: `/pages/item/detail?itemId=${item.itemId}` })
}

async function cancelFavorite(item) {
  try {
    await removeFavorite(item.itemId)
    items.value = items.value.filter((i) => String(i.itemId) !== String(item.itemId))
    uni.showToast({ title: '已取消收藏', icon: 'none' })
  } catch (e) {
    /* 已统一 toast */
  }
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
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
.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 20rpx;
  border-top: 1rpx solid $cm-border;
  .price {
    color: $cm-danger;
    font-size: 28rpx;
    font-weight: 600;
  }
  .cancel-btn {
    padding: 8rpx 24rpx;
    border: 1rpx solid $cm-border;
    border-radius: 999rpx;
    font-size: 23rpx;
    color: $cm-text-2;
  }
}
.load-more {
  padding: 24rpx 0 40rpx;
  text-align: center;
  font-size: 22rpx;
  color: $cm-text-4;
}
</style>
