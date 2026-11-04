<!-- 搜索页：关键词 + 排序（最新/价格/热度）+ 价格区间 + 结果双列流 -->
<template>
  <view class="page">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="input-wrap">
        <text class="icon">🔍</text>
        <input
          v-model="keyword"
          class="input"
          placeholder="搜索商品关键词"
          confirm-type="search"
          @confirm="doSearch"
        />
        <text v-if="keyword" class="clear" @tap="clearKeyword">✕</text>
      </view>
      <text class="search-btn" @tap="doSearch">搜索</text>
    </view>

    <!-- 排序 + 价格筛选 -->
    <view class="toolbar">
      <view class="sort-tabs">
        <text class="tab" :class="{ active: sort === 'new' }" @tap="changeSort('new')">最新</text>
        <text class="tab" :class="{ active: sort === 'priceAsc' || sort === 'priceDesc' }" @tap="togglePrice">
          价格
          <text class="arrow">{{ sort === 'priceAsc' ? '↑' : sort === 'priceDesc' ? '↓' : '↕' }}</text>
        </text>
        <text class="tab" :class="{ active: sort === 'hot' }" @tap="changeSort('hot')">热度</text>
      </view>
      <text class="filter" @tap="pricePanel = !pricePanel">价格区间 {{ priceText }}</text>
    </view>

    <!-- 价格区间面板 -->
    <view v-if="pricePanel" class="price-panel">
      <input v-model="minPrice" class="price-input" type="digit" placeholder="最低价" />
      <text class="dash">—</text>
      <input v-model="maxPrice" class="price-input" type="digit" placeholder="最高价" />
      <view class="panel-btns">
        <view class="cm-btn cm-btn-ghost" @tap="resetPrice">重置</view>
        <view class="cm-btn cm-btn-primary" @tap="applyPrice">确定</view>
      </view>
    </view>

    <!-- 搜索历史 -->
    <view v-if="!searched && history.length" class="history">
      <view class="history-head">
        <text class="title">搜索历史</text>
        <text class="clear-history" @tap="clearHistory">清空</text>
      </view>
      <view class="history-list">
        <text v-for="(h, i) in history" :key="i" class="history-item" @tap="pickHistory(h)">{{ h }}</text>
      </view>
    </view>

    <!-- 结果 -->
    <view v-if="searched" class="result">
      <view v-if="loading && !items.length">
        <skeleton-box :rows="4" mode="grid" />
      </view>
      <view v-else-if="items.length" class="grid">
        <view v-for="item in items" :key="item.itemId" class="grid-cell">
          <item-card :item="item" @select="goDetail" @favorite="onFavorite" />
        </view>
      </view>
      <empty-state
        v-else
        icon="🔍"
        text="没有找到相关商品"
        sub="换个关键词或放宽价格区间试试"
        action-text="返回首页"
        @action="goHome"
      />
      <view v-if="items.length" class="load-more">
        <text v-if="loadingMore">加载中…</text>
        <text v-else-if="finished">— 没有更多了 —</text>
        <text v-else>上拉加载更多</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import itemCard from '@/components/item-card.vue'
import emptyState from '@/components/empty-state.vue'
import skeletonBox from '@/components/skeleton-box.vue'
import { getItemPage } from '@/api/item'
import { addFavorite, removeFavorite } from '@/api/favorite'
import { useUserStore } from '@/store/user'

const HISTORY_KEY = 'cm_search_history'
const userStore = useUserStore()

const keyword = ref('')
const sort = ref('new')
const minPrice = ref('')
const maxPrice = ref('')
const pricePanel = ref(false)
const appliedMin = ref('')
const appliedMax = ref('')
const items = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const finished = ref(false)
const searched = ref(false)
const current = ref(1)
const size = 10
const history = ref([])

const priceText = computed(() => {
  if (!appliedMin.value && !appliedMax.value) return ''
  return `¥${appliedMin.value || 0}-${appliedMax.value || '不限'}`
})

function loadHistory() {
  try {
    history.value = uni.getStorageSync(HISTORY_KEY) || []
  } catch (e) {
    history.value = []
  }
}

function saveHistory(word) {
  if (!word) return
  const list = [word].concat(history.value.filter((h) => h !== word)).slice(0, 10)
  history.value = list
  uni.setStorageSync(HISTORY_KEY, list)
}

function clearHistory() {
  history.value = []
  uni.removeStorageSync(HISTORY_KEY)
}

function clearKeyword() {
  keyword.value = ''
  items.value = []
  searched.value = false
}

function pickHistory(word) {
  keyword.value = word
  doSearch()
}

function changeSort(value) {
  sort.value = value
  doSearch()
}

function togglePrice() {
  sort.value = sort.value === 'priceAsc' ? 'priceDesc' : 'priceAsc'
  doSearch()
}

function resetPrice() {
  minPrice.value = ''
  maxPrice.value = ''
  appliedMin.value = ''
  appliedMax.value = ''
  doSearch()
}

function applyPrice() {
  const min = Number(minPrice.value)
  const max = Number(maxPrice.value)
  if (minPrice.value && maxPrice.value && min > max) {
    uni.showToast({ title: '最低价不能大于最高价', icon: 'none' })
    return
  }
  appliedMin.value = minPrice.value
  appliedMax.value = maxPrice.value
  pricePanel.value = false
  doSearch()
}

async function loadPage(reset = true) {
  if (reset) {
    current.value = 1
    finished.value = false
    loading.value = true
  } else {
    if (finished.value || loadingMore.value) return
    loadingMore.value = true
  }
  try {
    const params = { current: current.value, size, sort: sort.value }
    if (keyword.value) params.keyword = keyword.value
    if (appliedMin.value) params.minPrice = appliedMin.value
    if (appliedMax.value) params.maxPrice = appliedMax.value
    const res = await getItemPage(params)
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

function doSearch() {
  searched.value = true
  if (keyword.value) saveHistory(keyword.value)
  loadPage(true)
}

async function onFavorite(item) {
  if (!userStore.requireLogin()) return
  try {
    const next = !item.favorited
    if (next) await addFavorite(item.itemId)
    else await removeFavorite(item.itemId)
    item.favorited = next
    uni.showToast({ title: next ? '已收藏' : '已取消收藏', icon: 'none' })
  } catch (e) {
    /* 已统一 toast */
  }
}

function goDetail(item) {
  uni.navigateTo({ url: `/pages/item/detail?itemId=${item.itemId}` })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

onLoad((options) => {
  loadHistory()
  if (options && options.keyword) {
    keyword.value = decodeURIComponent(options.keyword)
    doSearch()
  }
})

onReachBottom(() => {
  if (searched.value && !finished.value) loadPage(false)
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $cm-page-bg;
}
.search-bar {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #fff;
  .input-wrap {
    flex: 1;
    display: flex;
    align-items: center;
    height: 72rpx;
    background: $cm-page-bg;
    border-radius: 999rpx;
    padding: 0 24rpx;
  }
  .icon {
    margin-right: 10rpx;
    font-size: 26rpx;
  }
  .input {
    flex: 1;
    font-size: 26rpx;
  }
  .clear {
    color: $cm-text-4;
    font-size: 24rpx;
    padding: 0 8rpx;
  }
  .search-btn {
    margin-left: 20rpx;
    color: $cm-primary;
    font-size: 28rpx;
  }
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid $cm-border;
  .sort-tabs {
    display: flex;
  }
  .tab {
    font-size: 26rpx;
    color: $cm-text-2;
    margin-right: 32rpx;
    &.active {
      color: $cm-primary;
      font-weight: 600;
    }
  }
  .arrow {
    font-size: 20rpx;
  }
  .filter {
    font-size: 24rpx;
    color: $cm-text-3;
  }
}
.price-panel {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid $cm-border;
  .price-input {
    flex: 1;
    height: 64rpx;
    background: $cm-page-bg;
    border-radius: 12rpx;
    padding: 0 20rpx;
    font-size: 24rpx;
  }
  .dash {
    margin: 0 16rpx;
    color: $cm-text-4;
  }
  .panel-btns {
    display: flex;
    margin-left: 16rpx;
    .cm-btn {
      padding: 0 22rpx;
      height: 64rpx;
      line-height: 64rpx;
      font-size: 24rpx;
      margin-left: 12rpx;
    }
  }
}
.history {
  padding: 28rpx 24rpx;
  .history-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .title {
    font-size: 28rpx;
    color: $cm-text;
  }
  .clear-history {
    font-size: 22rpx;
    color: $cm-text-3;
  }
  .history-list {
    display: flex;
    flex-wrap: wrap;
    margin-top: 20rpx;
  }
  .history-item {
    padding: 10rpx 24rpx;
    background: #fff;
    border-radius: 999rpx;
    font-size: 24rpx;
    color: $cm-text-2;
    margin: 0 16rpx 16rpx 0;
  }
}
.grid {
  display: flex;
  flex-wrap: wrap;
  padding: 8rpx;
  .grid-cell {
    width: 50%;
    padding: 8rpx;
    box-sizing: border-box;
  }
}
.load-more {
  padding: 24rpx 0 40rpx;
  text-align: center;
  font-size: 22rpx;
  color: $cm-text-4;
}
</style>
