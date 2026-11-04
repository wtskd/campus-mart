import { onMounted, reactive, ref } from 'vue'

/**
 * 列表页通用逻辑（分页 + 查询 + 加载态）
 *
 * @param {Function} fetcher 接口函数，接收 { current, size, ...查询条件 }，返回 { records, total }
 * @param {Object} initialState 查询条件初始值（会被展开到 query 中）
 * @param {Object} options { size: 默认每页条数, immediate: 是否挂载后立即加载 }
 *
 * 用法：
 * const { loading, list, total, query, getList, handleSearch, resetQuery } = useTable(getUserPage, { username: '' })
 */
/**
 * 后端部分字段是 String（如 sys_user.status='0'），而 el-switch/比较用数字 0/1。
 * 统一把「纯数字字符串」的常用枚举字段转成数字，避免开关在渲染时被判定为「不匹配」而误触发 change 事件
 * （表现为进入用户/角色列表就弹出一串「已停用」提示）。
 */
const NUMERIC_FIELDS = ['status', 'delFlag', 'sex', 'gender', 'isRead', 'msgType', 'viewCount', 'creditScore']

function normalizeRow(row) {
  if (!row || typeof row !== 'object') return row
  NUMERIC_FIELDS.forEach((field) => {
    const value = row[field]
    if (typeof value === 'string' && /^-?\d+$/.test(value)) row[field] = Number(value)
  })
  return row
}

export function useTable(fetcher, initialState = {}, options = {}) {
  const { size = 10, immediate = true } = options

  const loading = ref(false)
  const list = ref([])
  const total = ref(0)

  const query = reactive({
    current: 1,
    size,
    ...initialState
  })

  async function getList() {
    loading.value = true
    try {
      const params = { ...query }
      const data = await fetcher(params)
      // 兼容后端两种返回：分页对象 { records, total } 或直接数组
      if (Array.isArray(data)) {
        list.value = data.map(normalizeRow)
        total.value = data.length
      } else {
        list.value = (data?.records || []).map(normalizeRow)
        total.value = Number(data?.total || 0)
      }
    } catch (error) {
      list.value = []
      total.value = 0
      console.error('[useTable] 查询失败：', error)
    } finally {
      loading.value = false
    }
  }

  /** 点查询按钮：回到第一页 */
  function handleSearch() {
    query.current = 1
    getList()
  }

  /** 重置查询条件（保留 current/size 之外的初始值） */
  function resetQuery() {
    Object.keys(query).forEach((key) => {
      if (key !== 'current' && key !== 'size') delete query[key]
    })
    Object.assign(query, { current: 1, size }, initialState)
    getList()
  }

  /** 分页变化（Pagination 组件抛出的 { page, limit }） */
  function handlePagination({ page, limit }) {
    query.current = page
    query.size = limit
    getList()
  }

  if (immediate) onMounted(getList)

  return { loading, list, total, query, getList, handleSearch, resetQuery, handlePagination }
}

export default useTable
