/**
 * 举报（契约 v1.1 §4.7）
 * targetType 目前只支持 item
 */
import request from '@/utils/request'

export function reportItem(targetId, reason, description) {
  return request.post('/item/app/report', {
    targetType: 'item',
    targetId,
    reason,
    description
  })
}
