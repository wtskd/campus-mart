<!-- 个人中心 -->
<template>
  <view class="page">
    <!-- 头部 -->
    <view class="header">
      <view class="user-info" @tap="onUserTap">
        <image class="avatar" :src="mediaUrl(userStore.avatar)" mode="aspectFill" />
        <view class="info">
          <text class="nickname">{{ userStore.isLogin ? userStore.nickname : '未登录' }}</text>
          <text class="sub">{{ userStore.isLogin ? userStore.deptName || '校园用户' : '点击登录，开启二手交易' }}</text>
        </view>
      </view>
      <view class="setting" @tap="goSetting">
        <text class="gear">⚙️</text>
      </view>
    </view>

    <!-- 订单快捷入口 -->
    <view class="card order-card">
      <view class="card-head" @tap="goOrders('buy')">
        <text class="card-title">我的订单</text>
        <text class="more">全部订单 ›</text>
      </view>
      <view class="order-entries">
        <view class="entry" @tap="goOrders('buy', 0)">
          <text class="entry-icon">⏳</text>
          <text class="entry-text">待确认</text>
        </view>
        <view class="entry" @tap="goOrders('buy', 1)">
          <text class="entry-icon">📦</text>
          <text class="entry-text">待交付</text>
        </view>
        <view class="entry" @tap="goOrders('buy', 2)">
          <text class="entry-icon">✅</text>
          <text class="entry-text">已完成</text>
        </view>
        <view class="entry" @tap="goOrders('sell')">
          <text class="entry-icon">💰</text>
          <text class="entry-text">我卖出的</text>
        </view>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="card menu">
      <view class="menu-item" @tap="goMyItems">
        <text class="menu-icon">📤</text>
        <text class="menu-text">我的发布</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goFavorite">
        <text class="menu-icon">💗</text>
        <text class="menu-text">我的收藏</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goMessage">
        <text class="menu-icon">💬</text>
        <text class="menu-text">消息中心</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goAi">
        <text class="menu-icon">🤖</text>
        <text class="menu-text">AI 智能客服</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="card menu">
      <view class="menu-item" @tap="goSetting">
        <text class="menu-icon">⚙️</text>
        <text class="menu-text">设置</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="about">
        <text class="menu-icon">ℹ️</text>
        <text class="menu-text">关于校园易站</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="logout-wrap">
      <view v-if="userStore.isLogin" class="cm-btn cm-btn-ghost logout" @tap="logout">退出登录</view>
      <view v-else class="cm-btn cm-btn-primary logout" @tap="goLogin">登录 / 注册</view>
    </view>

    <text class="version">校园易站 v1.0.0 · 企业实习结课项目</text>
  </view>
</template>

<script setup>
import { onShow } from '@dcloudio/uni-app'
import { mediaUrl } from '@/utils/format'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

function requireLogin() {
  return userStore.requireLogin()
}

function onUserTap() {
  if (!userStore.isLogin) goLogin()
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}

function goOrders(role, status) {
  if (!requireLogin()) return
  let url = `/pages/order/list?role=${role}`
  if (status !== undefined && status !== null) url += `&status=${status}`
  uni.navigateTo({ url })
}

function goMyItems() {
  if (!requireLogin()) return
  uni.navigateTo({ url: '/pages/item/my' })
}

function goFavorite() {
  if (!requireLogin()) return
  uni.navigateTo({ url: '/pages/favorite/favorite' })
}

function goMessage() {
  uni.switchTab({ url: '/pages/message/message' })
}

function goAi() {
  uni.setStorageSync('cm_message_tab', 'ai')
  uni.switchTab({ url: '/pages/message/message' })
}

function goSetting() {
  if (!requireLogin()) return
  const items = ['账号与安全', '清除缓存', '关于我们']
  uni.showActionSheet({
    itemList: items,
    success: (res) => {
      if (res.tapIndex === 0) {
        const info = userStore.userInfo || {}
        uni.showModal({
          title: '账号信息',
          content: `用户名：${info.username || '-'}\n昵称：${userStore.nickname}\n学院：${userStore.deptName || '-'}`,
          showCancel: false
        })
      } else if (res.tapIndex === 1) {
        try {
          uni.clearStorageSync()
        } catch (e) {
          /* ignore */
        }
        userStore.clearLoginState()
        uni.showToast({ title: '缓存已清除', icon: 'none' })
      } else {
        about()
      }
    }
  })
}

function about() {
  uni.showModal({
    title: '关于校园易站',
    content: '校园易站 CampusMart —— 面向校园的二手交易平台\n技术栈：uniapp + Vue3 + Spring Cloud Gateway\nAI 能力：智能推荐 / 一键文案 / 智能问答',
    showCancel: false
  })
}

function logout() {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    success: async (res) => {
      if (!res.confirm) return
      await userStore.logout()
      uni.showToast({ title: '已退出登录', icon: 'none' })
    }
  })
}

onShow(() => {
  userStore.initFromStorage()
  // 有 token 但没有用户信息（或缺少昵称）时补拉一次 /auth/me
  if (userStore.isLogin && (!userStore.userInfo || !userStore.userInfo.nickname)) {
    userStore.fetchMe().catch(() => {})
  }
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $cm-page-bg;
  padding-bottom: 60rpx;
}
.header {
  display: flex;
  align-items: center;
  padding: 60rpx 32rpx 70rpx;
  background: linear-gradient(135deg, #2b7fff 0%, #6aa8ff 100%);
  position: relative;
  .user-info {
    display: flex;
    align-items: center;
    flex: 1;
  }
  .avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
    border: 4rpx solid rgba(255, 255, 255, 0.6);
    background: #fff;
  }
  .info {
    margin-left: 22rpx;
  }
  .nickname {
    display: block;
    color: #fff;
    font-size: 36rpx;
    font-weight: 600;
  }
  .sub {
    display: block;
    margin-top: 8rpx;
    color: rgba(255, 255, 255, 0.85);
    font-size: 23rpx;
  }
  .setting {
    padding: 10rpx;
  }
  .gear {
    font-size: 38rpx;
  }
}
.card {
  margin: 0 24rpx 20rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: $cm-radius;
}
.order-card {
  margin-top: -44rpx;
  position: relative;
  z-index: 2;
  .card-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .card-title {
    font-size: 28rpx;
    font-weight: 600;
    color: $cm-text;
  }
  .more {
    font-size: 22rpx;
    color: $cm-text-3;
  }
  .order-entries {
    display: flex;
    margin-top: 24rpx;
  }
  .entry {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  .entry-icon {
    font-size: 40rpx;
  }
  .entry-text {
    margin-top: 10rpx;
    font-size: 22rpx;
    color: $cm-text-2;
  }
}
.menu {
  padding: 0 24rpx;
  .menu-item {
    display: flex;
    align-items: center;
    min-height: 100rpx;
    border-bottom: 1rpx solid $cm-border;
    &:last-child {
      border-bottom: none;
    }
  }
  .menu-icon {
    font-size: 34rpx;
    width: 52rpx;
    flex: none;
  }
  .menu-text {
    flex: 1;
    font-size: 27rpx;
    color: $cm-text;
  }
  .menu-arrow {
    color: $cm-text-4;
    font-size: 28rpx;
  }
}
.logout-wrap {
  padding: 30rpx 24rpx 10rpx;
  .logout {
    height: 84rpx;
    line-height: 84rpx;
    border-radius: 999rpx;
    font-size: 28rpx;
  }
}
.version {
  display: block;
  text-align: center;
  font-size: 20rpx;
  color: $cm-text-4;
  margin-top: 20rpx;
}
</style>
