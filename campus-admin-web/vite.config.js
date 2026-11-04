import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/**
 * CampusMart 后台管理前端 —— Vite 配置
 *
 * 说明：scripts/ 目录下的 dev.mjs / build.mjs 包装脚本会直接 import 本文件，
 * 再通过 Vite 的 Node API 启动（便于探测环境、注入降级策略）。
 * 在正常环境下也可以直接使用 `npx vite` / `npx vite build`。
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: [
      // '@' 指向 src 目录（必须使用绝对路径，相对路径会被 Vite 当成项目根目录下的文件）
      { find: '@', replacement: fileURLToPath(new URL('./src', import.meta.url)) }
    ]
  },
  server: {
    host: '127.0.0.1',
    port: 5180,
    strictPort: false,
    proxy: {
      // 后端网关地址（见 docs/01-design.md），仅开发环境代理
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // 上传文件（商品封面/头像/轮播图）由 campus-item 以静态资源 /upload/** 暴露，
      // 必须同样代理到网关，否则开发态下会命中前端自己的 SPA 回退，图片全部加载失败
      '/upload': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    chunkSizeWarningLimit: 2000,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          element: ['element-plus'],
          echarts: ['echarts']
        }
      }
    }
  }
})
