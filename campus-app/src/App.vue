<script>
import { useUserStore } from '@/store/user'

export default {
  onLaunch() {
    // 启动时把本地缓存的登录态回灌到 Pinia（token + 用户信息）
    const userStore = useUserStore()
    userStore.initFromStorage()
    console.log('[CampusMart] App Launch, platform =', uni.getSystemInfoSync().uniPlatform)
  },
  onShow() {
    // 预留：从后台切回前台时可刷新未读消息数
  },
  onHide() {}
}
</script>

<style lang="scss">
/* ==================== 全局样式（App.vue 中的样式对所有页面生效） ==================== */
page {
  background-color: $cm-page-bg;
  color: $cm-text;
  font-size: 28rpx;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

view,
text,
image,
scroll-view,
swiper,
swiper-item,
input,
textarea,
button {
  box-sizing: border-box;
}

/* 页面容器 */
.cm-page {
  min-height: 100vh;
  background-color: $cm-page-bg;
}

/* 卡片 */
.cm-card {
  background: $cm-card-bg;
  border-radius: $cm-radius;
  box-shadow: $cm-shadow;
}

/* 主按钮（覆盖 uni button 默认样式） */
.cm-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  line-height: 1;
}

.cm-btn::after {
  border: none;
}

.cm-btn-primary {
  background: linear-gradient(135deg, #4a90ff 0%, #2b7fff 100%);
  color: #fff;
  box-shadow: 0 8rpx 20rpx rgba(43, 127, 255, 0.28);
}

.cm-btn-primary[disabled] {
  background: #c3d7f7;
  color: #fff;
  box-shadow: none;
}

.cm-btn-plain {
  background: #fff;
  color: $cm-primary;
  border: 2rpx solid $cm-primary;
}

.cm-btn-ghost {
  background: #f2f4f7;
  color: $cm-text-2;
}

/* 标签 */
.cm-tag {
  display: inline-flex;
  align-items: center;
  padding: 0 12rpx;
  height: 34rpx;
  border-radius: 8rpx;
  font-size: 20rpx;
  background: $cm-primary-light;
  color: $cm-primary;
}

/* 价格 */
.cm-price {
  color: $cm-danger;
  font-weight: 700;
}

.cm-price-symbol {
  font-size: 22rpx;
}

/* 文本省略 */
.cm-ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.cm-ellipsis-2 {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-all;
}

/* 通用行 */
.cm-row {
  display: flex;
  align-items: center;
}

.cm-row-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 分割线 */
.cm-divider {
  height: 2rpx;
  background: $cm-border;
}

/* 安全区底部占位（H5 与小程序通用） */
.cm-safe-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

/* ==================== H5：PC 浏览器打开时约束成手机宽度 ==================== */
/* 只在 H5 生效（小程序 / App 不会命中宽度断点） */
@media screen and (min-width: 600px) {
  html,
  body {
    background: #e6ebf2;
  }

  /* 居中列宽交给框架：pages.json 的 globalStyle.maxWidth = 480。
     uni 会在 uni-app 上写 max-width + margin:0 auto，并把 --window-left/--window-right
     设成左右留白，内置导航栏和 tabBar 会自动跟着收进中间这一列。
     这里只补一个投影，让它看起来像一台手机。 */
  uni-app {
    min-height: 100vh;
    box-shadow: 0 0 40px rgba(31, 34, 41, 0.14);
    max-width: 480px;
    margin: 0 auto;
  }

  /* 兜底：若 dev server 还没重新读取 pages.json 的 maxWidth，固定栏仍按列宽定位 */
  .uni-page-head,
  uni-tabbar.uni-tabbar-bottom,
  uni-tabbar.uni-tabbar-bottom .uni-tabbar {
    left: calc(50% - 240px) !important;
    right: auto !important;
    width: 480px !important;
    max-width: 480px;
  }

  /* 页面内 position:fixed 的底部操作栏 / 遮罩：框架不会自动收窄，按列宽居中 */
  uni-page-body .bottom-bar,
  uni-page-body .ai-mask,
  uni-page-body .mask,
  uni-page-body .footer-bar,
  uni-page-body .pay-bar,
  uni-page-body .action-bar {
    left: calc(50% - 240px) !important;
    right: auto !important;
    width: 480px !important;
  }
}
</style>
