/**
 * 动态路由构建
 *
 * 后端 GET /auth/menus 返回菜单树，节点形如：
 *   { id, parentId, name, path, component, icon, type, perms, hidden, children }
 *   type: 1 目录 / 2 菜单 / 3 按钮
 *
 * 前端用 import.meta.glob 预收集 views 下所有页面组件，再按 component 字段映射成路由，
 * 最后通过 router.addRoute('Layout', route) 挂到布局路由下 —— 实现「后端配菜单、前端出页面」。
 */
const viewModules = import.meta.glob('../views/**/*.vue')

/** component 字段 → 异步组件 */
export function resolveView(component) {
  if (!component) return null
  const candidates = [`../views/${component}.vue`, `../views/${component}/index.vue`]
  for (const key of candidates) {
    if (viewModules[key]) return viewModules[key]
  }
  console.warn(`[router] 菜单配置的组件不存在：views/${component}.vue，已降级为 404 页面`)
  return () => import('@/views/error/404.vue')
}

/** 拼接父子路径：('', '/system') -> '/system'；('/system', 'user') -> '/system/user' */
function joinPath(parent, path) {
  if (!path) return parent || '/'
  if (path.startsWith('/')) return path
  const left = (parent || '').replace(/\/+$/, '')
  return `${left}/${path}`
}

/**
 * 菜单树 → 路由数组
 * @param {Array} tree 菜单树
 * @param {string} parentPath 父级路径
 */
export function buildRoutes(tree = [], parentPath = '') {
  const routes = []
  for (const node of tree) {
    const fullPath = joinPath(parentPath, node.path)
    const children = Array.isArray(node.children) ? node.children : []

    // 目录：递归构建子路由
    if (children.length) {
      routes.push(...buildRoutes(children, fullPath))
    }

    // 只有「有页面组件」的节点才生成路由（目录、按钮不生成）
    if (node.component && node.component !== 'Layout' && node.type !== 3) {
      routes.push({
        path: fullPath.replace(/^\//, ''), // addRoute 到 Layout 下，使用相对路径
        name: `Menu${node.id}`,
        component: resolveView(node.component),
        meta: {
          title: node.name,
          icon: node.icon,
          perms: node.perms,
          menuId: node.id,
          hidden: Number(node.hidden) === 1
        }
      })
    }
  }
  return routes
}

export default buildRoutes
