<!-- 商品卡片：mode=grid 双列瀑布流 / mode=row 横向列表 -->
<template>
  <view class="item-card" :class="'mode-' + mode" @tap="onTapCard">
    <view class="cover-wrap">
      <image class="cover" :src="cover" mode="aspectFill" lazy-load />
      <view v-if="showStatus" class="status-tag" :style="{ background: statusInfo.color }">
        {{ statusInfo.text }}
      </view>
      <view class="fav" @tap.stop="$emit('favorite', item)">
        <text class="fav-icon">{{ item.favorited ? '❤️' : '🤍' }}</text>
      </view>
    </view>

    <view class="info">
      <text class="title">{{ item.title }}</text>
      <view class="price-row">
        <text class="price">{{ formatPrice(item.price) }}</text>
        <text v-if="item.originalPrice" class="origin">{{ formatPrice(item.originalPrice) }}</text>
      </view>
      <view class="meta-row">
        <text class="cond-tag">{{ conditionText(item.itemCondition) }}</text>
        <text v-if="mode === 'row' && item.sellerNickname" class="seller">{{ item.sellerNickname }}</text>
        <text class="count">👁 {{ formatCount(item.viewCount) }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { formatPrice, formatCount, conditionText, itemStatusInfo, coverOf } from '@/utils/format'

const props = defineProps({
  item: { type: Object, required: true },
  mode: { type: String, default: 'grid' },
  showStatus: { type: Boolean, default: false }
})

const emit = defineEmits(['select', 'favorite'])

/**
 * 点击整卡：必须用自定义事件名（select）而不是 tap。
 * 原因：uni-app 编译到 H5 / 小程序时，父组件上的 @tap 会被当成"原生点击事件"，
 * 直接挂在组件根节点上，回调收到的是 DOM 事件而不是 item，导致详情页拿到
 * itemId=undefined（表现为"参数类型错误：itemId"）。
 */
function onTapCard() {
  emit('select', props.item)
}

const cover = computed(() => coverOf(props.item))
const statusInfo = computed(() => itemStatusInfo(props.item.status))
</script>

<style lang="scss" scoped>
.item-card {
  background: $cm-card-bg;
  border-radius: $cm-radius;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(31, 34, 41, 0.04);

  .cover-wrap {
    position: relative;
    width: 100%;
  }
  .cover {
    width: 100%;
    height: 330rpx;
    background: #eef0f3;
    display: block;
  }
  .status-tag {
    position: absolute;
    left: 0;
    top: 0;
    padding: 4rpx 14rpx;
    color: #fff;
    font-size: 20rpx;
    border-bottom-right-radius: 12rpx;
  }
  .fav {
    position: absolute;
    right: 12rpx;
    bottom: 12rpx;
    width: 52rpx;
    height: 52rpx;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.9);
    display: flex;
    align-items: center;
    justify-content: center;
    .fav-icon {
      font-size: 26rpx;
    }
  }
  .info {
    padding: 16rpx;
  }
  .title {
    font-size: 26rpx;
    color: $cm-text;
    line-height: 36rpx;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .price-row {
    margin-top: 12rpx;
    display: flex;
    align-items: baseline;
  }
  .price {
    color: $cm-danger;
    font-size: 32rpx;
    font-weight: 600;
  }
  .origin {
    margin-left: 10rpx;
    font-size: 20rpx;
    color: $cm-text-4;
    text-decoration: line-through;
  }
  .meta-row {
    margin-top: 10rpx;
    display: flex;
    align-items: center;
    font-size: 20rpx;
    color: $cm-text-3;
  }
  .cond-tag {
    padding: 2rpx 10rpx;
    border-radius: 8rpx;
    background: $cm-primary-light;
    color: $cm-primary;
    margin-right: 12rpx;
  }
  .seller {
    flex: 1;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  .count {
    margin-left: auto;
  }

  // 横向列表形态（我的收藏、订单里的商品行）
  &.mode-row {
    display: flex;
    padding: 20rpx;

    .cover-wrap {
      width: 200rpx;
      flex: none;
    }
    .cover {
      height: 200rpx;
      border-radius: $cm-radius-sm;
    }
    .info {
      flex: 1;
      padding: 0 0 0 20rpx;
      display: flex;
      flex-direction: column;
    }
    .title {
      font-size: 28rpx;
    }
    .price-row {
      margin-top: auto;
    }
  }
}
</style>
