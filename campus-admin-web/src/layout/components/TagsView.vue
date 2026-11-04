<template>
  <div class="tags-view">
    <el-scrollbar>
      <div class="tags-wrap">
        <span
          v-for="tag in appStore.visitedViews"
          :key="tag.path"
          class="tag-item"
          :class="{ active: tag.path === route.path }"
          @click="goTag(tag)"
        >
          {{ tag.title }}
          <el-icon
            v-if="tag.path !== '/dashboard'"
            class="tag-close"
            @click.stop="closeTag(tag)"
          >
            <Close />
          </el-icon>
        </span>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

// 路由变化时维护标签页；刷新后首屏也要补上当前页
watch(
  () => route.path,
  () => {
    if (route.meta?.title && !route.meta?.hidden) {
      appStore.addView(route)
    }
  },
  { immediate: true }
)

function goTag(tag) {
  if (tag.path !== route.path) router.push(tag.path)
}

/** 关闭标签：若关闭的是当前页，跳到最后一个标签或首页 */
function closeTag(tag) {
  appStore.removeView(tag.path)
  if (tag.path === route.path) {
    const last = appStore.visitedViews[appStore.visitedViews.length - 1]
    router.push(last ? last.path : '/dashboard')
  }
}
</script>

<style scoped>
.tags-view {
  height: 38px;
  padding: 5px 12px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.tags-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 10px;
  font-size: 12px;
  color: #5a5e66;
  background: #f4f4f5;
  border: 1px solid #e4e7ed;
  border-radius: 3px;
  cursor: pointer;
}

.tag-item:hover {
  color: #409eff;
}

.tag-item.active {
  color: #fff;
  background: #409eff;
  border-color: #409eff;
}

.tag-close {
  font-size: 12px;
  border-radius: 50%;
}

.tag-close:hover {
  background: rgba(0, 0, 0, 0.15);
}
</style>
