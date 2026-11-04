<!-- 骨架屏：首屏加载占位，减少白屏跳动 -->
<template>
  <view class="skeleton" :class="'mode-' + mode">
    <view v-for="i in rows" :key="i" class="sk-item">
      <view class="sk-cover shimmer"></view>
      <view class="sk-lines">
        <view class="sk-line shimmer" style="width: 90%"></view>
        <view class="sk-line shimmer" style="width: 60%"></view>
        <view class="sk-line shimmer" style="width: 36%"></view>
      </view>
    </view>
  </view>
</template>

<script setup>
/** 骨架屏：mode=grid 双列卡片 / mode=row 横向列表 */
defineProps({
  rows: { type: Number, default: 4 },
  mode: { type: String, default: 'grid' }
})
</script>

<style lang="scss" scoped>
.skeleton {
  padding: 0;
}
.sk-cover {
  background: #eef0f3;
  border-radius: $cm-radius-sm;
}
.sk-line {
  height: 22rpx;
  border-radius: 999rpx;
  background: #eef0f3;
  margin-bottom: 14rpx;
}
@keyframes fade {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.45;
  }
  100% {
    opacity: 1;
  }
}
.shimmer {
  animation: fade 1.2s ease-in-out infinite;
}

/* 双列网格 */
.skeleton.mode-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  .sk-item {
    width: 48.6%;
    background: $cm-card-bg;
    border-radius: $cm-radius;
    padding-bottom: 20rpx;
    margin-bottom: 20rpx;
  }
  .sk-cover {
    width: 100%;
    height: 330rpx;
    border-radius: $cm-radius $cm-radius 0 0;
  }
  .sk-lines {
    padding: 18rpx 18rpx 0;
  }
}

/* 横向列表 */
.skeleton.mode-row {
  .sk-item {
    display: flex;
    background: $cm-card-bg;
    border-radius: $cm-radius;
    padding: 18rpx;
    margin-bottom: 20rpx;
  }
  .sk-cover {
    width: 200rpx;
    height: 200rpx;
    flex-shrink: 0;
  }
  .sk-lines {
    flex: 1;
    padding-left: 20rpx;
    padding-top: 10rpx;
  }
}
</style>
