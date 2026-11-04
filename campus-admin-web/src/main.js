import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
// 注册全局路由守卫（副作用导入，必须在 createApp 之前完成）
import './router/guard'
import hasPerm from './directives/hasPerm'
import '@/styles/index.css'

const app = createApp(App)

// 全局注册 Element Plus 图标，模板里可直接写 <el-icon><Setting /></el-icon>
Object.entries(ElementPlusIconsVue).forEach(([name, component]) => {
  app.component(name, component)
})

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
// 权限指令：无权限的按钮直接从 DOM 移除
app.directive('hasPerm', hasPerm)

app.mount('#app')
