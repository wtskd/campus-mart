import request from '@/utils/request'
import { normalizePage } from './system'

/**
 * AI 管理接口（campus-ai，网关前缀 /api/ai）
 * 契约 v1.1 §5：日志 / 统计 / Prompt 模板 / 知识库
 */
const PREFIX = '/ai'

/* -------- 日志与统计 -------- */
export const getAiLogPage = (params) =>
  request.get(`${PREFIX}/log/page`, { params }).then((data) => normalizePage(data, params))
/** AI 消耗统计：{ totalCalls, successCalls, avgCostMs, totalTokens, byScene, byDay } */
export const getAiStatOverview = () => request.get(`${PREFIX}/stat/overview`)
/** 已配置的 Prompt 场景 */
export const getAiSceneList = () => request.get(`${PREFIX}/scene/list`)

/* -------- Prompt 模板 -------- */
export const getPromptPage = (params) =>
  request.get(`${PREFIX}/prompt/page`, { params }).then((data) => normalizePage(data, params))
export const addPrompt = (data) => request.post(`${PREFIX}/prompt`, data)
export const updatePrompt = (data) => request.put(`${PREFIX}/prompt`, data)
export const deletePrompt = (promptId) => request.delete(`${PREFIX}/prompt/${promptId}`)

/* -------- 知识库 -------- */
export const getKnowledgePage = (params) =>
  request.get(`${PREFIX}/knowledge/page`, { params }).then((data) => normalizePage(data, params))
export const addKnowledge = (data) => request.post(`${PREFIX}/knowledge`, data)
export const updateKnowledge = (data) => request.put(`${PREFIX}/knowledge`, data)
export const deleteKnowledge = (knowledgeId) => request.delete(`${PREFIX}/knowledge/${knowledgeId}`)

/* -------- 在线试用（前台 AI 能力的后台调试入口，答辩演示用） -------- */
export const aiChat = (data) => request.post(`${PREFIX}/chat`, data)
export const aiCopywrite = (data) => request.post(`${PREFIX}/copywrite`, data)
export const aiModerate = (data) => request.post(`${PREFIX}/moderate`, data)
