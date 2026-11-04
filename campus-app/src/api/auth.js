/**
 * 认证接口（契约 v1.1 §2）
 */
import request from '@/utils/request'

/** 小程序/App 登录（免图形验证码）POST /auth/app/login */
export function appLogin(data) {
  return request.post('/auth/app/login', data, { loading: true, loadingText: '登录中...' })
}

/** 退出登录 */
export function logout() {
  return request.post('/auth/logout', {}, { silent: true })
}

/** 当前登录用户信息 */
export function getMe() {
  return request.get('/auth/me', { silent: true })
}
