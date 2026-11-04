<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="toolbar">
        <div>
          <span class="title">文件上传</span>
          <span class="tip">对接 POST /system/file/upload（multipart，字段名 file），返回 { url, fileName, size }</span>
        </div>
      </div>
      <el-upload
        drag
        multiple
        :show-file-list="false"
        :http-request="handleUpload"
        :before-upload="beforeUpload"
        accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.zip"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">单个文件不超过 10MB；图片会直接用于轮播图 / 商品封面。</div>
        </template>
      </el-upload>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <div class="toolbar">
        <span class="title">本次上传记录</span>
        <span class="tip">契约未提供文件列表分页接口，此处仅展示当前会话内上传结果</span>
      </div>
      <el-table :data="fileList" border>
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sizeText" label="大小" width="110" />
        <el-table-column prop="uploader" label="上传人" width="120" />
        <el-table-column prop="uploadTime" label="上传时间" width="170" />
        <el-table-column label="地址" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :href="row.url" target="_blank">{{ row.url }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="copyUrl(row.url)">复制链接</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无上传记录，拖拽文件到上方区域试试" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadFile } from '@/api/system'
import { formatDateTime } from '@/utils/format'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const fileList = ref([])

function beforeUpload(file) {
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('单个文件不能超过 10MB')
    return false
  }
  return true
}

function sizeText(size) {
  if (!size && size !== 0) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

async function handleUpload(options) {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const data = await uploadFile(formData)
    const record = {
      fileName: data?.fileName || options.file.name,
      url: data?.url || '',
      sizeText: sizeText(data?.size ?? options.file.size),
      uploader: userStore.nickName || userStore.username || '当前用户',
      uploadTime: formatDateTime(new Date())
    }
    fileList.value.unshift(record)
    ElMessage.success('上传成功')
    options.onSuccess?.(data)
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
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
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
</style>
