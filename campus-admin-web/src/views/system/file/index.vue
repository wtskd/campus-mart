<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="toolbar">
        <div>
          <span class="title">文件上传</span>
          <span class="tip">支持 jpg / jpeg / png / gif / webp / pdf / zip，单个文件不超过 10MB</span>
        </div>
      </div>
      <el-upload
        drag
        multiple
        :show-file-list="false"
        :http-request="handleUpload"
        :before-upload="beforeUpload"
        accept=".jpg,.jpeg,.png,.gif,.webp,.pdf,.zip"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">图片可直接用于轮播图 / 商品封面；上传成功后会自动出现在下方文件列表。</div>
        </template>
      </el-upload>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <div class="search-bar">
        <el-input v-model="query.originalName" placeholder="文件名" clearable style="width: 220px" @keyup.enter="handleSearch" />
        <el-select v-model="query.fileType" placeholder="文件类型" clearable style="width: 140px">
          <el-option v-for="item in typeOptions" :key="item" :label="item.toUpperCase()" :value="item" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <div class="toolbar">
        <span class="title">文件列表</span>
        <span class="tip">共 {{ total }} 个文件（来自 sys_file，刷新页面也不会丢）</span>
        <el-button size="small" :icon="Refresh" @click="getList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column label="文件名" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="file-name">
              <el-image
                v-if="isImageType(row.fileType)"
                :src="resolveFileUrl(row.fileUrl)"
                :preview-src-list="[resolveFileUrl(row.fileUrl)]"
                preview-teleported
                fit="cover"
                class="file-thumb"
              />
              <el-icon v-else class="file-icon"><Document /></el-icon>
              <span>{{ row.originalName || row.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="FILE_TYPE_TAG[row.fileType] || 'info'" size="small">
              {{ (row.fileType || '-').toUpperCase() }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="110">
          <template #default="{ row }">{{ fileSizeText(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="上传人" width="130">
          <template #default="{ row }">{{ uploaderName(row.createBy) }}</template>
        </el-table-column>
        <el-table-column label="上传时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="地址" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :href="resolveFileUrl(row.fileUrl)" target="_blank">
              {{ resolveFileUrl(row.fileUrl) }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="copyUrl(resolveFileUrl(row.fileUrl))">复制链接</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无文件，拖拽文件到上方区域试试" />
        </template>
      </el-table>

      <Pagination :total="total" :page="query.current" :limit="query.size" @pagination="handlePagination" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Refresh, Search, UploadFilled } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { useTable } from '@/composables/useTable'
import { deleteFiles, getFilePage, getUserPage, uploadFile } from '@/api/system'
import { useUserStore } from '@/store/user'
import { formatDateTime } from '@/utils/format'
import { FILE_TYPE_TAG, fileSizeText, isImageType, resolveFileUrl } from '@/utils/file'

const userStore = useUserStore()

/** 与后端白名单保持一致 */
const typeOptions = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'pdf', 'zip']

const { loading, list, total, query, getList, handleSearch, resetQuery, handlePagination } = useTable(
  getFilePage,
  { originalName: '', fileType: '' },
  { immediate: false }
)

/** createBy 只是用户 id，能查用户列表时翻译成昵称 */
const userNameMap = ref({})
function uploaderName(userId) {
  if (!userId) return '-'
  // 后端把 Long 序列化成字符串（createBy: "1"），这里统一按字符串取
  return userNameMap.value[String(userId)] || `用户 #${userId}`
}

async function loadUserNames() {
  if (!userStore.hasPerm('system:user:list')) return
  try {
    const data = await getUserPage({ current: 1, size: 200 })
    const map = {}
    ;(data?.records || []).forEach((user) => {
      map[String(user.userId)] = user.nickname || user.username
    })
    userNameMap.value = map
  } catch (error) {
    // 没有权限或接口异常时退化为「用户 #id」，不影响文件列表
  }
}

function beforeUpload(file) {
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('单个文件不能超过 10MB')
    return false
  }
  return true
}

async function handleUpload(options) {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const data = await uploadFile(formData)
    ElMessage.success(`上传成功：${data?.url || ''}`)
    options.onSuccess?.(data)
    // 上传后回到第一页看最新文件（列表按 file_id 倒序）
    query.current = 1
    await getList()
  } catch (error) {
    options.onError?.(error)
  }
}

async function copyUrl(url) {
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('链接已复制')
  } catch (error) {
    ElMessage.warning('复制失败，请手动复制')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.originalName || row.fileName}」吗？磁盘文件会一并删除，引用了该地址的图片会失效。`,
      '删除文件',
      { type: 'warning' }
    )
  } catch (error) {
    return
  }
  await deleteFiles(row.fileId)
  ElMessage.success('删除成功')
  await getList()
}

function handleReset() {
  query.originalName = ''
  query.fileType = ''
  resetQuery()
}

onMounted(async () => {
  await getList()
  loadUserNames()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
}

.title {
  font-weight: 600;
}

.tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.upload-icon {
  font-size: 48px;
  color: #c0c4cc;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-thumb {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  flex-shrink: 0;
}

.file-icon {
  font-size: 18px;
  color: #909399;
  flex-shrink: 0;
}
</style>
