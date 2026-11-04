/**
 * 文件上传（契约 v1.1 §3.8）：POST /system/file/upload，multipart 字段名 file
 */
import { uploadFile } from '@/utils/request'

/** 上传图片 → { fileName, url, size } */
export function uploadImage(filePath) {
  return uploadFile(filePath)
}
