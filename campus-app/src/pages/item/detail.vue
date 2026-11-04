<!-- 商品详情：轮播 / 价格成色 / 卖家 / 评论 / 收藏 / AI 问答 / 下单入口 -->
<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <skeleton-box :rows="1" mode="grid" />
      <view class="sk-lines">
        <view class="sk-line" style="width: 60%"></view>
        <view class="sk-line" style="width: 35%"></view>
      </view>
    </view>

    <block v-else-if="item">
      <!-- 图片轮播 -->
      <view class="gallery">
        <swiper class="swiper" circular :current="swiperIndex" @change="onSwiperChange">
          <swiper-item v-for="(img, i) in images" :key="i" @tap="preview(i)">
            <image class="swiper-img" :src="mediaUrl(img)" mode="aspectFill" lazy-load />
          </swiper-item>
        </swiper>
        <view class="gallery-bar">
          <view class="dots">
            <text
              v-for="(img, i) in images"
              :key="i"
              class="dot"
              :class="{ active: swiperIndex === i }"
            ></text>
          </view>
          <text class="counter">{{ swiperIndex + 1 }}/{{ images.length }}</text>
        </view>
        <view class="more-btn" @tap="openMore">⋯</view>
      </view>

      <!-- 价格与标题 -->
      <view class="card">
        <view class="price-row">
          <text class="price">{{ formatPrice(item.price) }}</text>
          <text v-if="item.originalPrice" class="origin">原价 {{ formatPrice(item.originalPrice) }}</text>
          <text class="status" :style="{ color: statusInfo.color }">{{ statusInfo.text }}</text>
        </view>
        <text class="title">{{ item.title }}</text>
        <text v-if="item.summary" class="summary">{{ item.summary }}</text>
        <view class="meta-row">
          <text class="tag">{{ conditionText(item.itemCondition) }}</text>
          <text v-if="item.categoryName" class="tag">{{ item.categoryName }}</text>
          <text v-if="item.tradeType !== undefined" class="tag">{{ tradeTypeText(item.tradeType) }}</text>
          <text v-if="item.aiGenerated" class="tag ai-tag">AI 文案</text>
        </view>
        <view class="stats">
          <text>👁 {{ formatCount(item.viewCount) }} 浏览</text>
          <text>❤️ {{ formatCount(item.favoriteCount) }} 收藏</text>
          <text>{{ timeAgo(item.publishTime) }}</text>
        </view>
      </view>

      <!-- AI 智能问答入口 -->
      <view class="card ai-entry" @tap="openAiChat">
        <view class="ai-left">
          <text class="ai-icon">🤖</text>
          <view>
            <text class="ai-title">AI 智能问答</text>
            <text class="ai-sub">对这件商品有疑问？让 AI 帮你分析</text>
          </view>
        </view>
        <text class="ai-btn">去问问 ›</text>
      </view>

      <!-- 卖家信息 -->
      <view class="card seller-card">
        <image class="avatar" :src="mediaUrl(item.sellerAvatar) || '/static/mock/avatar-default.png'" mode="aspectFill" />
        <view class="seller-info">
          <text class="nickname">{{ item.sellerNickname || '校园卖家' }}</text>
          <text class="credit">信用分 {{ item.sellerCreditScore !== null && item.sellerCreditScore !== undefined ? item.sellerCreditScore : '—' }}</text>
        </view>
        <view class="seller-badge">🎓 在校认证</view>
      </view>

      <!-- 描述 -->
      <view class="card">
        <text class="card-title">商品描述</text>
        <text class="desc">{{ item.description || item.summary || '卖家很懒，什么都没写～' }}</text>
        <view v-if="item.tags && item.tags.length" class="tag-list">
          <text v-for="(t, i) in item.tags" :key="i" class="tag"># {{ t }}</text>
        </view>
      </view>

      <!-- 评论 -->
      <view class="card">
        <view class="card-head">
          <text class="card-title">交易评价（{{ commentTotal }}）</text>
        </view>
        <view v-if="comments.length" class="comment-list">
          <view v-for="c in comments" :key="c.commentId" class="comment-item">
            <image class="c-avatar" :src="mediaUrl(c.fromAvatar) || '/static/mock/avatar-default.png'" mode="aspectFill" />
            <view class="c-body">
              <view class="c-head">
                <text class="c-name">{{ c.fromNickname || '匿名同学' }}</text>
                <text class="c-score">{{ stars(c.score) }}</text>
              </view>
              <text class="c-content">{{ c.content }}</text>
              <text class="c-time">{{ timeAgo(c.createTime) }}</text>
            </view>
          </view>
          <text v-if="commentTotal > comments.length" class="load-more" @tap="loadComments(false)">加载更多评价</text>
        </view>
        <view v-else class="no-comment">
          <text>暂无评价，交易完成后买家可以评价哦～</text>
        </view>
      </view>

      <view class="bottom-space"></view>

      <!-- 底部操作栏 -->
      <view class="bottom-bar">
        <view class="bar-icon" @tap="toggleFav">
          <text class="bar-emoji">{{ item.favorited ? '❤️' : '🤍' }}</text>
          <text class="bar-label">{{ item.favorited ? '已收藏' : '收藏' }}</text>
        </view>
        <view class="bar-icon" @tap="contactSeller">
          <text class="bar-emoji">💬</text>
          <text class="bar-label">联系卖家</text>
        </view>
        <view v-if="isOwner" class="owner-btns">
          <view class="cm-btn cm-btn-ghost" @tap="editItem">编辑</view>
          <view class="cm-btn cm-btn-primary" @tap="offShelf">下架</view>
        </view>
        <view v-else class="buy-btn" :class="{ disabled: !canBuy }" @tap="buyNow">
          {{ canBuy ? '立即购买' : statusInfo.text }}
        </view>
      </view>
    </block>

    <empty-state
      v-else
      icon="😕"
      text="商品不存在或已下架"
      action-text="回首页看看"
      @action="goHome"
    />

    <!-- AI 问答弹层 -->
    <view v-if="aiVisible" class="ai-mask" @tap="closeAiChat">
      <view class="ai-panel" @tap.stop>
        <view class="ai-panel-head">
          <text class="ai-panel-title">🤖 关于「{{ item && item.title }}」</text>
          <text class="close" @tap="closeAiChat">✕</text>
        </view>
        <ai-chat
          ref="aiChatRef"
          title="商品智能问答"
          :show-header="false"
          height="900rpx"
          :welcome="aiWelcome"
          :suggestions="aiSuggestions"
          placeholder="就这件商品提问…"
        />
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import skeletonBox from '@/components/skeleton-box.vue'
import emptyState from '@/components/empty-state.vue'
import aiChat from '@/components/ai-chat.vue'
import { getItemDetail, offShelfItem } from '@/api/item'
import { addFavorite, removeFavorite } from '@/api/favorite'
import { getCommentList } from '@/api/comment'
import { reportItem } from '@/api/report'
import {
  formatPrice,
  formatCount,
  conditionText,
  itemStatusInfo,
  tradeTypeText,
  timeAgo,
  imagesOf,
  mediaUrl
} from '@/utils/format'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const itemId = ref('')
const item = ref(null)
const loading = ref(true)
const swiperIndex = ref(0)
const comments = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentSize = 5
const aiVisible = ref(false)
const aiChatRef = ref(null)

const images = computed(() => {
  const list = imagesOf(item.value)
  return list.length ? list : ['/static/mock/item-1.png']
})
const statusInfo = computed(() => itemStatusInfo(item.value && item.value.status))
const isOwner = computed(
  () => item.value && String(item.value.sellerId) === String(userStore.userId)
)
const canBuy = computed(() => item.value && Number(item.value.status) === 2 && !isOwner.value)
const aiWelcome = computed(
  () => `我是 AI 助手，可以帮你分析「${(item.value && item.value.title) || '这件商品'}」的成色、交易方式与注意事项。`
)
const aiSuggestions = computed(() => {
  const title = (item.value && item.value.title) || '这件商品'
  return [
    `${title} 这个价格合理吗？`,
    `${title} 的成色怎么样？`,
    '校园面交要注意什么？'
  ]
})

async function loadDetail() {
  loading.value = true
  try {
    const res = await getItemDetail(itemId.value)
    // 后端把卖家信息放在 seller 对象里，模板用 sellerXxx 扁平字段，这里拍平一次
    const seller = (res && res.seller) || {}
    item.value = {
      ...res,
      sellerNickname: seller.nickname,
      sellerAvatar: seller.avatar,
      sellerCreditScore: seller.creditScore
    }
  } catch (e) {
    item.value = null
  } finally {
    loading.value = false
  }
}

async function loadComments(reset = true) {
  if (reset) commentPage.value = 1
  try {
    const res = await getCommentList(itemId.value, { current: commentPage.value, size: commentSize })
    const records = (res && res.records) || []
    comments.value = reset ? records : comments.value.concat(records)
    commentTotal.value = (res && res.total) || comments.value.length
    commentPage.value += 1
  } catch (e) {
    if (reset) comments.value = []
  }
}

function onSwiperChange(e) {
  swiperIndex.value = e.detail.current
}

function preview(index) {
  uni.previewImage({ current: index, urls: images.value.map((u) => mediaUrl(u)) })
}

async function toggleFav() {
  if (!userStore.requireLogin()) return
  try {
    const next = !item.value.favorited
    if (next) await addFavorite(itemId.value)
    else await removeFavorite(itemId.value)
    item.value.favorited = next
    item.value.favoriteCount = Math.max(0, Number(item.value.favoriteCount || 0) + (next ? 1 : -1))
    uni.showToast({ title: next ? '已收藏' : '已取消收藏', icon: 'none' })
  } catch (e) {
    /* 已统一 toast */
  }
}

function buyNow() {
  if (!canBuy.value) {
    uni.showToast({ title: `商品当前${statusInfo.value.text}，无法下单`, icon: 'none' })
    return
  }
  if (!userStore.requireLogin()) return
  uni.navigateTo({ url: `/pages/order/confirm?itemId=${itemId.value}` })
}

function contactSeller() {
  uni.showModal({
    title: '联系卖家',
    content: `卖家：${(item.value && item.value.sellerNickname) || '校园卖家'}\n建议在校内公共区域当面交易，确认商品无误后再付款。`,
    confirmText: '去下单',
    cancelText: '知道了',
    success: (res) => {
      if (res.confirm) buyNow()
    }
  })
}

function openAiChat() {
  aiVisible.value = true
}

function closeAiChat() {
  aiVisible.value = false
}

function openMore() {
  const menus = ['举报该商品', '复制商品标题']
  if (isOwner.value) menus.push('编辑商品', '下架商品')
  uni.showActionSheet({
    itemList: menus,
    success: (res) => {
      const label = menus[res.tapIndex]
      if (label === '举报该商品') report()
      else if (label === '复制商品标题') copyTitle()
      else if (label === '编辑商品') editItem()
      else if (label === '下架商品') offShelf()
    }
  })
}

function copyTitle() {
  uni.setClipboardData({
    data: (item.value && item.value.title) || '',
    success: () => uni.showToast({ title: '已复制', icon: 'none' })
  })
}

function report() {
  if (!userStore.requireLogin()) return
  const reasons = ['虚假信息', '违禁物品', '价格欺诈', '骚扰谩骂', '其他']
  const values = ['fake', 'illegal', 'price', 'harass', 'other']
  uni.showActionSheet({
    itemList: reasons,
    success: async (res) => {
      try {
        await reportItem(itemId.value, values[res.tapIndex], reasons[res.tapIndex])
        uni.showToast({ title: '举报已提交', icon: 'none' })
      } catch (e) {
        /* 已统一 toast */
      }
    }
  })
}

function editItem() {
  // 发布页是 tabBar 页面，用本地缓存传递待编辑商品 id
  uni.setStorageSync('pendingEditItemId', itemId.value)
  uni.switchTab({ url: '/pages/item/publish' })
}

async function offShelf() {
  uni.showModal({
    title: '确认下架',
    content: '下架后买家将无法看到这件商品',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await offShelfItem(itemId.value)
        uni.showToast({ title: '已下架', icon: 'none' })
        loadDetail()
      } catch (e) {
        /* 已统一 toast */
      }
    }
  })
}

function stars(score) {
  const n = Math.max(0, Math.min(5, Number(score) || 0))
  return '★'.repeat(n) + '☆'.repeat(5 - n)
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

onLoad((options) => {
  userStore.initFromStorage()
  // 兼容 itemId / id 两种入参，并过滤掉 "undefined"/"null" 这类脏值，
  // 否则会直接打到 /item/app/item/undefined，后端返回"参数类型错误：itemId"
  const raw = options && (options.itemId || options.id)
  itemId.value = raw && /^\d+$/.test(String(raw)) ? String(raw) : ''
  if (!itemId.value) {
    loading.value = false
    return
  }
  loadDetail()
  loadComments(true)
})

onPullDownRefresh(async () => {
  await loadDetail()
  await loadComments(true)
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (commentTotal.value > comments.value.length) loadComments(false)
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $cm-page-bg;
}
.loading-wrap {
  padding: 24rpx;
  background: #fff;
}
.sk-lines {
  margin-top: 24rpx;
  .sk-line {
    height: 30rpx;
    background: #eef0f3;
    border-radius: 8rpx;
    margin-bottom: 16rpx;
  }
}
.gallery {
  position: relative;
  background: #fff;
}
.swiper {
  height: 750rpx;
}
.swiper-img {
  width: 100%;
  height: 750rpx;
  background: #eef0f3;
}
.gallery-bar {
  position: absolute;
  bottom: 24rpx;
  left: 24rpx;
  right: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  .dots {
    display: flex;
  }
  .dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.5);
    margin-right: 10rpx;
    &.active {
      background: #fff;
      width: 30rpx;
      border-radius: 6rpx;
    }
  }
  .counter {
    padding: 4rpx 16rpx;
    background: rgba(0, 0, 0, 0.4);
    color: #fff;
    font-size: 20rpx;
    border-radius: 999rpx;
  }
}
.more-btn {
  position: absolute;
  top: 24rpx;
  right: 24rpx;
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.3);
  color: #fff;
  font-size: 34rpx;
  line-height: 54rpx;
  text-align: center;
}
.card {
  margin: 20rpx 24rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: $cm-radius;
}
.price-row {
  display: flex;
  align-items: baseline;
  .price {
    color: $cm-danger;
    font-size: 52rpx;
    font-weight: 600;
  }
  .origin {
    margin-left: 16rpx;
    font-size: 22rpx;
    color: $cm-text-4;
    text-decoration: line-through;
  }
  .status {
    margin-left: auto;
    font-size: 24rpx;
  }
}
.title {
  display: block;
  margin-top: 14rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: $cm-text;
  line-height: 46rpx;
}
.summary {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $cm-text-2;
}
.meta-row {
  display: flex;
  flex-wrap: wrap;
  margin-top: 16rpx;
}
.tag {
  padding: 4rpx 16rpx;
  margin: 0 12rpx 10rpx 0;
  background: $cm-primary-light;
  color: $cm-primary;
  border-radius: 8rpx;
  font-size: 20rpx;
  &.ai-tag {
    background: #f3eeff;
    color: #7d5bff;
  }
}
.stats {
  display: flex;
  justify-content: space-between;
  margin-top: 10rpx;
  font-size: 21rpx;
  color: $cm-text-3;
}
.ai-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #eef4ff, #f7f0ff);
  .ai-left {
    display: flex;
    align-items: center;
  }
  .ai-icon {
    font-size: 48rpx;
    margin-right: 16rpx;
  }
  .ai-title {
    display: block;
    font-size: 28rpx;
    font-weight: 600;
    color: $cm-text;
  }
  .ai-sub {
    display: block;
    margin-top: 4rpx;
    font-size: 21rpx;
    color: $cm-text-2;
  }
  .ai-btn {
    font-size: 24rpx;
    color: $cm-primary;
  }
}
.seller-card {
  display: flex;
  align-items: center;
  .avatar {
    width: 88rpx;
    height: 88rpx;
    border-radius: 50%;
    background: #eef0f3;
  }
  .seller-info {
    flex: 1;
    margin-left: 18rpx;
  }
  .nickname {
    display: block;
    font-size: 28rpx;
    color: $cm-text;
    font-weight: 500;
  }
  .credit {
    display: block;
    margin-top: 4rpx;
    font-size: 21rpx;
    color: $cm-text-3;
  }
  .seller-badge {
    padding: 6rpx 16rpx;
    background: $cm-primary-light;
    color: $cm-primary;
    border-radius: 999rpx;
    font-size: 20rpx;
  }
}
.card-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $cm-text;
}
.desc {
  display: block;
  margin-top: 14rpx;
  font-size: 26rpx;
  color: $cm-text-2;
  line-height: 44rpx;
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  margin-top: 16rpx;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.comment-list {
  margin-top: 12rpx;
}
.comment-item {
  display: flex;
  padding: 18rpx 0;
  border-top: 1rpx solid $cm-border;
  .c-avatar {
    width: 60rpx;
    height: 60rpx;
    border-radius: 50%;
    background: #eef0f3;
  }
  .c-body {
    flex: 1;
    margin-left: 16rpx;
  }
  .c-head {
    display: flex;
    justify-content: space-between;
  }
  .c-name {
    font-size: 24rpx;
    color: $cm-text;
  }
  .c-score {
    color: #ff9f0a;
    font-size: 22rpx;
  }
  .c-content {
    display: block;
    margin-top: 8rpx;
    font-size: 25rpx;
    color: $cm-text-2;
    line-height: 40rpx;
  }
  .c-time {
    display: block;
    margin-top: 6rpx;
    font-size: 20rpx;
    color: $cm-text-4;
  }
}
.load-more {
  display: block;
  text-align: center;
  padding: 16rpx 0;
  font-size: 22rpx;
  color: $cm-primary;
}
.no-comment {
  padding: 30rpx 0;
  font-size: 23rpx;
  color: $cm-text-3;
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
  padding: 14rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid $cm-border;
  .bar-icon {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-right: 34rpx;
  }
  .bar-emoji {
    font-size: 34rpx;
  }
  .bar-label {
    font-size: 20rpx;
    color: $cm-text-3;
  }
  .owner-btns {
    flex: 1;
    display: flex;
    justify-content: flex-end;
    .cm-btn {
      margin-left: 16rpx;
      padding: 0 40rpx;
    }
  }
  .buy-btn {
    flex: 1;
    height: 80rpx;
    line-height: 80rpx;
    text-align: center;
    background: $cm-primary;
    color: #fff;
    border-radius: 999rpx;
    font-size: 30rpx;
    font-weight: 600;
    &.disabled {
      background: $cm-text-4;
    }
  }
}
.ai-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 99;
  display: flex;
  align-items: flex-end;
}
.ai-panel {
  width: 100%;
  background: #f5f6f8;
  border-top-left-radius: 28rpx;
  border-top-right-radius: 28rpx;
  padding: 20rpx;
  box-sizing: border-box;
  .ai-panel-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 16rpx;
  }
  .ai-panel-title {
    font-size: 26rpx;
    color: $cm-text;
    font-weight: 600;
    flex: 1;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  .close {
    font-size: 28rpx;
    color: $cm-text-3;
    padding: 0 10rpx;
  }
}
</style>
