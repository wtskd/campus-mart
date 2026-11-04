import { defineStore } from 'pinia'
import { getMe, getMenus, login as loginApi, logout as logoutApi } from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/auth'
import { buildRoutes } from '@/router/buildRoutes'
import router from '@/router'

/**
 * 前端补充菜单
 *
 * 数据库种子里的菜单只有 29 条（系统/商品/统计/AI 四组），
 * 但交付要求包含「AI 配置（切换 provider + 连通性测试）」页面，而种子中没有对应菜单，
 * 因此在拉取到菜单树后由前端补充一个节点；若后端后续补了该菜单，这里会自动跳过（不会重复）。
 */
const LOCAL_MENUS = [
  {
    id: 'local-ai-config',
    parentId: '4',
    name: 'AI 配置',
    path: 'config',
    component: 'ai/config/index',
    icon: 'setting',
    type: 2,
    perms: 'ai:config:view',
    hidden: 0,
    children: []
  }
]

/** 把本地补充菜单挂到对应父节点（找不到父节点则挂到根） */
function injectLocalMenus(tree) {
  const menus = JSON.parse(JSON.stringify(tree || []))
  menus.forEach((node) => {
    node.children = node.children || []
  })
  LOCAL_MENUS.forEach((local) => {
    const exists = menus.some((node) => (node.children || []).some((c) => c.component === local.component))
    if (exists) return
    const parent = menus.find((node) => String(node.id) === String(local.parentId))
    if (parent) parent.children.push({ ...local })
    else menus.push({ ...local })
  })
  return menus
}

/**
 * v1.1 契约菜单节点 → 前端内部结构
 *
 * 契约字段：{ menuId, parentId, menuName, path, component, menuType, perms, icon, visible, children }
 * 内部字段：{ id, parentId, name, path, component, type, perms, icon, hidden, children }
 * menuType：M 目录 / C 菜单 / F 按钮；visible：'0' 显示 / '1' 隐藏
 */
function normalizeMenus(nodes) {
  return (nodes || []).map((node) => ({
    id: String(node.menuId ?? node.id ?? ''),
    parentId: String(node.parentId ?? '0'),
    name: node.menuName ?? node.name ?? '',
    path: node.path || '',
    component: node.component || null,
    queryParam: node.queryParam || null,
    type: node.menuType ? { M: 1, C: 2, F: 3 }[node.menuType] ?? 2 : node.type ?? 2,
    perms: node.perms || null,
    icon: node.icon || null,
    hidden: String(node.visible ?? '0') === '1' || Number(node.hidden) === 1 ? 1 : 0,
    children: normalizeMenus(node.children)
  }))
}

/** 菜单树补全 fullPath（父 /system + 子 user → /system/user），供侧边栏与面包屑使用 */
function withFullPath(nodes, base = '') {
  return (nodes || []).map((node) => {
    const fullPath = node.path?.startsWith('/')
      ? node.path
      : `${base.replace(/\/+$/, '')}/${node.path || ''}`
    const normalized = {
      ...node,
      fullPath: fullPath.replace(/\/+$/, '') || '/',
      children: withFullPath(node.children || [], fullPath)
    }
    return normalized
  })
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: null,
    roles: [],
    perms: [],
    menus: [],
    routesLoaded: false,
    routeNames: []
  }),

  getters: {
    /** 侧边栏渲染用的菜单（已补全 fullPath、过滤 hidden=1） */
    sidebarMenus: (state) => {
      const tree = withFullPath(state.menus)
      const filterHidden = (nodes) =>
        nodes
          .filter((node) => Number(node.hidden) !== 1)
          .map((node) => ({ ...node, children: filterHidden(node.children || []) }))
      return filterHidden(tree)
    },
    nickName: (state) => state.userInfo?.nickname || state.userInfo?.username || '未登录',
    avatar: (state) => state.userInfo?.avatar || ''
  },

  actions: {
    /** 登录：成功即写入 token（Pinia + localStorage） */
    async login(form) {
      const data = await loginApi(form)
      const token = data?.token || ''
      setToken(token)
      this.token = token
      this.userInfo = data?.userInfo || null
      this.roles = data?.userInfo?.roles || []
      this.perms = data?.userInfo?.perms || []
      return data
    },

    /** 刷新页面后重新拉取当前用户信息 */
    async fetchUserInfo() {
      const info = await getMe()
      this.userInfo = info
      this.roles = info?.roles || []
      this.perms = info?.perms || []
      return info
    },

    /** 拉取动态菜单 + 注册动态路由（刷新页面后必须重新执行，否则路由会丢失） */
    async fetchMenus() {
      const tree = await getMenus()
      this.menus = injectLocalMenus(normalizeMenus(tree))
      const routes = buildRoutes(this.menus)
      routes.forEach((route) => {
        // 所有业务页面统一挂在 Layout 布局下
        router.addRoute('Layout', route)
        this.routeNames.push(route.name)
      })
      this.routesLoaded = true
      return routes
    },

    /** 退出：通知后端 → 清理本地状态与动态路由 */
    async logout() {
      try {
        if (this.token) await logoutApi()
      } catch (error) {
        // 退出接口失败也要清理本地登录态
        console.warn('[user] 退出接口调用失败，仍清理本地登录态', error?.message)
      }
      this.resetState()
      router.push('/login')
    },

    resetState() {
      this.token = ''
      this.userInfo = null
      this.roles = []
      this.perms = []
      this.menus = []
      this.routesLoaded = false
      removeToken()
      this.routeNames.forEach((name) => {
        if (name && router.hasRoute(name)) router.removeRoute(name)
      })
      this.routeNames = []
    },

    /** 是否拥有某权限（超管 *:*:* 直接放行） */
    hasPerm(perm) {
      if (!perm) return true
      const perms = this.perms || []
      if (perms.includes('*:*:*')) return true
      return perms.includes(perm)
    },

    /** 角色是否匹配（v-hasRole 用） */
    hasRole(role) {
      if (!role) return true
      const roles = this.roles || []
      if (roles.includes('admin')) return true
      return roles.includes(role)
    }
  }
})

export default useUserStore
