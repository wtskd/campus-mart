<template>
  <div class="sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
    <!-- 品牌区 -->
    <div class="brand">
      <el-icon class="brand-icon"><ShoppingBag /></el-icon>
      <span v-show="!appStore.sidebarCollapsed" class="brand-text">校园易站</span>
    </div>

    <el-scrollbar class="menu-scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        unique-opened
        router
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        <SidebarItem v-for="item in menus" :key="item.id" :item="item" />
      </el-menu>
    </el-scrollbar>

    <div class="sidebar-footer">
      <span v-show="!appStore.sidebarCollapsed">CampusMart Admin v1.0</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import SidebarItem from './SidebarItem.vue'

const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()

// 动态菜单（登录后由 /auth/menus 拉取并规范化 fullPath）
const menus = computed(() => userStore.sidebarMenus)
// 当前高亮项：优先用 meta.activeMenu（详情页可指定高亮到列表页）
const activeMenu = computed(() => route.meta?.activeMenu || route.path)
</script>

<style scoped>
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 1001;
  display: flex;
  flex-direction: column;
  width: 220px;
  background: #001529;
  transition: width 0.25s;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 64px;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 56px;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
  background: #002140;
  white-space: nowrap;
}

.brand-icon {
  font-size: 22px;
  color: #409eff;
}

.menu-scroll {
  flex: 1;
  overflow: hidden;
}

.sidebar-menu {
  border-right: none;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #409eff !important;
}

.sidebar-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #fff;
}

.sidebar-footer {
  height: 36px;
  line-height: 36px;
  text-align: center;
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
}
</style>
