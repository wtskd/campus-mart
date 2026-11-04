<!-- 首页：搜索 + 公告 + 轮播 + 分类金刚区 + AI 个性化推荐 -->
<template>
  <view class="page">
    <!-- 顶部搜索区 -->
    <view class="header">
      <view class="brand">
        <text class="logo">🛒</text>
        <view class="brand-text">
          <text class="name">校园易站</text>
          <text class="slogan">让闲置在校园里流动起来</text>
        </view>
      </view>
      <view class="search-box" @tap="goSearch">
        <text class="search-icon">🔍</text>
        <text class="search-placeholder">搜索教材、数码、宿舍神器…</text>
      </view>
    </view>

    <!-- 公告条 -->
    <view v-if="notices.length" class="notice-bar">
      <text class="notice-tag">公告</text>
      <swiper class="notice-swiper" vertical circular autoplay :interval="3500" :duration="400">
        <swiper-item v-for="n in notices" :key="n.noticeId" @tap="openNotice(n)">
          <text class="notice-text">{{ n.title }}</text>
        </swiper-item>
      </swiper>
      <text class="notice-arrow">›</text>
    </view>

    <!-- 轮播图 -->
    <view v-if="banners.length" class="banner-wrap">
      <swiper class="banner" circular autoplay :interval="4000" :duration="500" indicator-dots indicator-active-color="#ffffff">
        <swiper-item v-for="b in banners" :key="b.bannerId" @tap="openBanner(b)">
          <image class="banner-img" :src="mediaUrl(b.imageUrl)" mode="aspectFill" lazy-load />
        </swiper-item>
      </swiper>
    </view>

    <!-- 分类金刚区 -->
    <view class="cate-box">
      <view v-for="c in categories" :key="c.categoryId" class="cate-item" @tap="goCategory(c)">
        <view class="cate-icon">{{ categoryIcon(c) }}</view>
        <text class="cate-name">{{ c.categoryName }}</text>
      </view>
    </view>

    <!-- AI 推荐 -->
    <view class="section-head">
      <view class="left">
        <text class="section-title">✨ AI 猜你喜欢</text>
        <text class="section-sub">{{ recommendTip }}</text>
      </view>
      <text class="more" @tap="goCategory(null)">全部分类 ›</text>
    </view>

    <!-- 骨架屏 -->
    <view v-if="loading && !items.length" class="grid">
      <skeleton-box :rows="4" mode="grid" />
    </view>

    <!-- 双列瀑布流 -->
    <view v-else-if="items.length" class="grid">
      <view v-for="item in items" :key="item.itemId" class="grid-cell">
        <item-card :item="item" @select="goDetail" @favorite="onFavorite" />
      </view>
    </view>

    <empty-state
      v-else
      icon="🛍️"
      text="还没有推荐商品"
      sub="下拉刷新试试，或去分类里逛逛"
      action-text="刷新看看"
      @action="refresh"
    />

    <view v-if="items.length" class="load-more">
      <text v-if="loadingMore">加载中…</text>
      <text v-else-if="finished">— 没有更多了 —</text>
      <text v-else>上拉加载更多</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import itemCard from '@/components/item-card.vue'
import emptyState from '@/components/empty-state.vue'
import skeletonBox from '@/components/skeleton-box.vue'
import { getBannerList, getCategoryTree, getItemPage, getNoticeList, getRecommend } from '@/api/item'
import { aiRecommend } from '@/api/ai'
import { addFavorite, removeFavorite } from '@/api/favorite'
import { mediaUrl } from '@/utils/format'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const banners = ref([])
const notices = ref([])
const categories = ref([])
const items = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const finished = ref(false)
const current = ref(1)
const size = 10
const recommendTip = ref('根据你的浏览与收藏智能推荐')
const aiPicked = ref(false)

/** 分类图标：库里存的是英文 key（phone/book…），映射成 emoji；已是 emoji 就直接用 */
const CATEGORY_ICONS = {
  phone: '📱',
  laptop: '💻',
  computer: '💻',
  education: '📚',
  book: '📖',
  books: '📚',
  shopping: '🧴',
  life: '🧺',
  run: '🏃',
  sport: '🏀',
  star: '✨',
  beauty: '💄',
  home: '🛏️',
  furniture: '🪑',
  music: '🎸',
  game: '🎮',
  more: '📦',
  other: '📦',
  bike: '🚲',
  camp: '⛺'
}

function categoryIcon(category) {
  const icon = (category && category.icon) || ''
  if (!icon) return '📦'
  if (CATEGORY_ICONS[icon]) return CATEGORY_ICONS[icon]
  // 已经是 emoji（非 ASCII）就直接展示
  return /[^\x00-\x7F]/.test(icon) ? icon : '📦'
}

/** 首屏加载：轮播 / 公告 / 分类 / 推荐（并行，单个失败不影响其它） */
async function loadBase() {
  const tasks = [
    getBannerList()
      .then((res) => {
        banners.value = (res && res.records) || res || []
      })
      .catch(() => {}),
    getNoticeList({ current: 1, size: 5 })
      .then((res) => {
        notices.value = (res && res.records) || res || []
      })
      .catch(() => {}),
    getCategoryTree()
      .then((res) => {
        categories.value = (Array.isArray(res) ? res : (res && res.records) || []).slice(0, 8)
      })
      .catch(() => {})
  ]
  await Promise.all(tasks)
}

/** 推荐商品：先 AI 个性化，失败/为空回退商品热度推荐 */
async function loadRecommend(reset = true) {
  if (reset) {
    current.value = 1
    finished.value = false
  }
  if (loading.value || loadingMore.value) return

  if (reset && !items.value.length) loading.value = true
  else loadingMore.value = true

  try {
    if (reset) {
      let list = []
      try {
        const aiRes = await aiRecommend(8)
        list = (aiRes && (aiRes.records || aiRes.list || aiRes)) || []
        if (Array.isArray(list) && list.length) {
          aiPicked.value = true
          recommendTip.value = 'AI 依据你的浏览与收藏生成'
        }
      } catch (e) {
        list = []
      }
      if (!Array.isArray(list) || !list.length) {
        aiPicked.value = false
        recommendTip.value = '热销与最新上架的校园好物'
        const res = await getRecommend(8)
        list = (res && (res.records || res)) || []
      }
      items.value = Array.isArray(list) ? list : []
      current.value = 1
    } else {
      const res = await getItemPage({ current: current.value, size, sort: 'hot' })
      const records = (res && res.records) || []
      // 按 itemId 去重，避免与 AI 推荐重复
      const exists = new Set(items.value.map((i) => i.itemId))
      items.value = items.value.concat(records.filter((r) => !exists.has(r.itemId)))
    }
    const total = items.value.length
    finished.value = !aiPicked.value && total < 8
    if (!finished.value && !aiPicked.value) current.value += 1
  } catch (e) {
    finished.value = true
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function refresh() {
  await Promise.all([loadBase(), loadRecommend(true)])
}

function goSearch() {
  uni.navigateTo({ url: '/pages/search/search' })
}

function goCategory(category) {
  if (category) uni.setStorageSync('pendingCategoryId', String(category.categoryId))
  uni.switchTab({ url: '/pages/category/category' })
}

function goDetail(item) {
  uni.navigateTo({ url: `/pages/item/detail?itemId=${item.itemId}` })
}

function openNotice(notice) {
  uni.navigateTo({ url: `/pages/message/message?noticeId=${notice.noticeId}` })
}

/** tabBar 页面只能用 switchTab，用 navigateTo 会静默失败 */
const TAB_PAGES = [
  '/pages/index/index',
  '/pages/category/category',
  '/pages/item/publish',
  '/pages/message/message',
  '/pages/user/user'
]

/** 站内页面跳转：tabBar 页走 switchTab，其余走 navigateTo，保留 query */
function goPage(url) {
  const [rawPath, query = ''] = String(url).split('?')
  const path = rawPath.replace(/\/+$/, '')
  if (TAB_PAGES.indexOf(path) >= 0) {
    // 分类页通过本地缓存接收一级分类 id
    const hit = /(?:^|&)categoryId=(\d+)/.exec(query)
    if (path === '/pages/category/category' && hit) {
      uni.setStorageSync('pendingCategoryId', hit[1])
    }
    uni.switchTab({ url: path })
    return
  }
  uni.navigateTo({ url })
}

/**
 * 轮播点击跳转，link_url 约定（后台「跳转地址」按此填写）：
 *   item:1234            → 商品详情
 *   category:2           → 分类页并选中该一级分类
 *   search:教材           → 搜索页并带上关键词
 *   /pages/category/category?categoryId=2  → 站内页面（tab 页自动 switchTab）
 *   留空                 → 不跳转
 */
function openBanner(banner) {
  const link = banner && banner.linkUrl ? String(banner.linkUrl).trim() : ''
  if (!link) return
  if (link.indexOf('item:') === 0) {
    const id = link.slice(5).trim()
    if (id) return uni.navigateTo({ url: `/pages/item/detail?itemId=${id}` })
  }
  if (link.indexOf('category:') === 0) {
    const id = link.slice(9).trim()
    if (id) {
      uni.setStorageSync('pendingCategoryId', id)
      return uni.switchTab({ url: '/pages/category/category' })
    }
  }
  if (link.indexOf('search:') === 0) {
    const keyword = link.slice(7).trim()
    return uni.navigateTo({ url: `/pages/search/search?keyword=${encodeURIComponent(keyword)}` })
  }
  if (link.indexOf('/pages/') === 0) {
    return goPage(link)
  }
  uni.showToast({ title: banner.title || '活动详情', icon: 'none' })
}

/** 收藏 / 取消收藏（未登录先去登录） */
async function onFavorite(item) {
  if (!userStore.requireLogin()) return
  try {
    const next = !item.favorited
    if (next) await addFavorite(item.itemId)
    else await removeFavorite(item.itemId)
    item.favorited = next
    item.favoriteCount = Math.max(0, Number(item.favoriteCount || 0) + (next ? 1 : -1))
    uni.showToast({ title: next ? '已收藏' : '已取消收藏', icon: 'none' })
  } catch (e) {
    /* request 已统一 toast */
  }
}

onLoad(() => {
  refresh()
})

onShow(() => {
  userStore.initFromStorage()
})

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (finished.value || loadingMore.value || loading.value) return
  loadRecommend(false)
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $cm-page-bg;
  padding-bottom: 40rpx;
}

.header {
  padding: 30rpx 24rpx 36rpx;
  background: linear-gradient(135deg, #2b7fff 0%, #6aa8ff 100%);
  border-bottom-left-radius: 32rpx;
  border-bottom-right-radius: 32rpx;

  .brand {
    display: flex;
    align-items: center;
    margin-bottom: 22rpx;
  }
  .logo {
    font-size: 48rpx;
    margin-right: 14rpx;
  }
  .name {
    display: block;
    color: #fff;
    font-size: 34rpx;
    font-weight: 600;
    letter-spacing: 1rpx;
  }
  .slogan {
    display: block;
    color: rgba(255, 255, 255, 0.85);
    font-size: 20rpx;
    margin-top: 2rpx;
  }
  .search-box {
    display: flex;
    align-items: center;
    height: 76rpx;
    background: #fff;
    border-radius: 999rpx;
    padding: 0 26rpx;
  }
  .search-icon {
    font-size: 28rpx;
    margin-right: 12rpx;
  }
  .search-placeholder {
    font-size: 26rpx;
    color: $cm-text-3;
  }
}

.notice-bar {
  display: flex;
  align-items: center;
  margin: 20rpx 24rpx 0;
  padding: 16rpx 20rpx;
  background: #fff;
  border-radius: $cm-radius;
  .notice-tag {
    flex: none;
    padding: 2rpx 12rpx;
    background: $cm-primary-light;
    color: $cm-primary;
    border-radius: 8rpx;
    font-size: 20rpx;
    margin-right: 16rpx;
  }
  .notice-swiper {
    flex: 1;
    height: 40rpx;
  }
  .notice-text {
    font-size: 24rpx;
    color: $cm-text-2;
    line-height: 40rpx;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  .notice-arrow {
    color: $cm-text-4;
    font-size: 28rpx;
  }
}

.banner-wrap {
  margin: 20rpx 24rpx 0;
}
.banner {
  height: 300rpx;
  border-radius: $cm-radius;
  overflow: hidden;
  .banner-img {
    width: 100%;
    height: 300rpx;
    background: #e8f1ff;
  }
}

.cate-box {
  display: flex;
  flex-wrap: wrap;
  margin: 20rpx 24rpx 0;
  padding: 20rpx 4rpx;
  background: #fff;
  border-radius: $cm-radius;
  .cate-item {
    width: 25%;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 12rpx 0;
  }
  .cate-icon {
    width: 84rpx;
    height: 84rpx;
    border-radius: 50%;
    background: $cm-primary-light;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40rpx;
  }
  .cate-name {
    margin-top: 10rpx;
    font-size: 22rpx;
    color: $cm-text-2;
  }
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 32rpx 24rpx 16rpx;
  .section-title {
    font-size: 32rpx;
    font-weight: 600;
    color: $cm-text;
  }
  .section-sub {
    display: block;
    font-size: 20rpx;
    color: $cm-text-3;
    margin-top: 4rpx;
  }
  .more {
    font-size: 24rpx;
    color: $cm-primary;
  }
}

.grid {
  display: flex;
  flex-wrap: wrap;
  padding: 0 16rpx;
  .grid-cell {
    width: 50%;
    padding: 8rpx;
    box-sizing: border-box;
  }
}

.load-more {
  padding: 30rpx 0;
  text-align: center;
  font-size: 22rpx;
  color: $cm-text-4;
}
</style>
