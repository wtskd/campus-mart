import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'

/**
 * 静态路由（不需要权限就能访问）
 * 业务路由在登录后由 store/user.js 的 fetchMenus() 动态注册到 name 为 'Layout' 的路由下。
 */
export const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        // 首页直接复用数据看板页面（指标卡 + 趋势 + 分类占比 + 漏斗 + 热门商品 + AI 使用量）
        component: () => import('@/views/analysis/overview/index.vue'),
        meta: { title: '首页', icon: 'dashboard', affix: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', hidden: true }
      }
    ]
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', hidden: true }
  },
  // 兜底：未匹配到的路径跳 404（必须最后注册，动态路由的优先级高于通配符）
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
    meta: { hidden: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

export default router
