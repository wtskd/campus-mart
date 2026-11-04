/**
 * 评论（契约 v1.1 §4.5）
 * 发表评价必须带 orderId（只能评价已完成订单）
 */
import request from '@/utils/request'

/** 商品评论分页 */
export function getCommentList(itemId, params = {}) {
  return request.get(`/item/app/comment/list/${itemId}`, { params })
}

/** 发表评价 { orderId, itemId, rating, content, images } */
export function addComment(data) {
  return request.post('/item/app/comment', data, { loading: true, loadingText: '提交评价...' })
}
