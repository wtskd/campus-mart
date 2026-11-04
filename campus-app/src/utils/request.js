/**
 * uni.request 统一封装（契约 v1.1）
 * - 自动携带 Authorization: Bearer <token>
 * - 统一解包 Result<T>：code === 200 时 resolve(data)
 * - 401：清登录态 → 跳登录页
 * - 1001：AI 降级，resolve 兜底数据并提示已使用本地模板
 * - 无 mock 数据，全部直连网关（后端未就绪时给出空态 / toast）
 */
import config from '@/config'

const TOKEN_KEY = 'cm_token'
const USER_KEY = 'cm_user'

let redirecting = false

export function getToken() {
  try {
    return uni.getStorageSync(TOKEN_KEY) || ''
  } catch (e) {
    return ''
  }
}

export function setToken(token) {
  try {
    if (token) uni.setStorageSync(TOKEN_KEY, token)
    else uni.removeStorageSync(TOKEN_KEY)
  } catch (e) {
    /* ignore */
  }
}

export function getStorageUser() {
  try {
    return uni.getStorageSync(USER_KEY) || null
  } catch (e) {
    return null
  }
}

export function setStorageUser(user) {
  try {
    if (user) uni.setStorageSync(USER_KEY, user)
    else uni.removeStorageSync(USER_KEY)
  } catch (e) {
    /* ignore */
  }
}

export function clearAuthStorage() {
  setToken('')
  setStorageUser(null)
}

function toast(title) {
  uni.showToast({ title, icon: 'none', duration: 2200 })
}

/** 401 跳登录（防重复跳转） */
function redirectToLogin() {
  clearAuthStorage()
  if (redirecting) return
  redirecting = true
  toast('登录已过期，请重新登录')
  setTimeout(() => {
    uni.reLaunch({
      url: '/pages/login/login',
      complete: () => {
        setTimeout(() => {
          redirecting = false
        }, 1000)
      }
    })
  }, 300)
}

/** 把 params 拼到 url 后面（过滤空值） */
function buildUrl(url, params) {
  if (!params) return url
  const query = Object.keys(params)
    .filter((k) => params[k] !== undefined && params[k] !== null && params[k] !== '')
    .map((k) => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`)
    .join('&')
  if (!query) return url
  return url + (url.indexOf('?') >= 0 ? '&' : '?') + query
}

/**
 * 发起请求
 * @param {Object} options { url, method, data, params, header, auth, silent, loading, loadingText, timeout }
 */
export function request(options = {}) {
  const {
    url,
    method = 'GET',
    data,
    params,
    header = {},
    auth = true,
    silent = false,
    loading = false,
    loadingText = '加载中...',
    timeout = config.timeout
  } = options

  const fullUrl = buildUrl(config.baseUrl + url, params)
  const headers = { 'Content-Type': 'application/json', ...header }
  const token = getToken()
  if (auth && token) headers.Authorization = `Bearer ${token}`

  if (loading) uni.showLoading({ title: loadingText, mask: true })

  return new Promise((resolve, reject) => {
    const done = () => {
      if (loading) uni.hideLoading()
    }
    uni.request({
      url: fullUrl,
      method: method.toUpperCase(),
      data,
      header: headers,
      timeout,
      success: (res) => {
        const { statusCode, data: body } = res
        // 网关鉴权失败（统一 401）
        if (statusCode === 401) {
          done()
          redirectToLogin()
          reject({ code: 401, msg: '未登录或登录已过期' })
          return
        }
        if (statusCode === 429) {
          done()
          if (!silent) toast('请求太频繁，稍后再试')
          reject({ code: 429, msg: '请求太频繁' })
          return
        }
        if (statusCode >= 500) {
          done()
          if (!silent) toast('系统繁忙，请稍后再试')
          reject({ code: statusCode, msg: '服务器内部错误' })
          return
        }
        // 非 JSON（例如网关原始错误）
        if (!body || typeof body !== 'object' || typeof body.code === 'undefined') {
          done()
          if (!silent) toast('接口返回异常')
          reject({ code: statusCode, msg: '接口返回异常' })
          return
        }
        done()
        if (body.code === 200) {
          resolve(body.data)
          return
        }
        if (body.code === 1001) {
          // AI 服务降级：后端仍返回可用兜底结果
          if (!silent) toast('AI 繁忙，已使用本地模板')
          resolve(body.data)
          return
        }
        if (body.code === 401) {
          redirectToLogin()
          reject({ code: 401, msg: body.msg })
          return
        }
        if (!silent) toast(body.msg || '请求失败')
        reject({ code: body.code, msg: body.msg || '请求失败' })
      },
      fail: (err) => {
        done()
        if (!silent) toast('网络异常，请确认后端服务已启动')
        reject({ code: -1, msg: (err && err.errMsg) || '网络异常' })
      }
    })
  })
}

request.get = (url, options = {}) => request({ ...options, url, method: 'GET' })
request.post = (url, data, options = {}) => request({ ...options, url, method: 'POST', data })
request.put = (url, data, options = {}) => request({ ...options, url, method: 'PUT', data })
request.del = (url, data, options = {}) => request({ ...options, url, method: 'DELETE', data })

/**
 * 文件上传（POST /system/file/upload，字段名 file）
 * 返回 { fileName, url, size }；失败 reject
 */
export function uploadFile(filePath) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.uploadFile({
      url: config.baseUrl + '/system/file/upload',
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          if (body && body.code === 200) resolve(body.data)
          else reject({ code: body && body.code, msg: (body && body.msg) || '上传失败' })
        } catch (e) {
          reject({ code: -1, msg: '上传结果解析失败' })
        }
      },
      fail: () => reject({ code: -1, msg: '上传失败，请稍后再试' })
    })
  })
}

export default request
