<!-- 空状态占位 -->
<template>
  <view class="empty-state">
    <text class="emoji">{{ icon }}</text>
    <text class="text">{{ text }}</text>
    <text v-if="sub" class="sub">{{ sub }}</text>
    <view v-if="actionText" class="cm-btn cm-btn-plain action" @tap="$emit('action')">{{ actionText }}</view>
  </view>
</template>

<script setup>
/** 空状态：列表无数据 / 加载失败时使用 */
defineProps({
  icon: { type: String, default: '🗂️' },
  text: { type: String, default: '暂无数据' },
  sub: { type: String, default: '' },
  actionText: { type: String, default: '' }
})
defineEmits(['action'])
</script>

<style lang="scss" scoped>
.empty-state {
  padding: 120rpx 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;

  .emoji {
    font-size: 96rpx;
    line-height: 1.2;
    opacity: 0.85;
  }
  .text {
    margin-top: 24rpx;
    font-size: 28rpx;
    color: $cm-text-2;
  }
  .sub {
    margin-top: 10rpx;
    font-size: 24rpx;
    color: $cm-text-3;
    text-align: center;
    line-height: 36rpx;
  }
  .action {
    margin-top: 32rpx;
    min-width: 240rpx;
  }
}
</style>
