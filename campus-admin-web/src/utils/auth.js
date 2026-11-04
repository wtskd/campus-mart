/**
 * 令牌的本地存储（localStorage）
 * 说明：Pinia store 只保存运行期用户信息，token 必须同时落 localStorage，
 * 否则刷新页面后 Pinia 重建会丢失登录态。
 */
const TOKEN_KEY = 'CampusMart-Admin-Token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}
