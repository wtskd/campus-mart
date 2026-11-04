<template>
  <div class="navbar">
    <div class="navbar-left">
      <el-icon class="hamburger" @click="appStore.toggleSidebar()">
        <Expand v-if="appStore.sidebarCollapsed" />
        <Fold v-else />
      </el-icon>

      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.fullPath">
          {{ item.name }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="navbar-right">
      <el-tooltip content="数据分析大屏" placement="bottom">
        <el-button link class="icon-btn" @click="goScreen">
          <el-icon><Monitor /></el-icon>
        </el-button>
      </el-tooltip>

      <el-dropdown trigger="click" @command="handleCommand">
        <span class="user-info">
          <el-avatar :size="30" :src="userStore.avatar">{{ nickInitial }}</el-avatar>
          <span class="user-name">{{ userStore.nickName }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人中心
            </el-dropdown-item>
            <el-dropdown-item command="screen">
              <el-icon><Monitor /></el-icon>数据大屏
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const nickInitial = computed(() => (userStore.nickName || 'A').charAt(0))

/** 递归查找当前路由在菜单树中的祖先链，用于面包屑 */
function findChain(nodes, path, parents = []) {
  for (const node of nodes) {
    const chain = [...parents, node]
    if (node.fullPath === path) return chain
    if (node.children && node.children.length) {
      const found = findChain(node.children, path, chain)
      if (found) return found
    }
  }
  return null
}

const breadcrumbs = computed(() => {
  const chain = findChain(userStore.sidebarMenus, route.path) || []
  // 首页本身不重复显示
  return chain.filter((item) => item.fullPath !== '/dashboard')
})

function goScreen() {
  router.push('/analysis/screen')
}

function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'screen') {
    goScreen()
    return
  }
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
      .then(() => userStore.logout())
      .catch(() => {})
  }
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px;
  padding: 0 16px;
  background: #fff;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hamburger {
  font-size: 20px;
  cursor: pointer;
  color: #5a5e66;
}

.hamburger:hover {
  color: #409eff;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon-btn {
  font-size: 18px;
  color: #5a5e66;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
}

.user-name {
  font-size: 14px;
  color: #333;
}
</style>
