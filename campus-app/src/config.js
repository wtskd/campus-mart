/**
 * 全局配置（契约 v1.1）
 * BaseURL 约定：
 *  - H5：/api（vite devServer proxy 转发到 http://localhost:8081，避免跨域）
 *  - 小程序/App：直接访问网关完整地址；真机调试把 LAN_IP 换成电脑局域网 IP
 */
const LAN_IP = '192.168.1.100'

const H5_BASE = '/api'
const APP_BASE = 'http://127.0.0.1:8081/api'
const LAN_BASE = `http://${LAN_IP}:8081/api`

let baseUrl = APP_BASE
// #ifdef H5
baseUrl = H5_BASE
// #endif

export default {
  // 当前平台使用的 BaseURL
  baseUrl,
  // 小程序真机调试时替换成这个（局域网 IP）
  lanBaseUrl: LAN_BASE,
  // H5 开发代理目标（与 vite.config.js 保持一致）
  proxyTarget: 'http://localhost:8081',
  // 普通接口超时
  timeout: 15000,
  // AI 接口超时（大模型推理较慢）
  aiTimeout: 40000,
  // 演示账号（登录页一键填充）
  demoAccounts: [
    { username: 'student01', password: 'admin123' },
    { username: 'student02', password: 'admin123' },
    { username: 'student03', password: 'admin123' }
  ],
  // 与数据库字典 sys_dict_data 对齐的前台枚举
  itemStatus: {
    0: { text: '草稿', color: '#9aa1ab' },
    1: { text: '待审核', color: '#ff9f0a' },
    2: { text: '在售', color: '#07c160' },
    3: { text: '已预订', color: '#2b7fff' },
    4: { text: '已售出', color: '#9aa1ab' },
    5: { text: '已下架', color: '#9aa1ab' },
    6: { text: '违规', color: '#ee0a24' }
  },
  orderStatus: {
    0: { text: '待确认', color: '#ff9f0a' },
    1: { text: '待交付', color: '#2b7fff' },
    2: { text: '已完成', color: '#07c160' },
    3: { text: '已取消', color: '#9aa1ab' },
    4: { text: '退款中', color: '#ff7d00' },
    5: { text: '已退款', color: '#9aa1ab' }
  },
  msgType: {
    1: { text: '系统通知', icon: '📢', color: '#2b7fff' },
    2: { text: '订单消息', icon: '🧾', color: '#ff9f0a' },
    3: { text: '审核结果', icon: '🔍', color: '#07c160' },
    4: { text: 'AI 消息', icon: '🤖', color: '#7d5bff' }
  },
  tradeType: {
    1: '出售',
    2: '求购',
    3: '交换'
  }
}
