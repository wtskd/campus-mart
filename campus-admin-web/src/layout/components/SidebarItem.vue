<template>
  <!-- 没有子菜单：普通菜单项（el-menu 开了 router 模式，index 即跳转地址） -->
  <el-menu-item v-if="!item.children || item.children.length === 0" :index="item.fullPath">
    <el-icon v-if="item.icon"><component :is="iconOf(item.icon)" /></el-icon>
    <template #title>{{ item.name }}</template>
  </el-menu-item>

  <!-- 有子菜单：递归渲染 -->
  <el-sub-menu v-else :index="item.fullPath">
    <template #title>
      <el-icon v-if="item.icon"><component :is="iconOf(item.icon)" /></el-icon>
      <span>{{ item.name }}</span>
    </template>
    <SidebarItem v-for="child in item.children" :key="child.id" :item="child" />
  </el-sub-menu>
</template>

<script setup>
import { resolveIcon } from '@/utils/icons'

defineProps({
  item: { type: Object, required: true }
})

const iconOf = (name) => resolveIcon(name) || resolveIcon('menu')
</script>
