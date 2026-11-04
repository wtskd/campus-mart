import * as Icons from '@element-plus/icons-vue'

/**
 * 菜单图标解析
 *
 * 数据库 sys_menu.icon 里存的是简化名（seed 数据：setting / user / peoples / tree-table ...），
 * Element Plus 的图标是 PascalCase 组件名，这里做一层映射，避免页面里写死图标。
 */
const ICON_ALIAS = {
  setting: 'Setting',
  user: 'User',
  peoples: 'UserFilled',
  'tree-table': 'Grid',
  tree: 'Share',
  dict: 'Collection',
  edit: 'EditPen',
  message: 'Message',
  picture: 'Picture',
  upload: 'Upload',
  form: 'Tickets',
  logininfor: 'Key',
  shopping: 'ShoppingBag',
  checkbox: 'Select',
  list: 'List',
  star: 'Star',
  documentation: 'Document',
  bug: 'Warning',
  chart: 'DataLine',
  dashboard: 'Odometer',
  money: 'Money',
  example: 'TrendCharts',
  monitor: 'Monitor',
  log: 'Document',
  education: 'Reading',
  system: 'Setting'
}

/** 简化名 → Element Plus 图标组件 */
export function resolveIcon(name) {
  if (!name) return null
  if (Icons[name]) return Icons[name]
  const alias = ICON_ALIAS[name]
  if (alias && Icons[alias]) return Icons[alias]
  // 兜底：把 kebab-case 转成 PascalCase 再找一次
  const pascal = String(name)
    .split(/[-_]/)
    .filter(Boolean)
    .map((s) => s.charAt(0).toUpperCase() + s.slice(1))
    .join('')
  return Icons[pascal] || Icons.Menu
}
