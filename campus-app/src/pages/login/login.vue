<!-- 登录页：App 端账号密码登录（契约 v1.1 /auth/app/login，免图形验证码） -->
<template>
  <view class="page">
    <view class="brand">
      <view class="logo">🛒</view>
      <text class="title">校园易站</text>
      <text class="subtitle">CampusMart · 校园二手交易平台</text>
    </view>

    <view class="form-card">
      <view class="field">
        <text class="field-icon">👤</text>
        <input
          v-model="username"
          class="input"
          placeholder="学号 / 用户名"
          maxlength="30"
          :adjust-position="true"
        />
      </view>
      <view class="field">
        <text class="field-icon">🔒</text>
        <input
          v-model="password"
          class="input"
          :password="!showPwd"
          placeholder="密码"
          maxlength="30"
          :adjust-position="true"
          @confirm="submit"
        />
        <text class="eye" @tap="showPwd = !showPwd">{{ showPwd ? '🙈' : '👁' }}</text>
      </view>

      <view class="agreement" @tap="agreed = !agreed">
        <text class="checkbox" :class="{ checked: agreed }">{{ agreed ? '✓' : '' }}</text>
        <text class="agreement-text">我已阅读并同意《用户协议》与《隐私政策》</text>
      </view>

      <view class="login-btn" :class="{ disabled: submitting }" @tap="submit">
        {{ submitting ? '登录中…' : '登 录' }}
      </view>

      <view class="demo-tip">
        <text class="demo-title">演示账号（点击自动填充）</text>
        <view class="demo-list">
          <view v-for="acc in demoAccounts" :key="acc.username" class="demo-item" @tap="fillDemo(acc)">
            {{ acc.username }} / {{ acc.password }}
          </view>
        </view>
      </view>
    </view>

    <view class="footer">
      <text class="footer-text">企业实习结课项目 · Spring Cloud 微服务架构</text>
      <text class="back" @tap="goBack">先随便逛逛 ›</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import config from '@/config'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const username = ref('')
const password = ref('')
const showPwd = ref(false)
const agreed = ref(true)
const submitting = ref(false)
const redirect = ref('')
const demoAccounts = ref(config.demoAccounts || [])

function fillDemo(acc) {
  username.value = acc.username
  password.value = acc.password
}

async function submit() {
  if (submitting.value) return
  if (!username.value.trim()) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return
  }
  if (!password.value) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await userStore.loginByApp(username.value.trim(), password.value)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      back()
    }, 600)
  } catch (e) {
    /* request 已统一 toast */
  } finally {
    submitting.value = false
  }
}

function back() {
  const pages = getCurrentPages()
  if (redirect.value) {
    uni.reLaunch({ url: redirect.value })
  } else if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.switchTab({ url: '/pages/index/index' })
  }
}

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/index/index' })
}

onLoad((options) => {
  userStore.initFromStorage()
  if (options && options.redirect) {
    redirect.value = decodeURIComponent(options.redirect)
  }
  if (!demoAccounts.value.length) {
    demoAccounts.value = [
      { username: 'student01', password: 'admin123' },
      { username: 'student02', password: 'admin123' },
      { username: 'student03', password: 'admin123' }
    ]
  }
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #2b7fff 0%, #6aa8ff 42%, #f5f6f8 42%, #f5f6f8 100%);
  padding: 0 40rpx;
  box-sizing: border-box;
}
.brand {
  padding: 120rpx 0 70rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  .logo {
    width: 150rpx;
    height: 150rpx;
    border-radius: 40rpx;
    background: rgba(255, 255, 255, 0.2);
    border: 2rpx solid rgba(255, 255, 255, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 76rpx;
  }
  .title {
    margin-top: 24rpx;
    color: #fff;
    font-size: 44rpx;
    font-weight: 700;
    letter-spacing: 4rpx;
  }
  .subtitle {
    margin-top: 10rpx;
    color: rgba(255, 255, 255, 0.9);
    font-size: 22rpx;
  }
}
.form-card {
  background: #fff;
  border-radius: $cm-radius-lg;
  padding: 40rpx 32rpx;
  box-shadow: 0 12rpx 40rpx rgba(43, 127, 255, 0.12);
}
.field {
  display: flex;
  align-items: center;
  height: 96rpx;
  background: $cm-page-bg;
  border-radius: $cm-radius;
  padding: 0 24rpx;
  margin-bottom: 24rpx;
  .field-icon {
    font-size: 30rpx;
    margin-right: 16rpx;
  }
  .input {
    flex: 1;
    font-size: 28rpx;
    color: $cm-text;
  }
  .eye {
    font-size: 28rpx;
    padding: 0 6rpx;
  }
}
.agreement {
  display: flex;
  align-items: center;
  margin: 6rpx 0 30rpx;
  .checkbox {
    width: 32rpx;
    height: 32rpx;
    border: 2rpx solid $cm-text-4;
    border-radius: 8rpx;
    margin-right: 12rpx;
    text-align: center;
    line-height: 30rpx;
    font-size: 22rpx;
    color: #fff;
    flex: none;
    &.checked {
      background: $cm-primary;
      border-color: $cm-primary;
    }
  }
  .agreement-text {
    font-size: 21rpx;
    color: $cm-text-3;
  }
}
.login-btn {
  height: 92rpx;
  line-height: 92rpx;
  text-align: center;
  background: $cm-primary;
  color: #fff;
  border-radius: 999rpx;
  font-size: 32rpx;
  font-weight: 600;
  letter-spacing: 6rpx;
  &.disabled {
    opacity: 0.7;
  }
}
.demo-tip {
  margin-top: 36rpx;
  padding-top: 28rpx;
  border-top: 1rpx dashed $cm-border;
  .demo-title {
    font-size: 22rpx;
    color: $cm-text-3;
  }
  .demo-list {
    display: flex;
    flex-wrap: wrap;
    margin-top: 14rpx;
  }
  .demo-item {
    padding: 10rpx 20rpx;
    margin: 0 14rpx 14rpx 0;
    background: $cm-primary-light;
    color: $cm-primary;
    border-radius: 999rpx;
    font-size: 21rpx;
  }
}
.footer {
  margin-top: 50rpx;
  padding-bottom: 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  .footer-text {
    font-size: 20rpx;
    color: $cm-text-4;
  }
  .back {
    margin-top: 20rpx;
    font-size: 24rpx;
    color: $cm-primary;
  }
}
</style>
