import { reactive } from 'vue'
import { getDictDataByType } from '@/api/system'

/**
 * 字典数据（下拉框/标签）统一缓存
 *
 * 为什么需要缓存：字典数据几乎每个页面都要用（商品状态、订单状态等），
 * 每个页面都发一次请求既慢又浪费；这里用 Map 缓存 Promise，避免并发重复请求。
 */
const cache = new Map()

export function loadDict(dictType) {
  if (!dictType) return Promise.resolve([])
  if (cache.has(dictType)) return cache.get(dictType)
  const promise = getDictDataByType(dictType)
    .then((list) => list || [])
    .catch(() => []) // 字典接口失败不阻塞页面，降级为空数组
  cache.set(dictType, promise)
  return promise
}

export function getCachedDict(dictType) {
  return cache.get(dictType)
}

export function clearDictCache() {
  cache.clear()
}

/**
 * 页面内使用：
 * const dict = useDict('item_status', 'order_status')
 * <DictTag dict-type="item_status" :value="row.status" />
 */
export function useDict(...types) {
  const result = reactive(Object.fromEntries(types.map((t) => [t, []])))
  types.forEach((type) => {
    loadDict(type).then((list) => {
      result[type] = list
    })
  })
  return result
}
