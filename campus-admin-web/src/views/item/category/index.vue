<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="分类名称" clearable style="width: 220px" />
        <el-button type="primary" :icon="Search" @click="loadList">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        <el-button v-hasPerm="'item:category:add'" type="success" :icon="Plus" @click="openDialog()">新增分类</el-button>
        <el-button :icon="Sort" @click="toggleExpand">展开/折叠</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        row-key="categoryId"
        border
        :default-expand-all="expandAll"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="categoryName" label="分类名称" min-width="200" />
        <el-table-column label="图标" width="90" align="center">
          <template #default="{ row }">
            <el-image v-if="row.icon" :src="row.icon" fit="contain" style="width: 24px; height: 24px" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderNum" label="排序" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="0"
              inactive-value="1"
              :disabled="!hasPerm('item:category:edit')"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'item:category:edit'" link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button v-hasPerm="'item:category:add'" link type="success" @click="openDialog(null, row)">新增子类</el-button>
            <el-button v-hasPerm="'item:category:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.categoryId ? '编辑分类' : '新增分类'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级分类" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'categoryName', value: 'categoryId', children: 'children' }"
            value-key="categoryId"
            check-strictly
            placeholder="不选则为一级分类"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="如：教材书籍" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="图标 URL">
          <el-input v-model="form.icon" placeholder="选填，分类小图标地址" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.orderNum" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, Sort } from '@element-plus/icons-vue'
import { addCategory, deleteCategory, getCategoryList, updateCategory, updateCategoryStatus } from '@/api/item'
import { useUserStore } from '@/store/user'

const loading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const list = ref([])
const expandAll = ref(true)
const permissions = []
const userStore = useUserStore()

function hasPerm(perm) {
  return userStore.hasPerm(perm)
}

/** 扁平列表 → 树（分类接口 /category/list 返回平铺数据） */
const tableData = computed(() => buildTree(list.value))

function buildTree(rows) {
  const map = new Map()
  const roots = []
  const filtered = keyword.value
    ? rows.filter((row) => row.categoryName && row.categoryName.includes(keyword.value))
    : rows
  filtered.forEach((row) => map.set(row.categoryId, { ...row, children: [] }))
  filtered.forEach((row) => {
    const node = map.get(row.categoryId)
    if (row.parentId && map.has(row.parentId)) {
      map.get(row.parentId).children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

/** 上级分类选项：树结构，新增时排除自身 */
const parentOptions = computed(() => [{ categoryId: 0, categoryName: '顶级分类', children: buildTree(list.value) }])

async function loadList() {
  loading.value = true
  try {
    list.value = (await getCategoryList()) || []
  } catch (error) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleReset() {
  keyword.value = ''
  loadList()
}

function toggleExpand() {
  expandAll.value = !expandAll.value
}

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive(emptyForm())

function emptyForm() {
  return { categoryId: '', parentId: 0, categoryName: '', icon: '', orderNum: 0, status: '0' }
}

const rules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

function openDialog(row, parent) {
  Object.assign(form, emptyForm())
  if (row) {
    Object.assign(form, row)
  }
  if (parent) {
    form.parentId = parent.categoryId
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.categoryId) {
      await updateCategory({ ...form })
    } else {
      await addCategory({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } catch (error) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

async function handleStatusChange(row) {
  const next = row.status
  try {
    await updateCategoryStatus({ categoryId: row.categoryId, status: next })
    ElMessage.success(next === '0' ? '已启用' : '已停用')
  } catch (error) {
    row.status = next === '0' ? '1' : '0'
  }
}

function handleDelete(row) {
  if (row.children && row.children.length) {
    ElMessage.warning('请先删除该分类下的子分类')
    return
  }
  ElMessageBox.confirm(`确认删除分类「${row.categoryName}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteCategory(row.categoryId)
      ElMessage.success('删除成功')
      loadList()
    })
    .catch(() => {})
}

onMounted(loadList)
</script>
