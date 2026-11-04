<!-- 分类页：左侧一级分类树 + 右侧二级分类 + 商品列表 -->
<template>
  <view class="page">
    <view class="search-mini" @tap="goSearch">
      <text class="icon">🔍</text>
      <text class="placeholder">搜索校园二手好物</text>
    </view>

    <view class="body">
      <!-- 左侧一级分类 -->
      <scroll-view class="side" scroll-y>
        <view class="side-item" :class="{ active: !activeRoot }" @tap="selectRoot(null)">
          <text>全部</text>
        </view>
        <view
          v-for="c in categoryTree"
          :key="c.categoryId"
          class="side-item"
          :class="{ active: activeRoot && activeRoot.categoryId === c.categoryId }"
          @tap="selectRoot(c)"
        >
          <text>{{ c.categoryName }}</text>
        </view>
      </scroll-view>

      <!-- 右侧内容 -->
      <view class="main">
        <!-- 二级分类 chips -->
        <scroll-view v-if="subCategories.length" class="chips" scroll-x>
          <view class="chip" :class="{ active: !activeSubId }" @tap="selectSub(null)">全部</view>
          <view
            v-for="s in subCategories"
            :key="s.categoryId"
            class="chip"
            :class="{ active: activeSubId === String(s.categoryId) }"
            @tap="selectSub(s)"
          >{{ s.categoryName }}</view>
        </scroll-view>

        <!-- 结果条数：切换分类后能立刻看出列表变了 -->
        <view v-if="!loading" class="list-count">
          <text>{{ loadedTotal }} 件商品</text>
          <text v-if="activeRoot" class="list-count-cur">· {{ activeRoot.categoryName }}</text>
          <text v-if="activeSubName" class="list-count-cur">/ {{ activeSubName }}</text>
        </view>

        <scroll-view class="list" scroll-y @scrolltolower="loadMore">
          <view v-if="loading && !items.length">
            <skeleton-box :rows="4" mode="grid" />
          </view>
          <view v-else-if="items.length" class="grid">
            <view v-for="item in items" :key="item.itemId" class="grid-cell">
              <item-card :item="item" @select="goDetail" />
            </view>
          </view>
          <empty-state v-else icon="📦" text="该分类下暂无商品" sub="换个分类看看～" />
          <view v-if="items.length" class="load-more">
            <text v-if="loadingMore">加载中…</text>
            <text v-else-if="finished">— 没有更多了 —</text>
            <text v-else>上拉加载更多</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import itemCard from '@/components/item-card.vue'
import emptyState from '@/components/empty-state.vue'
import skeletonBox from '@/components/skeleton-box.vue'
import { getCategoryTree, getItemPage } from '@/api/item'

const categoryTree = ref([])
const activeRoot = ref(null)
const activeSubId = ref('')
const items = ref([])
const loadedTotal = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const finished = ref(false)
const current = ref(1)
const size = 10

const subCategories = computed(() => (activeRoot.value && activeRoot.value.children) || [])
const activeSubName = computed(() => {
  const hit = subCategories.value.find((s) => String(s.categoryId) === activeSubId.value)
  return hit ? hit.categoryName : ''
})

async function loadCategories() {
  try {
    const res = await getCategoryTree()
    categoryTree.value = Array.isArray(res) ? res : (res && res.records) || []
  } catch (e) {
    categoryTree.value = []
  }
}

/**
 * 当前要查询的分类 id（单个）。
 * 后端 `pageAppItems` 对一级分类会自动包含其所有子分类：
 *   categoryId=1  → 28 件（手机 8 + 电脑 5 + 配件 7 + 平板 4 + 耳机 4）
 *   categoryId=101→ 8 件（只有手机）
 * 所以这里只需要把「选中的一级或二级 id」原样传给后端，不要自己拼子分类再合并，
 * 否则分页与排序都会错位（这也是上一版各二级分类看起来"一样"的原因）。
 */
function currentCategoryId() {
  if (activeSubId.value) return Number(activeSubId.value)
  if (activeRoot.value) return Number(activeRoot.value.categoryId)
  return null
}

/** 取第 pageNo 页数据 */
async function fetchPage(pageNo) {
  const categoryId = currentCategoryId()
  const params = { current: pageNo, size, sort: 'new' }
  if (categoryId) params.categoryId = categoryId
  const res = await getItemPage(params)
  return { records: (res && res.records) || [], total: Number((res && res.total) || 0) }
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
    const { records, total } = await fetchPage(current.value)
    loadedTotal.value = total
    items.value = reset ? records : items.value.concat(records)
    finished.value = !records.length || records.length < size || items.value.length >= total
    if (!reset) current.value += 1
    else current.value = 2
  } catch (e) {
    if (reset) items.value = []
    finished.value = true
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function selectRoot(category) {
  activeRoot.value = category
  activeSubId.value = ''
  loadItems(true)
}

function selectSub(sub) {
  activeSubId.value = sub ? String(sub.categoryId) : ''
  loadItems(true)
}

function loadMore() {
  if (!finished.value) loadItems(false)
}

function goDetail(item) {
  uni.navigateTo({ url: `/pages/item/detail?itemId=${item.itemId}` })
}

function goSearch() {
  uni.navigateTo({ url: '/pages/search/search' })
}

onShow(async () => {
  // 首页金刚区跳转时通过本地缓存传一级分类 id
  const pending = uni.getStorageSync('pendingCategoryId')
  if (pending) {
    uni.removeStorageSync('pendingCategoryId')
  }
  if (!categoryTree.value.length) await loadCategories()
  if (pending) {
    const target = categoryTree.value.find((c) => String(c.categoryId) === String(pending))
    if (target) {
      activeRoot.value = target
      activeSubId.value = ''
    }
  }
  loadItems(true)
})

onPullDownRefresh(async () => {
  await loadCategories()
  await loadItems(true)
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $cm-page-bg;
}
.search-mini {
  display: flex;
  align-items: center;
  margin: 20rpx 24rpx;
  height: 70rpx;
  background: #fff;
  border-radius: 999rpx;
  padding: 0 24rpx;
  .icon {
    margin-right: 10rpx;
    font-size: 26rpx;
  }
  .placeholder {
    font-size: 25rpx;
    color: $cm-text-3;
  }
}
.body {
  flex: 1;
  display: flex;
  overflow: hidden;
}
.side {
  width: 190rpx;
  flex: none;
  background: #f0f2f5;
  .side-item {
    padding: 30rpx 16rpx;
    font-size: 26rpx;
    color: $cm-text-2;
    text-align: center;
    border-left: 6rpx solid transparent;
    &.active {
      background: #fff;
      color: $cm-primary;
      font-weight: 600;
      border-left-color: $cm-primary;
    }
  }
}
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
}
.chips {
  white-space: nowrap;
  padding: 16rpx 16rpx 8rpx;
  flex: none;
  .chip {
    display: inline-block;
    padding: 8rpx 22rpx;
    margin-right: 12rpx;
    border-radius: 999rpx;
    background: $cm-page-bg;
    color: $cm-text-2;
    font-size: 22rpx;
    &.active {
      background: $cm-primary;
      color: #fff;
    }
  }
}
.list-count {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 0 20rpx 10rpx;
  font-size: 22rpx;
  color: $cm-text-3;
  .list-count-cur {
    color: $cm-text-4;
  }
}
.list {
  flex: 1;
  overflow: hidden;
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
