<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input v-model="filterName" placeholder="菜单名称" clearable style="width: 180px" />
        <el-select v-model="filterType" placeholder="菜单类型" clearable style="width: 140px">
          <el-option label="目录" value="M" />
          <el-option label="菜单" value="C" />
          <el-option label="按钮" value="F" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="loadTree">查询</el-button>
        <el-button :icon="Refresh" @click="resetFilter">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">菜单列表</span>
        <div>
          <el-button v-hasPerm="'system:menu:add'" type="primary" :icon="Plus" @click="openDialog()">
            新增菜单
          </el-button>
          <el-button :icon="Sort" @click="toggleExpandAll">展开/折叠</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="displayTree"
        :key="renderKey"
        row-key="menuId"
        border
        :default-expand-all="expandAll"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="200" />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.menuType)" size="small">{{ typeText(row.menuType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNum" label="排序" width="80" align="center" />
        <el-table-column prop="path" label="路由地址" min-width="150" />
        <el-table-column prop="component" label="组件路径" min-width="190" />
        <el-table-column prop="perms" label="权限标识" min-width="170" />
        <el-table-column label="图标" width="80" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="resolveIcon(row.icon)" /></el-icon>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="显示" width="80" align="center">
          <template #default="{ row }">
            <!-- v1.1：visible 为 char(1)，'0' 显示 / '1' 隐藏 -->
            <el-tag :type="isHidden(row) ? 'info' : 'success'" size="small">
              {{ isHidden(row) ? '隐藏' : '显示' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <!-- v1.1：status 为 char(1)，'0' 正常 / '1' 停用 -->
            <el-tag :type="isDisabled(row) ? 'info' : 'success'" size="small">
              {{ isDisabled(row) ? '停用' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'system:menu:edit'" link type="primary" @click="openDialog(row)">
              编辑
            </el-button>
            <el-button v-hasPerm="'system:menu:add'" link type="success" @click="openDialog(null, row)">
              新增下级
            </el-button>
            <el-button v-hasPerm="'system:menu:remove'" link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.menuId ? '编辑菜单' : '新增菜单'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'menuName', value: 'menuId' }"
            check-strictly
            placeholder="选择上级菜单"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item prop="menuName" label="菜单名称">
          <el-input v-model="form.menuName" placeholder="如：商品审核" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.orderNum" :min="0" :max="999" />
        </el-form-item>
        <template v-if="form.menuType !== 'F'">
          <el-form-item label="路由地址">
            <el-input v-model="form.path" placeholder="目录以 / 开头，菜单为相对路径，如 user" />
          </el-form-item>
          <el-form-item v-if="form.menuType === 'C'" label="组件路径">
            <el-input v-model="form.component" placeholder="如 system/user/index（对应 views 下路径）" />
          </el-form-item>
          <el-form-item label="菜单图标">
            <el-input v-model="form.icon" placeholder="Element Plus 图标名，如 user" />
          </el-form-item>
        </template>
        <el-form-item label="权限标识">
          <el-input v-model="form.perms" placeholder="如 system:user:list（按钮必填）" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="显示状态">
              <el-radio-group v-model="form.visible">
                <el-radio value="0">显示</el-radio>
                <el-radio value="1">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单状态">
              <el-radio-group v-model="form.status">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="是否外链">
              <el-radio-group v-model="form.isFrame">
                <el-radio value="0">是</el-radio>
                <el-radio value="1">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否缓存">
              <el-radio-group v-model="form.isCache">
                <el-radio value="0">缓存</el-radio>
                <el-radio value="1">不缓存</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
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
import { Plus, Refresh, Search, Sort } from '@element-plus/icons-vue'
import { addMenu, deleteMenu, getMenuTree, updateMenu } from '@/api/system'
import { resolveIcon } from '@/utils/icons'

const loading = ref(false)
const rawTree = ref([])
const expandAll = ref(true)
const filterName = ref('')
const filterType = ref('')

// 前端过滤：契约的菜单列表接口没有查询参数
const displayTree = computed(() => {
  const name = filterName.value.trim()
  const type = filterType.value
  if (!name && !type) return rawTree.value
  const filterNodes = (nodes) =>
    nodes
      .map((node) => {
        const children = node.children?.length ? filterNodes(node.children) : []
        const match = (!name || node.menuName.includes(name)) && (!type || node.menuType === type)
        if (match || children.length) return { ...node, children }
        return null
      })
      .filter(Boolean)
  return filterNodes(rawTree.value)
})

const parentOptions = computed(() => [
  { menuId: '0', menuName: '主目录', children: rawTree.value }
])

function typeText(type) {
  return { M: '目录', C: '菜单', F: '按钮' }[type] || type
}

function typeTag(type) {
  return { M: 'info', C: 'primary', F: 'warning' }[type] || 'info'
}

// v1.1 状态/显示都是 char(1) 字符串，这里同时兼容后端可能返回数字的情况
function isDisabled(row) {
  return String(row.status) === '1'
}

function isHidden(row) {
  return String(row.visible) === '1'
}

const renderKey = ref(0)

function toggleExpandAll() {
  expandAll.value = !expandAll.value
  // el-table 的 default-expand-all 只在首次渲染生效，这里通过 key 强制重渲染
  renderKey.value += 1
}

async function loadTree() {
  loading.value = true
  try {
    rawTree.value = (await getMenuTree()) || []
  } catch (error) {
    rawTree.value = []
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterName.value = ''
  filterType.value = ''
}

// ---------------- 新增 / 编辑 ----------------
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive(emptyForm())

function emptyForm() {
  return {
    menuId: '',
    parentId: '0',
    menuName: '',
    menuType: 'C',
    orderNum: 1,
    path: '',
    component: '',
    queryParam: '',
    perms: '',
    icon: '',
    visible: '0',
    status: '0',
    isFrame: '1',
    isCache: '0'
  }
}

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }]
}

function openDialog(row, parent) {
  Object.assign(form, emptyForm())
  if (row) {
    Object.assign(form, row, { parentId: String(row.parentId) })
  } else if (parent) {
    form.parentId = String(parent.menuId)
  }
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.menuType !== 'F' && !form.path) {
    ElMessage.warning('目录/菜单必须填写路由地址')
    return
  }
  submitting.value = true
  try {
    if (form.menuId) {
      await updateMenu(form)
      ElMessage.success('修改成功')
    } else {
      await addMenu(form)
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
  ElMessageBox.confirm(`确定删除菜单「${row.menuName}」吗？存在子菜单时需先删除子级。`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      await deleteMenu(row.menuId)
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

.page-container > .el-card + .el-card {
  margin-top: 12px;
}
</style>
