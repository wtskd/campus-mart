<template>
  <!-- 图片上传：对接 /system/file/upload，返回可访问 url -->
  <div class="image-upload">
    <el-upload
      class="uploader"
      :show-file-list="false"
      :http-request="handleUpload"
      :before-upload="beforeUpload"
      :accept="accept"
      :disabled="disabled"
    >
      <img v-if="modelValue" :src="modelValue" class="uploaded-image" alt="预览图" />
      <div v-else class="upload-placeholder">
        <el-icon><Plus /></el-icon>
        <span>点击上传</span>
      </div>
    </el-upload>
    <div v-if="modelValue && !disabled" class="upload-actions">
      <el-link type="primary" :underline="false" @click="emit('update:modelValue', '')">移除</el-link>
    </div>
    <div class="upload-tip">支持 jpg/png/webp，单张不超过 5MB</div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { uploadFile } from '@/api/system'

const props = defineProps({
  modelValue: { type: String, default: '' },
  accept: { type: String, default: 'image/*' },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

function beforeUpload(file) {
  const isImage = /^image\/(jpeg|png|png|webp|gif)$/.test(file.type)
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

async function handleUpload(options) {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const data = await uploadFile(formData, (event) => {
      if (event.total) options.onProgress?.({ percent: (event.loaded / event.total) * 100 })
    })
    const url = data?.url || ''
    if (!url) throw new Error('上传响应缺少 url')
    emit('update:modelValue', url)
    ElMessage.success('上传成功')
    options.onSuccess?.(data)
  } catch (error) {
    // 拦截器已在可控范围内提示，这里只处理进度条
    options.onError?.(error)
  }
}
</script>

<style scoped>
.image-upload {
  display: inline-block;
}

.uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s;
}

.uploader :deep(.el-upload:hover) {
  border-color: #409eff;
}

.uploaded-image {
  display: block;
  width: 148px;
  height: 148px;
  object-fit: cover;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 148px;
  height: 148px;
  color: #8c939d;
  font-size: 13px;
}

.upload-placeholder .el-icon {
  font-size: 26px;
}

.upload-actions {
  margin-top: 4px;
  text-align: center;
}

.upload-tip {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}
</style>
