<template>
  <div class="page-container">
    <el-row :gutter="12">
      <!-- 左：字典类型 -->
      <el-col :span="10">
        <el-card shadow="never">
          <div class="toolbar">
            <span class="title">字典类型</span>
            <el-button v-hasPerm="'system:dict:add'" type="primary" size="small" :icon="Plus" @click="openTypeDialog()">
              新增类型
            </el-button>
          </div>
          <el-input v-model="typeQuery" placeholder="按类型名称 / 标识筛选" clearable :prefix-icon="Search" style="margin-bottom: 10px" />
          <el-table
            v-loading="typeLoading"
            :data="filteredTypes"
            highlight-current-row
            border
            height="520"
            @current-change="handleTypeSelect"
          >
            <el-table-column prop="dictName" label="名称" min-width="110" />
            <el-table-column prop="dictType" label="类型标识" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <el-button v-hasPerm="'system:dict:edit'" link type="primary" size="small" @click.stop="openTypeDialog(row)">
                  编辑
                </el-button>
                <el-button v-hasPerm="'system:dict:remove'" link type="danger" size="small" @click.stop="handleDeleteType(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右：字典数据 -->
      <el-col :span="14">
        <el-card shadow="never">
          <div class="toolbar">
            <span class="title">
              字典数据
              <el-tag v-if="currentType.dictType" size="small" effect="plain" class="current-type">
                {{ currentType.dictName }}（{{ currentType.dictType }}）
              </el-tag>
            </span>
            <el-button
              v-hasPerm="'system:dict:add'"
              type="primary"
              size="small"
              :icon="Plus"
              :disabled="!currentType.dictType"
              @click="openDataDialog()"
            >
              新增数据
            </el-button>
          </div>
          <el-table v-loading="dataLoading" :data="dataList" border height="520">
            <el-table-column prop="dictLabel" label="字典标签" min-width="110" />
            <el-table-column prop="dictValue" label="字典键值" min-width="100" />
            <el-table-column prop="dictSort" label="排序" width="70" align="center" />
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="140" align="center">
              <template #default="{ row }">
                <el-button v-hasPerm="'system:dict:edit'" link type="primary" size="small" @click="openDataDialog(row)">
                  编辑
                </el-button>
                <el-button v-hasPerm="'system:dict:remove'" link type="danger" size="small" @click="handleDeleteData(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeDialogVisible" :title="typeForm.dictId ? '编辑字典类型' : '新增字典类型'" width="480px" destroy-on-close>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="如：商品状态" />
        </el-form-item>
        <el-form-item label="类型标识" prop="dictType">
          <el-input v-model="typeForm.dictType" placeholder="如：item_status" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeSubmitting" @click="submitType">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据弹窗 -->
    <el-dialog v-model="dataDialogVisible" :title="dataForm.dictCode ? '编辑字典数据' : '新增字典数据'" width="480px" destroy-on-close>
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="90px">
        <el-form-item label="字典类型">
          <el-input :model-value="currentType.dictType" disabled />
        </el-form-item>
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="展示文案，如：在售" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="存储值，如：2" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dataForm.dictSort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dataSubmitting" @click="submitData">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  addDictData,
  addDictType,
  deleteDictData,
  deleteDictType,
  getDictDataByType,
  getDictTypePage,
  updateDictData,
  updateDictType
} from '@/api/system'
import { clearDictCache } from '@/utils/dict'

// ---------------- 字典类型 ----------------
const typeLoading = ref(false)
const typeList = ref([])
const typeQuery = ref('')
const currentType = ref({})

const filteredTypes = computed(() => {
  const kw = typeQuery.value.trim().toLowerCase()
  if (!kw) return typeList.value
  return typeList.value.filter(
    (row) =>
      String(row.dictName || '').toLowerCase().includes(kw) ||
      String(row.dictType || '').toLowerCase().includes(kw)
  )
})

async function loadTypes() {
  typeLoading.value = true
  try {
    const data = await getDictTypePage({ current: 1, size: 200 })
    typeList.value = data?.records || []
    if (!currentType.value.dictType && typeList.value.length) {
      handleTypeSelect(typeList.value[0])
    }
  } catch (error) {
    typeList.value = []
  } finally {
    typeLoading.value = false
  }
}

function handleTypeSelect(row) {
  if (!row) return
  currentType.value = row
  loadData(row.dictType)
}

const typeDialogVisible = ref(false)
const typeSubmitting = ref(false)
const typeFormRef = ref(null)
const typeForm = reactive(emptyTypeForm())

function emptyTypeForm() {
  return { dictId: '', dictName: '', dictType: '', status: 0, remark: '' }
}

const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入类型标识', trigger: 'blur' }]
}

function openTypeDialog(row) {
  Object.assign(typeForm, emptyTypeForm(), row || {})
  typeDialogVisible.value = true
}

async function submitType() {
  const valid = await typeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  typeSubmitting.value = true
  try {
    if (typeForm.dictId) {
      await updateDictType(typeForm)
      ElMessage.success('修改成功')
    } else {
      await addDictType(typeForm)
      ElMessage.success('新增成功')
    }
    typeDialogVisible.value = false
    clearDictCache()
    loadTypes()
  } catch (error) {
    // 拦截器已提示
  } finally {
    typeSubmitting.value = false
  }
}

function handleDeleteType(row) {
  ElMessageBox.confirm(`确定删除字典类型「${row.dictName}」吗？其下数据将一并删除。`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteDictType(row.dictId)
      ElMessage.success('删除成功')
      clearDictCache()
      if (currentType.value.dictId === row.dictId) currentType.value = {}
      loadTypes()
    })
    .catch(() => {})
}

// ---------------- 字典数据 ----------------
const dataLoading = ref(false)
const dataList = ref([])

async function loadData(dictType) {
  if (!dictType) return
  dataLoading.value = true
  try {
    dataList.value = (await getDictDataByType(dictType)) || []
  } catch (error) {
    dataList.value = []
  } finally {
    dataLoading.value = false
  }
}

const dataDialogVisible = ref(false)
const dataSubmitting = ref(false)
const dataFormRef = ref(null)
const dataForm = reactive(emptyDataForm())

function emptyDataForm() {
  return { dictCode: '', dictType: '', dictLabel: '', dictValue: '', dictSort: 1, remark: '' }
}

const dataRules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }]
}

function openDataDialog(row) {
  Object.assign(dataForm, emptyDataForm(), row || {})
  dataForm.dictType = currentType.value.dictType
  dataForm.dictSort = row?.dictSort || dataList.value.length + 1
  dataDialogVisible.value = true
}

async function submitData() {
  const valid = await dataFormRef.value?.validate().catch(() => false)
  if (!valid) return
  dataSubmitting.value = true
  try {
    if (dataForm.dictCode) {
      await updateDictData(dataForm)
      ElMessage.success('修改成功')
    } else {
      await addDictData(dataForm)
      ElMessage.success('新增成功')
    }
    dataDialogVisible.value = false
    clearDictCache()
    loadData(currentType.value.dictType)
  } catch (error) {
    // 拦截器已提示
  } finally {
    dataSubmitting.value = false
  }
}

function handleDeleteData(row) {
  ElMessageBox.confirm(`确定删除字典数据「${row.dictLabel}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteDictData(row.dictCode)
      ElMessage.success('删除成功')
      clearDictCache()
      loadData(currentType.value.dictType)
    })
    .catch(() => {})
}

onMounted(loadTypes)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
}

.current-type {
  margin-left: 6px;
}
</style>
