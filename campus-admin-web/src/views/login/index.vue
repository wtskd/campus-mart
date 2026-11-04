<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-inner">
        <el-icon class="brand-logo"><ShoppingBag /></el-icon>
        <h1 class="brand-title">校园易站</h1>
        <p class="brand-sub">CampusMart · 校园二手交易平台</p>
        <ul class="brand-feature">
          <li><el-icon><CircleCheck /></el-icon>闲置发布 · 智能文案生成</li>
          <li><el-icon><CircleCheck /></el-icon>交易撮合 · 全流程可追溯</li>
          <li><el-icon><CircleCheck /></el-icon>违规审核 · AI 辅助风控</li>
          <li><el-icon><CircleCheck /></el-icon>数据看板 · 经营一屏掌握</li>
        </ul>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="login-form-panel">
      <div class="login-card">
        <h2 class="login-title">后台管理系统</h2>
        <p class="login-desc">请使用管理员账号登录</p>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="captchaCode">
            <div class="captcha-row">
              <el-input
                v-model="form.captchaCode"
                placeholder="请输入验证码"
                :prefix-icon="Key"
                maxlength="6"
              />
              <div class="captcha-img" title="点击刷新验证码" @click="refreshCaptcha">
                <img v-if="captchaImage" :src="captchaSrc" alt="验证码" />
                <span v-else class="captcha-loading">
                  <el-icon class="is-loading"><Loading /></el-icon>
                </span>
              </div>
            </div>
          </el-form-item>

          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form>

        <div class="login-tip">
          <p>账号由系统管理员分配，如忘记密码请联系管理员重置</p>
        </div>
      </div>

      <p class="copyright">© 2026 CampusMart 校园易站 · 企业实习结课项目</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const captchaImage = ref('')
const captchaKey = ref('')

const form = reactive({
  username: 'admin',
  password: '',
  captchaCode: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// 后端可能返回纯 base64，也可能返回完整 data URI，这里统一兼容
const captchaSrc = computed(() => {
  const image = captchaImage.value
  if (!image) return ''
  return image.startsWith('data:') ? image : `data:image/png;base64,${image}`
})

/** 获取图形验证码：GET /auth/captcha → { key, image } */
async function refreshCaptcha() {
  try {
    const data = await getCaptcha()
    captchaImage.value = data?.image || ''
    captchaKey.value = data?.key || ''
  } catch (error) {
    captchaImage.value = ''
    ElMessage.error('验证码加载失败，请检查后端服务是否启动')
  }
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password,
      captchaKey: captchaKey.value,
      captchaCode: form.captchaCode
    })
    ElMessage.success('登录成功')
    // 登录前想访问的页面（redirect 参数），登录后回到那里
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (error) {
    // 验证码一次性使用，失败后必须刷新
    form.captchaCode = ''
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #f5f7fb;
}

/* 左侧深色品牌区 */
.login-brand {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 55%;
  color: #fff;
  background: linear-gradient(135deg, #0b1e3d 0%, #113b74 45%, #1d6fd1 100%);
  overflow: hidden;
}

.login-brand::after {
  content: '';
  position: absolute;
  width: 640px;
  height: 640px;
  right: -220px;
  bottom: -260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.45), transparent 65%);
}

.brand-inner {
  position: relative;
  z-index: 1;
  padding: 40px;
}

.brand-logo {
  font-size: 46px;
  color: #7cc0ff;
}

.brand-title {
  margin: 18px 0 6px;
  font-size: 44px;
  letter-spacing: 6px;
}

.brand-sub {
  margin: 0 0 40px;
  font-size: 15px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.65);
}

.brand-feature {
  margin: 0;
  padding: 0;
  list-style: none;
}

.brand-feature li {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.85);
}

.brand-feature li .el-icon {
  color: #67c23a;
}

/* 右侧表单 */
.login-form-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-card {
  width: 380px;
  padding: 36px 32px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(17, 59, 116, 0.12);
}

.login-title {
  margin: 0;
  font-size: 24px;
  color: #1f2d3d;
}

.login-desc {
  margin: 8px 0 26px;
  font-size: 13px;
  color: #909399;
}

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-img {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 40px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #f5f7fa;
  cursor: pointer;
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-btn {
  width: 100%;
  margin-top: 6px;
  letter-spacing: 4px;
}

.login-tip {
  margin-top: 18px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.login-tip p {
  margin: 0;
}

.copyright {
  margin-top: 26px;
  font-size: 12px;
  color: #b1b7c3;
}

@media (max-width: 960px) {
  .login-brand {
    display: none;
  }
}
</style>
