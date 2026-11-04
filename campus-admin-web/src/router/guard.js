import router from './index'
import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/auth'

const APP_TITLE = '校园易站后台'

// 白名单：不需要登录就能访问
const whiteList = ['/login']

/**
 * 全局前置守卫
 *
 * 核心逻辑：
 * 1. 没有 token → 去登录页（带 redirect）
 * 2. 有 token 但「业务路由还没注册」（首次进入 / 刷新页面）→ 拉取用户信息 + 菜单 → 注册动态路由 → 重新进入目标路由
 *    这是动态菜单方案的关键：刷新后 Pinia 与 router 都会重置，必须重新拉菜单，否则页面会 404
 * 3. 已注册 → 直接放行
 */
router.beforeEach(async (to) => {
  const token = getToken()

  if (!token) {
    if (whiteList.includes(to.path)) return true
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 已登录还去登录页 → 回首页
  if (to.path === '/login') {
    return { path: '/' }
  }

  const userStore = useUserStore()
  if (userStore.routesLoaded) return true

  try {
    if (!userStore.userInfo) await userStore.fetchUserInfo()
    await userStore.fetchMenus()
    // 动态路由刚注册，本次导航的匹配结果已过期，用 replace 重新解析一次当前地址
    return { path: to.path, query: to.query, hash: to.hash, replace: true }
  } catch (error) {
    console.error('[guard] 初始化登录用户/菜单失败：', error)
    userStore.resetState()
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

router.afterEach((to) => {
  const title = to.meta?.title
  document.title = title ? `${title} - ${APP_TITLE}` : APP_TITLE
})

export default router
