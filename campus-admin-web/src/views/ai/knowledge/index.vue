<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input v-model="query.keyword" placeholder="标题 / 内容关键词（ngram 全文检索）" clearable style="width: 260px" @keyup.enter="handleSearch" />
        <el-select v-model="query.kbType" placeholder="知识类型" clearable style="width: 140px">
          <el-option v-for="(label, value) in KB_TYPE_MAP" :key="value" :label="label" :value="value" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">知识库（RAG 检索来源，MySQL ngram 全文索引）</span>
        <el-button v-hasPerm="'ai:knowledge:add'" type="primary" :icon="Plus" @click="openDialog()">新增知识</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ KB_TYPE_MAP[row.kbType] || row.kbType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
        <el-table-column prop="keywords" label="关键词" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="String(row.status) === '0' ? 'success' : 'info'" size="small">
              {{ String(row.status) === '0' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'ai:knowledge:edit'" link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button v-hasPerm="'ai:knowledge:edit'" link :type="String(row.status) === '0' ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ String(row.status) === '0' ? '停用' : '启用' }}
            </el-button>
            <el-button v-hasPerm="'ai:knowledge:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        v-model:page="query.current"
        v-model:limit="query.size"
        :total="total"
        @pagination="loadList"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑知识条目' : '新增知识条目'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item prop="title" label="标题">
          <el-input v-model="form.title" placeholder="如 如何发布商品" />
        </el-form-item>
        <el-form-item prop="kbType" label="类型">
          <el-select v-model="form.kbType" style="width: 100%">
            <el-option v-for="(label, value) in KB_TYPE_MAP" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item prop="content" label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="AI 客服将基于该内容回答用户问题" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keywords" placeholder="逗号分隔，辅助检索与排序，如 发布,上架,出售" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { addKnowledge, deleteKnowledge, getKnowledgePage, updateKnowledge } from '@/api/ai'

const KB_TYPE_MAP = {
  faq: '常见问题',
  rule: '平台规则',
  category: '分类说明'
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '', kbType: '', status: '' })

async function loadList() {
  loading.value = true
  try {
    const page = await getKnowledgePage({ ...query })
    list.value = page.records || []
    total.value = page.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.current = 1
  loadList()
}

function handleReset() {
  Object.assign(query, { current: 1, size: 10, keyword: '', kbType: '', status: '' })
  loadList()
}

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive(emptyForm())

function emptyForm() {
  return { id: '', kbType: 'faq', title: '', content: '', keywords: '', status: '0' }
}

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  kbType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

function openDialog(row) {
  Object.assign(form, emptyForm())
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (form.id) {
      await updateKnowledge(form)
      ElMessage.success('修改成功')
    } else {
      await addKnowledge(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } catch (error) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const next = String(row.status) === '0' ? '1' : '0'
  try {
    await updateKnowledge({ ...row, status: next })
    ElMessage.success(next === '0' ? '已启用' : '已停用')
    loadList()
  } catch (error) {
    // 拦截器已提示
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除知识「${row.title}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteKnowledge(row.id)
      ElMessage.success('删除成功')
      loadList()
    })
    .catch(() => {})
}

onMounted(loadList)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
}

.title {
  font-size: 15px;
  font-weight: 600;
}
</style>
