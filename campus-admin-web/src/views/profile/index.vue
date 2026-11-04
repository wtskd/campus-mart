<template>
  <div class="page-container">
    <el-row :gutter="14">
      <el-col :span="8">
        <el-card shadow="never" class="profile-card">
          <div class="avatar-box">
            <el-avatar :size="86" :src="userStore.avatar">{{ initial }}</el-avatar>
            <h3>{{ userStore.nickName }}</h3>
            <p class="username">@{{ userStore.userInfo?.username || '-' }}</p>
          </div>
          <el-divider />
          <div class="info-line"><span>用户 ID</span><b>{{ userStore.userInfo?.userId || '-' }}</b></div>
          <div class="info-line"><span>所属部门</span><b>{{ userStore.userInfo?.deptName || '-' }}</b></div>
          <div class="info-line"><span>手机号</span><b>{{ userStore.userInfo?.phonenumber || '-' }}</b></div>
          <div class="info-line"><span>最近登录</span><b>{{ lastLogin }}</b></div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="never">
          <template #header><span class="card-title">账号权限</span></template>
          <div class="perm-block">
            <h4>角色</h4>
            <div class="tag-row">
              <el-tag v-for="role in userStore.roles" :key="role" effect="plain">{{ role }}</el-tag>
              <el-empty v-if="!userStore.roles.length" description="暂无角色" :image-size="60" />
            </div>
          </div>
          <div class="perm-block">
            <h4>权限标识</h4>
            <div class="tag-row">
              <el-tag v-for="perm in userStore.perms" :key="perm" type="info" effect="plain">
                {{ perm }}
              </el-tag>
              <el-empty v-if="!userStore.perms.length" description="暂无权限数据" :image-size="60" />
            </div>
          </div>
          <el-alert
            class="notice"
            type="info"
            :closable="false"
            show-icon
            title="权限说明"
            description="页面路由与按钮由后端菜单/权限数据驱动：菜单决定你能看到哪些页面，权限标识（perms）决定按钮是否显示。"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store/user'
import { formatDateTime } from '@/utils/format'

const userStore = useUserStore()

const initial = computed(() => (userStore.nickName || 'A').charAt(0))
const lastLogin = computed(() => formatDateTime(userStore.userInfo?.lastLoginTime))
</script>

<style scoped>
.profile-card {
  text-align: center;
}

.avatar-box h3 {
  margin: 12px 0 2px;
  font-size: 18px;
}

.username {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.info-line {
  display: flex;
  justify-content: space-between;
  padding: 9px 4px;
  font-size: 13px;
  border-bottom: 1px dashed #f0f0f0;
}

.info-line span {
  color: #909399;
}

.card-title {
  font-weight: 600;
}

.perm-block {
  margin-bottom: 18px;
}

.perm-block h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #303133;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.notice {
  margin-top: 8px;
}
</style>
