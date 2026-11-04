/**
 * 登录态（Pinia）
 * 契约 v1.1：用户字段 nickname（全小写）、userId 为字符串
 */
import { defineStore } from 'pinia'
import { appLogin, logout as apiLogout, getMe } from '@/api/auth'
import {
  getToken,
  setToken,
  getStorageUser,
  setStorageUser,
  clearAuthStorage
} from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null,
    inited: false
  }),
  getters: {
    isLogin: (state) => !!state.token,
    userId: (state) => (state.userInfo ? state.userInfo.userId : ''),
    nickname: (state) => (state.userInfo ? state.userInfo.nickname || state.userInfo.username : '未登录'),
    avatar: (state) =>
      state.userInfo && state.userInfo.avatar ? state.userInfo.avatar : '/static/mock/avatar-default.png',
    deptName: (state) => (state.userInfo ? state.userInfo.deptName || '' : ''),
    roles: (state) => (state.userInfo && state.userInfo.roles) || []
  },
  actions: {
    /** App 启动时从本地缓存恢复登录态 */
    initFromStorage() {
      if (this.inited) return
      this.token = getToken() || ''
      this.userInfo = getStorageUser()
      this.inited = true
      if (this.token && !this.userInfo) this.fetchMe()
    },

    /** 账号密码登录（免验证码） */
    async loginByApp(username, password) {
      const data = await appLogin({ username, password })
      this.token = data.token
      this.userInfo = data.userInfo || null
      setToken(data.token)
      setStorageUser(this.userInfo)
      // 登录接口未返回用户信息时兜底拉一次
      if (!this.userInfo) await this.fetchMe()
      return data
    },

    /** 拉取当前用户信息 */
    async fetchMe() {
      try {
        const info = await getMe()
        this.userInfo = info
        setStorageUser(info)
        return info
      } catch (e) {
        return null
      }
    },

    /** 退出登录 */
    async logout() {
      try {
        if (this.token) await apiLogout()
      } catch (e) {
        /* 后端异常也要清本地登录态 */
      }
      this.clearLoginState()
    },

    clearLoginState() {
      this.token = ''
      this.userInfo = null
      clearAuthStorage()
    },

    /** 未登录时去登录页，返回是否已登录 */
    requireLogin() {
      if (this.isLogin) return true
      uni.showToast({ title: '请先登录', icon: 'none' })
      setTimeout(() => uni.navigateTo({ url: '/pages/login/login' }), 300)
      return false
    }
  }
})
