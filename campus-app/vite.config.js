import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// Vite 配置：H5 开发态把 /api 代理到本地网关 8081，避免跨域
export default defineConfig({
  plugins: [uni()],
  server: {
    host: '0.0.0.0',
    // 5181：避开后台的 5180 和本机被占用的 5173，两个前端可以同时开
    port: 5181,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
        // 网关本身就带 /api 前缀，无需 rewrite
      },
      // 演示图片 / 上传文件由网关静态映射，H5 开发态一并代理
      '/upload': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
