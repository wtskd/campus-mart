import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'

/**
 * 全局 axios 实例（直连后端网关，契约 v1.1）
 *
 * - baseURL 来自环境变量 VITE_API_BASE（默认 /api，开发态由 vite proxy 转发到 http://localhost:8081）
 * - 请求拦截：统一携带 Authorization: Bearer <token>
 * - 响应拦截：按统一响应体 { code, msg, data } 处理：
 *   200 直接返回 data；401 清理登录态跳登录；其余用 ElMessage 提示并 reject
 */
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 20000
})

// 防止多个 401 同时弹出多个提示 / 多次跳转
let redirecting = false

/** 清理登录态并跳转登录页（带 redirect，登录后可回到原页面） */
function toLogin() {
  if (redirecting) return
  redirecting = true
  removeToken()
  import('@/router')
    .then(({ default: router }) => {
      const current = router.currentRoute.value
      const redirect = current.path === '/login' ? '/dashboard' : current.fullPath
      router
        .replace({ path: '/login', query: { redirect } })
        .catch(() => {})
        .finally(() => {
          redirecting = false
        })
    })
    .catch(() => {
      window.location.href = '/login'
    })
}

// ---------------- 请求拦截：统一携带 Authorization ----------------
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---------------- 响应拦截：统一处理业务 code ----------------
service.interceptors.response.use(
  (response) => {
    // 文件下载（CSV 导出）直接返回二进制内容
    if (response.config?.responseType === 'blob') return response.data

    const res = response.data
    // 后端未按统一响应体返回（例如静态资源代理），原样返回
    if (res == null || typeof res !== 'object' || !('code' in res)) return res

    if (res.code === 200) return res.data

    if (res.code === 401) {
      ElMessage.error(res.msg || '登录已失效，请重新登录')
      toLogin()
      return Promise.reject(new Error(res.msg || '登录已失效'))
    }

    if (res.code === 403) {
      ElMessage.error(res.msg || '没有操作权限')
      return Promise.reject(new Error(res.msg || '没有操作权限'))
    }

    if (res.code === 1001) {
      // AI 服务不可用（后端已降级），用警告而不是报错，避免惊吓用户
      ElMessage.warning(res.msg || 'AI 服务暂时不可用，已使用本地能力降级')
      return Promise.reject(new Error(res.msg || 'AI 服务不可用'))
    }

    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    // 网络层错误（超时、断网、5xx、401 等）
    const status = error.response?.status
    if (status === 401) {
      toLogin()
      return Promise.reject(error)
    }
    if (status === 403) {
      ElMessage.error('没有访问权限（403）')
    } else if (status === 404) {
      ElMessage.error('接口不存在（404），请确认后端本模块已部署')
    } else if (status >= 500) {
      ElMessage.error(`服务器开小差了（${status}）`)
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (!error.response) {
      ElMessage.error('无法连接服务器，请确认后端网关（8081）已启动')
    } else {
      ElMessage.error(error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

/**
 * 保存/blob 下载：把后端返回的二进制内容存为文件
 * @param {Blob} blob 二进制内容
 * @param {string} filename 文件名
 */
export function downloadBlob(blob, filename = 'export.csv') {
  const url = window.URL.createObjectURL(blob instanceof Blob ? blob : new Blob([blob]))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export default service
