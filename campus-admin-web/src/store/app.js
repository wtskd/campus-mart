import { defineStore } from 'pinia'

const SIDEBAR_KEY = 'CampusMart-Sidebar-Collapsed'

/**
 * 应用级 UI 状态：侧边栏折叠 / 标签页
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: localStorage.getItem(SIDEBAR_KEY) === '1',
    // 已打开的标签页：[{ path, title, name }]
    visitedViews: []
  }),

  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem(SIDEBAR_KEY, this.sidebarCollapsed ? '1' : '0')
    },

    addView(route) {
      if (!route.meta?.title) return
      if (this.visitedViews.some((v) => v.path === route.path)) return
      this.visitedViews.push({
        path: route.path,
        title: route.meta.title,
        name: route.name
      })
    },

    removeView(path) {
      this.visitedViews = this.visitedViews.filter((v) => v.path !== path)
    },

    removeOtherViews(path) {
      this.visitedViews = this.visitedViews.filter((v) => v.path === path)
    },

    removeAllViews() {
      this.visitedViews = []
    }
  }
})

export default useAppStore
