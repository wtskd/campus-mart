/**
 * 收藏（契约 v1.1 §4.4，需登录）
 */
import request from '@/utils/request'

/** 收藏（幂等） */
export function addFavorite(itemId) {
  return request.post(`/item/app/favorite/${itemId}`)
}

/** 取消收藏 */
export function removeFavorite(itemId) {
  return request.del(`/item/app/favorite/${itemId}`)
}

/** 我的收藏分页 */
export function getMyFavorites(params = {}) {
  return request.get('/item/app/favorite/my', { params })
}

/** 是否已收藏 → data: true/false */
export function checkFavorite(itemId) {
  return request.get(`/item/app/favorite/check/${itemId}`, { silent: true })
}

/** 切换收藏状态（先查再改，避免重复收藏） */
export async function toggleFavorite(itemId, favorited) {
  if (favorited) {
    await removeFavorite(itemId)
    return false
  }
  await addFavorite(itemId)
  return true
}
