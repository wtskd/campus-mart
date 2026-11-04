import { useUserStore } from '@/store/user'

/**
 * v-hasPerm 权限指令
 *
 * 用法：
 *   <el-button v-hasPerm="'system:user:add'">新增</el-button>
 *   <el-button v-hasPerm="['system:user:edit', 'system:user:add']">编辑</el-button>  // 满足任一即可
 *
 * 没有权限时直接把元素从 DOM 中移除（而不是 display:none，避免用户从控制台改样式绕过）。
 * 注意：这只是前端体验优化，真正的权限校验必须由后端 @RequiresPerm 完成。
 */
function checkPermission(el, binding) {
  const userStore = useUserStore()
  const value = binding.value
  const required = Array.isArray(value) ? value : [value].filter(Boolean)
  if (!required.length) return
  const pass = required.some((perm) => userStore.hasPerm(perm))
  if (!pass) {
    el.parentNode?.removeChild(el)
  }
}

export default {
  mounted: checkPermission,
  updated: checkPermission
}
