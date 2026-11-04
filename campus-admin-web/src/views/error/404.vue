<template>
  <div class="not-found">
    <div class="code">404</div>
    <p class="desc">页面不存在，可能已被删除或没有访问权限</p>
    <el-button type="primary" @click="$router.replace('/dashboard')">返回首页</el-button>
  </div>
</template>

<script setup>
// 404 页面：由路由兜底 + 菜单里配置了不存在的组件时降级使用
</script>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  color: #303133;
}

.code {
  font-size: 96px;
  font-weight: 700;
  color: #409eff;
  line-height: 1;
}

.desc {
  margin: 16px 0 24px;
  color: #909399;
}
</style>
