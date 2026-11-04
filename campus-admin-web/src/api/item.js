import request from '@/utils/request'
import { normalizePage, listToTree } from './system'

/**
 * 商品域接口（campus-item，网关前缀 /api/item）
 * 契约 v1.1 §4：后台商品 /item/item，订单 /item/order，评论 /item/comment，举报 /item/report，
 * 公告 /item/notice，轮播 /item/banner
 */
const PREFIX = '/item'

/* ============================== 分类 ============================== */
export const getCategoryList = (params = {}) =>
  request.get(`${PREFIX}/category/list`, { params }).then((data) => (Array.isArray(data) ? data : []))
/** 分类树：后端支持 tree=true，兼容返回扁平列表的情况 */
export const getCategoryTree = () =>
  getCategoryList({ tree: true }).then((data) => listToTree(data, 'categoryId', 'parentId'))
export const addCategory = (data) => request.post(`${PREFIX}/category`, data)
export const updateCategory = (data) => request.put(`${PREFIX}/category`, data)
export const updateCategoryStatus = (data) => request.put(`${PREFIX}/category/status`, data)
export const deleteCategory = (categoryId) => request.delete(`${PREFIX}/category/${categoryId}`)

/* ============================== 商品（后台） ============================== */
export const getItemPage = (params) =>
  request.get(`${PREFIX}/item/page`, { params }).then((data) => normalizePage(data, params))
/** 详情：后台无专用详情接口，回退到前台详情（公开可见商品） */
export const getItem = (itemId) => request.get(`${PREFIX}/app/item/${itemId}`)
/** 审核：{ itemId, pass: true|false, auditRemark } */
export const auditItem = (data) => request.post(`${PREFIX}/item/audit`, data)
/** 改状态：{ itemId, status } */
export const updateItemStatus = (data) => request.put(`${PREFIX}/item/status`, data)
export const deleteItem = (itemId) => request.delete(`${PREFIX}/item/${itemId}`)

/* ============================== 订单（后台） ============================== */
export const getOrderPage = (params) =>
  request.get(`${PREFIX}/order/page`, { params }).then((data) => normalizePage(data, params))
/** 后台改状态：{ orderId, status }（后端严格校验流转） */
export const updateOrderStatus = (data) => request.put(`${PREFIX}/order/status`, data)
/** 退款：{ orderId, reason } */
export const refundOrder = (data) => request.put(`${PREFIX}/order/refund`, data)

/* ============================== 评论（后台） ============================== */
export const getCommentPage = (params) =>
  request.get(`${PREFIX}/comment/page`, { params }).then((data) => normalizePage(data, params))
/** 审核：{ commentId, status: '0' 通过 / '1' 隐藏 } */
export const auditComment = (data) => request.put(`${PREFIX}/comment/audit`, data)
export const deleteComment = (commentId) => request.delete(`${PREFIX}/comment/${commentId}`)

/* ============================== 举报（后台） ============================== */
export const getReportPage = (params) =>
  request.get(`${PREFIX}/report/page`, { params }).then((data) => normalizePage(data, params))
/** 处理：{ reportId, status: 1 成立 / 2 不成立, handleRemark } */
export const handleReport = (data) => request.put(`${PREFIX}/report/handle`, data)

/* ============================== 公告 ============================== */
export const getNoticePage = (params) =>
  request.get(`${PREFIX}/notice/page`, { params }).then((data) => normalizePage(data, params))
export const getNotice = (noticeId) => request.get(`${PREFIX}/notice/${noticeId}`)
export const addNotice = (data) => request.post(`${PREFIX}/notice`, data)
export const updateNotice = (data) => request.put(`${PREFIX}/notice`, data)
export const deleteNotice = (noticeId) => request.delete(`${PREFIX}/notice/${noticeId}`)

/* ============================== 轮播图 ============================== */
/**
 * 契约的轮播只有 /banner/list（不分页）。
 * 这里先取全量，再在前端做筛选 + 分页，保持页面与其它列表一致的分页体验。
 */
export const getBannerPage = (params = {}) =>
  request.get(`${PREFIX}/banner/list`).then((data) => {
    let list = Array.isArray(data) ? data : []
    if (params.title) list = list.filter((row) => String(row.title || '').includes(params.title))
    if (params.status !== '' && params.status !== undefined && params.status !== null) {
      list = list.filter((row) => String(row.status) === String(params.status))
    }
    return normalizePage(list, params)
  })
export const addBanner = (data) => request.post(`${PREFIX}/banner`, data)
export const updateBanner = (data) => request.put(`${PREFIX}/banner`, data)
export const deleteBanner = (bannerId) => request.delete(`${PREFIX}/banner/${bannerId}`)

/* ============================== 消息（后台） ============================== */
/** 全量消息分页：{ userId, msgType, isRead, beginTime, endTime } + current/size */
export const getMessagePage = (params) =>
  request.get(`${PREFIX}/message/page`, { params }).then((data) => normalizePage(data, params))
