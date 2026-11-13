import request from '@/utils/request'

/**
 * 系统管理接口（campus-system，网关前缀 /api/system）
 *
 * 契约 v1.1 约定：
 * - 字段名 = 数据库列名驼峰；status 为 char(1) 字符串（'0' 正常 / '1' 停用）
 * - 列表统一分页参数 current / size，返回 PageResult
 * - 前端在此层做「响应归一化」，页面只关心业务字段
 */

const PREFIX = '/system'

/* ============================== 通用工具 ============================== */

/**
 * 把后端返回统一成 { records, total, current, size, pages }
 * 兼容：PageResult 对象 / 纯数组（后端个别接口不分页）
 */
export function normalizePage(data, params = {}) {
  const current = Number(params.current || 1)
  const size = Number(params.size || 10)
  if (Array.isArray(data)) {
    const start = (current - 1) * size
    return {
      records: data.slice(start, start + size),
      total: data.length,
      current,
      size,
      pages: Math.ceil(data.length / size) || 1
    }
  }
  const records = data?.records || data?.rows || data?.list || []
  // 后端 Long 统一序列化成字符串（避免 JS 精度丢失），这里转回数字给分页组件用
  const total = Number(data?.total ?? records.length) || 0
  return {
    records,
    total,
    current: Number(data?.current ?? current) || current,
    size: Number(data?.size ?? size) || size,
    pages: Number(data?.pages) || Math.ceil(total / size) || 1
  }
}

/** 扁平列表 → 树（parentId 为 0 的作为根） */
export function listToTree(list = [], idKey = 'menuId', parentKey = 'parentId') {
  if (!Array.isArray(list)) return []
  // 后端若已返回树，直接使用
  if (list.some((item) => Array.isArray(item.children) && item.children.length)) return list
  const map = new Map()
  list.forEach((item) => map.set(String(item[idKey]), { ...item, children: [] }))
  const roots = []
  map.forEach((node) => {
    const parent = map.get(String(node[parentKey]))
    if (parent && String(node[parentKey]) !== '0') parent.children.push(node)
    else roots.push(node)
  })
  return roots
}

/* ============================== 用户 ============================== */
export const getUserPage = (params) =>
  request.get(`${PREFIX}/user/list`, { params }).then((data) => normalizePage(data, params))
export const getUser = (userId) => request.get(`${PREFIX}/user/${userId}`)
export const addUser = (data) => request.post(`${PREFIX}/user`, data)
export const updateUser = (data) => request.put(`${PREFIX}/user`, data)
export const deleteUser = (userIds) => request.delete(`${PREFIX}/user/${userIds}`)
export const resetUserPwd = (data) => request.put(`${PREFIX}/user/resetPwd`, data)
export const changeUserStatus = (userId, status) =>
  request.put(`${PREFIX}/user/changeStatus`, { userId, status })
/**
 * 部门树（完整字段版）
 *
 * 注意：`/user/deptTree` 返回的是 { id, label } 结构，只适合 el-tree-select 的默认 props；
 * 学院管理表格、用户表单（props 用 deptName/deptId）需要 deptId/deptName/status 等完整字段，
 * 因此这里统一走 `/dept/list` 在本地组装成树，两处共用同一份数据。
 */
export const getDeptTree = () => getDeptTreeFull()

/* ============================== 角色 ============================== */
export const getRolePage = (params) =>
  request.get(`${PREFIX}/role/list`, { params }).then((data) => normalizePage(data, params))
export const addRole = (data) => request.post(`${PREFIX}/role`, data)
export const updateRole = (data) => request.put(`${PREFIX}/role`, data)
export const deleteRole = (roleIds) => request.delete(`${PREFIX}/role/${roleIds}`)
export const changeRoleStatus = (roleId, status) =>
  request.put(`${PREFIX}/role/changeStatus`, { roleId, status })
/** 角色下拉选项（用户分配角色用） */
export const getRoleList = () => request.get(`${PREFIX}/role/optionselect`)
/** 角色已授权的菜单 id 数组 */
export const getRoleMenuIds = (roleId) => request.get(`${PREFIX}/role/${roleId}/menuIds`)
/** 保存角色菜单权限：{ roleId, menuIds } */
export const updateRoleMenu = (roleId, menuIds) =>
  request.put(`${PREFIX}/role/menu`, { roleId, menuIds })

/* ============================== 菜单 ============================== */
/** 菜单列表：契约是扁平列表，这里转成树给「菜单管理」树表格使用 */
export const getMenuTree = () =>
  request.get(`${PREFIX}/menu/list`).then((data) => listToTree(Array.isArray(data) ? data : [], 'menuId', 'parentId'))
export const addMenu = (data) => request.post(`${PREFIX}/menu`, data)
export const updateMenu = (data) => request.put(`${PREFIX}/menu`, data)
export const deleteMenu = (menuId) => request.delete(`${PREFIX}/menu/${menuId}`)
/** 菜单下拉树（角色授权 / 菜单上级选择） */

/* ============================== 部门 / 学院 ============================== */
export const getDeptTreeFull = () =>
  request
    .get(`${PREFIX}/dept/list`)
    .then((data) => listToTree(Array.isArray(data) ? data : [], 'deptId', 'parentId'))
export const addDept = (data) => request.post(`${PREFIX}/dept`, data)
export const updateDept = (data) => request.put(`${PREFIX}/dept`, data)
export const deleteDept = (deptId) => request.delete(`${PREFIX}/dept/${deptId}`)

/* ============================== 字典 ============================== */
export const getDictTypePage = (params) =>
  request.get(`${PREFIX}/dict/type/list`, { params }).then((data) => normalizePage(data, params))
export const addDictType = (data) => request.post(`${PREFIX}/dict/type`, data)
export const updateDictType = (data) => request.put(`${PREFIX}/dict/type`, data)
export const deleteDictType = (dictIds) => request.delete(`${PREFIX}/dict/type/${dictIds}`)

export const addDictData = (data) => request.post(`${PREFIX}/dict/data`, data)
export const updateDictData = (data) => request.put(`${PREFIX}/dict/data`, data)
export const deleteDictData = (dictCodes) => request.delete(`${PREFIX}/dict/data/${dictCodes}`)

/**
 * 前台下拉专用：/dict/data/type/{dictType}
 * 返回 [{ label, value, cssClass, isDefault }]，这里补齐 dictLabel/dictValue 兼容旧页面组件
 */
export const getDictDataByType = (dictType) =>
  request.get(`${PREFIX}/dict/data/type/${dictType}`).then((list) =>
    (Array.isArray(list) ? list : []).map((item) => ({
      ...item,
      dictLabel: item.label ?? item.dictLabel,
      dictValue: item.value ?? item.dictValue,
      listClass: item.cssClass ?? item.listClass ?? 'default'
    }))
  )

/* ============================== 参数配置 ============================== */
export const getConfigPage = (params) =>
  request.get(`${PREFIX}/config/list`, { params }).then((data) => normalizePage(data, params))
export const addConfig = (data) => request.post(`${PREFIX}/config`, data)
export const updateConfig = (data) => request.put(`${PREFIX}/config`, data)
export const deleteConfig = (configIds) => request.delete(`${PREFIX}/config/${configIds}`)

/* ============================== 日志 ============================== */
export const getOperLogPage = (params) =>
  request.get(`${PREFIX}/operlog/list`, { params }).then((data) => normalizePage(data, params))
export const deleteOperLogs = (operIds) => request.delete(`${PREFIX}/operlog/${operIds}`)
export const cleanOperLog = () => request.delete(`${PREFIX}/operlog/clean`)

export const getLoginLogPage = (params) =>
  request.get(`${PREFIX}/loginlog/list`, { params }).then((data) => normalizePage(data, params))
export const deleteLoginLogs = (infoIds) => request.delete(`${PREFIX}/loginlog/${infoIds}`)
export const cleanLoginLog = () => request.delete(`${PREFIX}/loginlog/clean`)

/* ============================== 文件 ============================== */
export const uploadFile = (formData, onProgress) =>
  request.post(`${PREFIX}/file/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })

export const getFilePage = (params) =>
  request.get(`${PREFIX}/file/list`, { params }).then((data) => normalizePage(data, params))
export const deleteFiles = (fileIds) => request.delete(`${PREFIX}/file/${fileIds}`)
