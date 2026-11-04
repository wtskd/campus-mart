<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input
          v-model="query.title"
          placeholder="公告标题"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">公告列表</span>
        <el-button v-hasPerm="'system:notice:add'" type="primary" :icon="Plus" @click="openDialog()">
          发布公告
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="noticeId" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="公告标题" min-width="240" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.noticeType)" size="small">{{ typeText(row.noticeType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createBy" label="发布人" width="100" />
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'info' : 'success'" size="small">
              {{ row.status === 1 ? '已下架' : '展示中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openView(row)">查看</el-button>
            <el-button v-hasPerm="'system:notice:edit'" link type="warning" @click="openDialog(row)">
              编辑
            </el-button>
            <el-button v-hasPerm="'system:notice:edit'" link type="success" @click="toggleStatus(row)">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-button>
            <el-button v-hasPerm="'system:notice:remove'" link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        v-model:page="query.current"
        v-model:limit="query.size"
        :total="total"
        @pagination="handlePagination"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.noticeId ? '编辑公告' : '发布公告'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item prop="title" label="标题">
          <el-input v-model="form.title" placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.noticeType">
            <el-radio value="1">公告</el-radio>
            <el-radio value="2">提示</el-radio>
            <el-radio value="3">规则</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否展示">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">展示</el-radio>
            <el-radio :value="1">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item prop="content" label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="公告正文" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewVisible" :title="viewRow.title" width="600px">
      <p class="view-meta">发布人：{{ viewRow.createBy }} ｜ 发布时间：{{ viewRow.createTime }}</p>
      <p class="view-content">{{ viewRow.content }}</p>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { useTable } from '@/composables/useTable'
import { addNotice, deleteNotice, getNoticePage, updateNotice } from '@/api/item'

const { loading, list, total, query, getList, handleSearch, resetQuery, handlePagination } = useTable(
  getNoticePage,
  { title: '', noticeType: '' }
)

function typeText(type) {
  return { 1: '公告', 2: '提示', 3: '规则' }[type] || type
}

function typeTag(type) {
  return { 1: 'primary', 2: 'warning', 3: 'danger' }[type] || 'info'
}

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive(emptyForm())

function emptyForm() {
  return { noticeId: '', title: '', noticeType: '1', content: '', status: 0 }
}

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

function openDialog(row) {
  Object.assign(form, emptyForm(), row || {})
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (form.noticeId) {
      await updateNotice(form)
      ElMessage.success('修改成功')
    } else {
      await addNotice(form)
      ElMessage.success('发布成功')
    }
    dialogVisible.value = false
    getList()
  } catch (error) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  try {
    await updateNotice({ ...row, status: row.status === 1 ? 0 : 1 })
    ElMessage.success(row.status === 1 ? '已上架' : '已下架')
    getList()
  } catch (error) {
    // 拦截器已提示
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteNotice(row.noticeId)
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

const viewVisible = ref(false)
const viewRow = ref({})

function openView(row) {
  viewRow.value = row
  viewVisible.value = true
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
}

.view-meta {
  color: #909399;
  font-size: 13px;
}

.view-content {
  line-height: 1.9;
  white-space: pre-wrap;
}
</style>
