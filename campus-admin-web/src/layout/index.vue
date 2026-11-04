<template>
  <div class="app-wrapper">
    <Sidebar class="sidebar-container" />
    <div class="main-container">
      <div class="app-header">
        <Navbar />
        <TagsView />
      </div>
      <AppMain />
    </div>
  </div>
</template>

<script setup>
import { useAppStore } from '@/store/app'
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import TagsView from './components/TagsView.vue'
import AppMain from './components/AppMain.vue'

// 侧边栏折叠状态由 Pinia 管理，布局宽度通过 CSS class 联动
const appStore = useAppStore()
</script>

<style scoped>
.app-wrapper {
  position: relative;
  min-height: 100vh;
  width: 100%;
  background: #f0f2f5;
}

.main-container {
  min-height: 100vh;
  margin-left: 220px;
  transition: margin-left 0.25s;
}

:global(.sidebar-collapsed) .main-container {
  margin-left: 64px;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}
</style>
