/**
 * AI 能力（契约 v1.1 §5）
 * 降级策略：接口永远返回 200 + 兜底结果，provider=mock/rule 表示未走真实大模型
 */
import config from '@/config'
import request from '@/utils/request'

/** 一键发布文案 { keywords, category, condition, originalPrice, price, remark }
 *  → { title, summary, description, tags[], suggestPrice, provider, costMs } */
export function aiCopywrite(data) {
  return request.post('/ai/copywrite', data, {
    timeout: config.aiTimeout,
    loading: true,
    loadingText: 'AI 正在撰写文案...'
  })
}

/** 智能客服 RAG { question } → { answer, sources:[{title,score}], provider, costMs } */
export function aiChat(data) {
  return request.post('/ai/chat', data, { timeout: config.aiTimeout, silent: true })
}

/** 首页个性化推荐（登录后自动带 userId，未登录热度兜底） */
export function aiRecommend(limit = 8) {
  return request.get('/ai/recommend', { params: { limit }, silent: true })
}

/** 内容审核 { title, description } */
export function aiModerate(data) {
  return request.post('/ai/moderate', data, { silent: true })
}

/** 智能估价 { itemId } 或 { title, categoryId, itemCondition } */
export function aiPrice(data) {
  return request.post('/ai/price', data, { timeout: config.aiTimeout, silent: true })
}
