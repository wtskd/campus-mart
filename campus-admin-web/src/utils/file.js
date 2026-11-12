/**
 * 文件地址与大小格式化。
 *
 * 后端 `sys_file.file_url` 存的是相对路径（例如 `/upload/2026/11/12/xxx.pdf`），
 * 它是给「网关/nginx」用的静态资源前缀。如果直接把相对路径放进 <a href> 或复制给浏览器，
 * 请求会落到前端 dev server（5180）上，Vite 找不到就回退到 index.html，
 * 于是打开一个 404 页面——看起来像「刷新后文件没了」。
 * 所以这里统一拼成后端服务的绝对地址：
 * - 开发环境读 VITE_FILE_BASE（默认 http://localhost:8081，即网关）
 * - 生产环境留空，由 nginx 把 /upload 与前端同域代理
 */
const ENV_BASE = String(import.meta.env.VITE_FILE_BASE || '').replace(/\/+$/, '')

/**
 * 兜底：开发态即使没配 VITE_FILE_BASE（或 dev server 还没重启吃不到新的 .env），
 * 也默认指向本机网关 8081，而不是当前前端端口（否则 /upload/* 会被 SPA 回退成 404 页面）。
 */
const DEV_FALLBACK =
  import.meta.env.DEV && typeof window !== 'undefined'
    ? `${window.location.protocol}//${window.location.hostname}:8081`
    : ''

const FILE_BASE = ENV_BASE || DEV_FALLBACK

/** 相对路径 -> 可直接访问的绝对地址；已是绝对地址则原样返回 */
export function resolveFileUrl(url) {
  if (!url) return ''
  const value = String(url)
  if (/^(https?:)?\/\//i.test(value) || value.startsWith('blob:') || value.startsWith('data:')) {
    return value
  }
  return `${FILE_BASE}${value.startsWith('/') ? '' : '/'}${value}`
}

/** 字节数 -> 人类可读大小 */
export function fileSizeText(size) {
  if (size === null || size === undefined || size === '') return '-'
  const num = Number(size)
  if (!Number.isFinite(num)) return '-'
  if (num < 1024) return `${num} B`
  if (num < 1024 * 1024) return `${(num / 1024).toFixed(1)} KB`
  return `${(num / 1024 / 1024).toFixed(2)} MB`
}

/** 按扩展名给个图标/标签颜色 */
export const FILE_TYPE_TAG = {
  png: 'success',
  jpg: 'success',
  jpeg: 'success',
  gif: 'success',
  webp: 'success',
  pdf: 'danger',
  zip: 'warning'
}

export function isImageType(type) {
  return ['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(String(type || '').toLowerCase())
}
