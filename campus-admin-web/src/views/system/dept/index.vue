<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input v-model="filterName" placeholder="学院/部门名称" clearable style="width: 200px" />
        <el-button type="primary" :icon="Search" @click="loadTree">查询</el-button>
        <el-button :icon="Refresh" @click="filterName = ''">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">学院/部门列表</span>
        <el-button v-hasPerm="'system:dept:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增学院
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="displayTree"
        row-key="deptId"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="deptName" label="学院名称" min-width="220" />
        <el-table-column prop="orderNum" label="排序" width="80" align="center" />
        <el-table-column prop="leader" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'info' : 'success'" size="small">
              {{ row.status === 1 ? '停用' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'system:dept:edit'" link type="primary" @click="openDialog(row)">
              编辑
            </el-button>
            <el-button v-hasPerm="'system:dept:add'" link type="success" @click="openDialog(null, row)">
              新增下级
            </el-button>
            <el-button v-hasPerm="'system:dept:remove'" link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.deptId ? '编辑学院' : '新增学院'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级学院">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'deptName', value: 'deptId' }"
            check-strictly
            placeholder="选择上级（顶级选主部门）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item prop="deptName" label="学院名称">
          <el-input v-model="form.deptName" placeholder="如：计算机学院" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.orderNum" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" placeholder="选填" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { addDept, deleteDept, getDeptTree, updateDept } from '@/api/system'

const loading = ref(false)
const rawTree = ref([])
const filterName = ref('')

const displayTree = computed(() => {
  const name = filterName.value.trim()
  if (!name) return rawTree.value
  const filterNodes = (nodes) =>
    nodes
      .map((node) => {
        const children = node.children?.length ? filterNodes(node.children) : []
        if (node.deptName.includes(name) || children.length) return { ...node, children }
        return null
      })
      .filter(Boolean)
  return filterNodes(rawTree.value)
})

const parentOptions = computed(() => [{ deptId: '0', deptName: '主部门', children: rawTree.value }])

async function loadTree() {
  loading.value = true
  try {
    const tree = (await getDeptTree()) || []
    // 后端 status 是 String（'0' 正常 / '1' 停用），模板按数字比较，这里统一转数字
    const fix = (nodes) =>
      nodes.map((node) => ({
        ...node,
        status: Number(node.status ?? 0),
        children: Array.isArray(node.children) ? fix(node.children) : []
      }))
    rawTree.value = fix(tree)
  } catch (error) {
    rawTree.value = []
  } finally {
    loading.value = false
  }
}

// ---------------- 新增 / 编辑 ----------------
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive(emptyForm())

function emptyForm() {
  return {
    deptId: '',
    parentId: '0',
    deptName: '',
    orderNum: 1,
    leader: '',
    phone: '',
    status: 0
  }
}

const rules = {
  deptName: [{ required: true, message: '请输入学院名称', trigger: 'blur' }]
}

function openDialog(row, parent) {
  Object.assign(form, emptyForm())
  if (row) {
    Object.assign(form, row, { parentId: String(row.parentId) })
  } else if (parent) {
    form.parentId = String(parent.deptId)
  }
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (form.deptId) {
      await updateDept(form)
      ElMessage.success('修改成功')
    } else {
      await addDept(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadTree()
  } catch (error) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除「${row.deptName}」吗？存在下级部门时需先删除子级。`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      await deleteDept(row.deptId)
      ElMessage.success('删除成功')
      loadTree()
    })
    .catch(() => {})
}

onMounted(loadTree)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
}
</style>
