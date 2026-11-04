import request from '@/utils/request'

/**
 * 认证模块接口（campus-auth，网关前缀 /api/auth）
 */

/** 图形验证码 → { key, image } */
export const getCaptcha = () => request.get('/auth/captcha')

/** 账号密码登录 → { token, expiresIn, userInfo } */
export const login = (data) => request.post('/auth/login', data)

/** 退出登录 */
export const logout = () => request.post('/auth/logout')

/** 当前登录用户信息（含 roles/perms） */
export const getMe = () => request.get('/auth/me')

/** 当前用户的动态菜单树 */
export const getMenus = () => request.get('/auth/menus')
