<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input v-model="query.code" placeholder="模板编码" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-input v-model="query.name" placeholder="模板名称" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-select v-model="query.scene" placeholder="场景" clearable style="width: 130px">
          <el-option v-for="(label, value) in SCENE_MAP" :key="value" :label="label" :value="value" />
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
        <span class="title">Prompt 模板（可在线调整 AI 提示词）</span>
        <el-button v-hasPerm="'ai:prompt:add'" type="primary" :icon="Plus" @click="openDialog()">新增模板</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="code" label="编码" width="160" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="场景" width="110">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ SCENE_MAP[row.scene] || row.scene }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="模型" width="140">
          <template #default="{ row }">{{ row.model || '默认模型' }}</template>
        </el-table-column>
        <el-table-column prop="temperature" label="温度" width="80" align="center" />
        <el-table-column prop="maxTokens" label="maxTokens" width="100" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="String(row.status) === '0' ? 'success' : 'info'" size="small">
              {{ String(row.status) === '0' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'ai:prompt:edit'" link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button v-hasPerm="'ai:prompt:edit'" link :type="String(row.status) === '0' ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ String(row.status) === '0' ? '停用' : '启用' }}
            </el-button>
            <el-button v-hasPerm="'ai:prompt:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑 Prompt 模板' : '新增 Prompt 模板'" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item prop="code" label="模板编码">
              <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 ITEM_COPYWRITE（唯一）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="name" label="模板名称">
              <el-input v-model="form.name" placeholder="如 商品文案生成" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item prop="scene" label="场景">
              <el-select v-model="form.scene" style="width: 100%">
                <el-option v-for="(label, value) in SCENE_MAP" :key="value" :label="label" :value="value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="温度">
              <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="maxTokens">
              <el-input-number v-model="form.maxTokens" :min="64" :max="8192" :step="64" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="指定模型">
          <el-input v-model="form.model" placeholder="留空使用 AI 配置的默认模型" />
        </el-form-item>
        <el-form-item prop="systemPrompt" label="System 提示词">
          <el-input v-model="form.systemPrompt" type="textarea" :rows="4" placeholder="角色设定与约束" />
        </el-form-item>
        <el-form-item prop="userPrompt" label="User 提示词">
          <el-input v-model="form.userPrompt" type="textarea" :rows="5" placeholder="支持 {{变量}} 占位，如 {{title}}、{{category}}" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { addPrompt, deletePrompt, getPromptPage, updatePrompt } from '@/api/ai'

const SCENE_MAP = {
  copywrite: '商品文案',
  chat: '智能客服',
  audit: '内容审核',
  price: '智能估价'
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, code: '', name: '', scene: '', status: '' })

async function loadList() {
  loading.value = true
  try {
    const page = await getPromptPage({ ...query })
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
  Object.assign(query, { current: 1, size: 10, code: '', name: '', scene: '', status: '' })
  loadList()
}

// ---------------- 新增 / 编辑 ----------------
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive(emptyForm())

function emptyForm() {
  return {
    id: '',
    code: '',
    name: '',
    scene: 'copywrite',
    systemPrompt: '',
    userPrompt: '',
    model: '',
    temperature: 0.7,
    maxTokens: 1024,
    status: '0',
    remark: ''
  }
}

const rules = {
  code: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  scene: [{ required: true, message: '请选择场景', trigger: 'change' }],
  systemPrompt: [{ required: true, message: '请输入 system 提示词', trigger: 'blur' }],
  userPrompt: [{ required: true, message: '请输入 user 提示词', trigger: 'blur' }]
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
      await updatePrompt(form)
      ElMessage.success('修改成功')
    } else {
      await addPrompt(form)
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
    await updatePrompt({ ...row, status: next })
    ElMessage.success(next === '0' ? '已启用' : '已停用')
    loadList()
  } catch (error) {
    // 拦截器已提示
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除模板「${row.name}」吗？删除后该场景将回退到代码内置默认提示词。`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      await deletePrompt(row.id)
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
