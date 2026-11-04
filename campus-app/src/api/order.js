/**
 * 订单（契约 v1.1 §4.6，需登录）
 * 状态：0 待确认 / 1 待交付 / 2 已完成 / 3 已取消 / 4 退款中 / 5 已退款
 */
import request from '@/utils/request'

/** 下单 { itemId, tradePlace, buyerRemark } */
export function createOrder(data) {
  return request.post('/item/app/order', data, { loading: true, loadingText: '提交订单...' })
}

/** 买家付款（0→1） */
export function payOrder(orderId) {
  return request.post(`/item/app/order/${orderId}/pay`)
}

/** 卖家确认交付（0/1→1） */
export function deliverOrder(orderId) {
  return request.post(`/item/app/order/${orderId}/deliver`)
}

/** 买家确认收货（1→2） */
export function confirmOrder(orderId) {
  return request.post(`/item/app/order/${orderId}/confirm`)
}

/** 取消订单（0/1→3） */
export function cancelOrder(orderId, cancelReason) {
  return request.post(`/item/app/order/${orderId}/cancel`, cancelReason ? { cancelReason } : {})
}

/** 我的订单 role=buy 我买到的 / role=sell 我卖出的 */
export function getMyOrders(params = {}) {
  return request.get('/item/app/order/my', { params })
}

/** 订单详情 */
export function getOrderDetail(orderId) {
  return request.get(`/item/app/order/${orderId}`)
}
