/**
 * 消息（契约 v1.1 §4.8，需登录）
 */
import request from '@/utils/request'

/** 我的消息分页（可带 msgType 过滤） */
export function getMyMessages(params = {}) {
  return request.get('/item/app/message/my', { params })
}

/** 未读消息数 */
export function getUnreadCount() {
  return request.get('/item/app/message/unread-count', { silent: true })
}

/** 标记单条已读 */
export function readMessage(messageId) {
  return request.put(`/item/app/message/read/${messageId}`, {}, { silent: true })
}

/** 全部已读 */
export function readAllMessages() {
  return request.put('/item/app/message/read-all', {}, { silent: true })
}
