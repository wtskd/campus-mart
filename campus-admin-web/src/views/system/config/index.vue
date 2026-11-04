<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input
          v-model="query.configName"
          placeholder="参数名称"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.configKey"
          placeholder="参数键名"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">参数列表</span>
        <el-button v-hasPerm="'system:config:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增参数
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="configId" label="ID" width="70" align="center" />
        <el-table-column prop="configName" label="参数名称" min-width="150" />
        <el-table-column prop="configKey" label="参数键名" min-width="200" />
        <el-table-column prop="configValue" label="参数键值" min-width="160" show-overflow-tooltip />
        <el-table-column label="系统内置" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.configType === 'Y' ? 'warning' : 'info'" size="small">
              {{ row.configType === 'Y' ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'system:config:edit'" link type="primary" @click="openDialog(row)">
              编辑
            </el-button>
            <el-button
              v-hasPerm="'system:config:remove'"
              link
              type="danger"
              :disabled="row.configType === 'Y'"
              @click="handleDelete(row)"
            >
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

    <el-dialog v-model="dialogVisible" :title="form.configId ? '编辑参数' : '新增参数'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item prop="configName" label="参数名称">
          <el-input v-model="form.configName" placeholder="如：AI 提供方" />
        </el-form-item>
        <el-form-item prop="configKey" label="参数键名">
          <el-input v-model="form.configKey" placeholder="如：ai.provider" :disabled="form.configId && form.configType === 'Y'" />
        </el-form-item>
        <el-form-item prop="configValue" label="参数键值">
          <el-input v-model="form.configValue" placeholder="如：local" />
        </el-form-item>
        <el-form-item label="系统内置">
          <el-radio-group v-model="form.configType">
            <el-radio value="Y">是</el-radio>
            <el-radio value="N">否</el-radio>
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
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { useTable } from '@/composables/useTable'
import { addConfig, deleteConfig, getConfigPage, updateConfig } from '@/api/system'

const { loading, list, total, query, getList, handleSearch, resetQuery, handlePagination } = useTable(
  getConfigPage,
  { configName: '', configKey: '' }
)

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive(emptyForm())

function emptyForm() {
  return { configId: '', configName: '', configKey: '', configValue: '', configType: 'N', remark: '' }
}

const rules = {
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  configKey: [{ required: true, message: '请输入参数键名', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入参数键值', trigger: 'blur' }]
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
    if (form.configId) {
      await updateConfig(form)
      ElMessage.success('修改成功')
    } else {
      await addConfig(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    getList()
  } catch (error) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除参数「${row.configName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteConfig(row.configId)
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
}
</style>
