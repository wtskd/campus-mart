/**
 * 商品 / 分类 / 公告 / 轮播（契约 v1.1 §4.1 §4.3 §4.9）
 */
import request from '@/utils/request'

/** 在售商品分页：keyword / categoryId / minPrice / maxPrice / itemCondition / sort=new|priceAsc|priceDesc|hot */
export function getItemPage(params = {}) {
  return request.get('/item/app/item/page', { params })
}

/** 商品详情 */
export function getItemDetail(itemId) {
  return request.get(`/item/app/item/${itemId}`)
}

/** 推荐（先走 AI 个性化，AI 不可用时由后端热度兜底） */
export function getRecommend(limit = 8) {
  return request.get('/item/app/item/recommend', { params: { limit } })
}

/** 我发布的商品（需登录） */
export function getMyItems(params = {}) {
  return request.get('/item/app/item/my', { params })
}

/** 发布商品（需登录，后端会做 AI 内容审核） */
export function publishItem(data) {
  return request.post('/item/app/item', data, { loading: true, loadingText: '发布中...' })
}

/** 编辑商品（仅本人，status ∈ 1/2/5） */
export function updateItem(data) {
  return request.put('/item/app/item', data, { loading: true, loadingText: '保存中...' })
}

/** 下架商品 POST /item/app/item/{itemId}/off-shelf */
export function offShelfItem(itemId) {
  return request.post(`/item/app/item/${itemId}/off-shelf`)
}

/** 分类树（两级） */
export function getCategoryTree() {
  return request.get('/item/app/category/tree')
}

/** 轮播图（匿名可访问） */
export function getBannerList() {
  return request.get('/item/app/banner/list')
}

/** 公告分页 */
export function getNoticeList(params = {}) {
  return request.get('/item/app/notice/list', { params })
}

/** 公告详情 */
export function getNoticeDetail(noticeId) {
  return request.get(`/item/app/notice/${noticeId}`)
}
