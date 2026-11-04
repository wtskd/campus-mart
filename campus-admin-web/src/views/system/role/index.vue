<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input
          v-model="query.roleName"
          placeholder="角色名称"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.roleKey"
          placeholder="权限字符"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <DictSelect v-model="query.status" dict-type="sys_normal_disable" placeholder="状态" width="140px" />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">角色列表</span>
        <el-button v-hasPerm="'system:role:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增角色
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="roleId" label="ID" width="70" align="center" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleKey" label="权限字符" min-width="140" />
        <el-table-column prop="roleSort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="0"
              :inactive-value="1"
              :disabled="!userStore.hasPerm('system:role:edit')"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'system:role:edit'" link type="primary" @click="openDialog(row)">
              编辑
            </el-button>
            <el-button v-hasPerm="'system:role:edit'" link type="success" @click="openPermDialog(row)">
              分配权限
            </el-button>
            <el-button v-hasPerm="'system:role:remove'" link type="danger" @click="handleDelete(row)">
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

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.roleId ? '编辑角色' : '新增角色'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item prop="roleName" label="角色名称">
          <el-input v-model="form.roleName" placeholder="如：商品审核员" />
        </el-form-item>
        <el-form-item prop="roleKey" label="权限字符">
          <el-input v-model="form.roleKey" placeholder="如：item:auditor" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.roleSort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单权限 -->
    <el-dialog v-model="permVisible" :title="`分配权限 - ${currentRole.roleName || ''}`" width="520px">
      <div class="perm-toolbar">
        <el-checkbox v-model="expandAll" @change="toggleExpand">展开/折叠</el-checkbox>
        <el-checkbox v-model="checkAll" @change="toggleCheckAll">全选/全不选</el-checkbox>
        <el-checkbox v-model="checkStrictly">父子联动</el-checkbox>
      </div>
      <el-tree
        ref="treeRef"
        :data="menuTree"
        :props="treeProps"
        node-key="menuId"
        show-checkbox
        :check-strictly="!checkStrictly"
        :default-expanded-keys="expandedKeys"
        class="perm-tree"
      />
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSubmitting" @click="submitPerm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import DictSelect from '@/components/DictSelect/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import { useTable } from '@/composables/useTable'
import {
  addRole,
  changeRoleStatus,
  deleteRole,
  getMenuTree,
  getRoleMenuIds,
  getRolePage,
  updateRole,
  updateRoleMenu
} from '@/api/system'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const { loading, list, total, query, getList, handleSearch, resetQuery, handlePagination } = useTable(
  getRolePage,
  { roleName: '', roleKey: '', status: '' }
)

// ---------------- 新增 / 编辑 ----------------
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive(emptyForm())

function emptyForm() {
  return { roleId: '', roleName: '', roleKey: '', roleSort: 0, status: 0, remark: '' }
}

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }]
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
    if (form.roleId) {
      await updateRole(form)
      ElMessage.success('修改成功')
    } else {
      await addRole(form)
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
  ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteRole(row.roleId)
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

async function handleStatusChange(row) {
  try {
    await changeRoleStatus(row.roleId, row.status)
    ElMessage.success(row.status === 0 ? '已启用' : '已停用')
  } catch (error) {
    row.status = row.status === 0 ? 1 : 0
  }
}

// ---------------- 菜单权限 ----------------
const permVisible = ref(false)
const permSubmitting = ref(false)
const menuTree = ref([])
const treeRef = ref(null)
const currentRole = ref({})
const treeProps = { label: 'menuName', children: 'children' }
const expandAll = ref(false)
const checkAll = ref(false)
const checkStrictly = ref(true)
const expandedKeys = ref([])

async function openPermDialog(row) {
  currentRole.value = row
  permVisible.value = true
  try {
    if (!menuTree.value.length) {
      menuTree.value = (await getMenuTree()) || []
    }
    const checked = (await getRoleMenuIds(row.roleId)) || []
    // 只勾选叶子节点，避免父节点联动导致回显错乱
    const leafIds = new Set()
    collectLeafIds(menuTree.value, leafIds)
    const leafChecked = checked.filter((id) => leafIds.has(id))
    await nextTick()
    treeRef.value?.setCheckedKeys([], false)
    treeRef.value?.setCheckedKeys(leafChecked, false)
  } catch (error) {
    // 拦截器已提示
  }
}

function collectLeafIds(nodes, output) {
  ;(nodes || []).forEach((node) => {
    if (node.children && node.children.length) {
      collectLeafIds(node.children, output)
    } else {
      output.add(node.menuId)
    }
  })
}

function toggleExpand(value) {
  const nodes = treeRef.value?.store?.nodesMap || {}
  Object.values(nodes).forEach((node) => {
    node.expanded = value
  })
}

function toggleCheckAll(value) {
  if (value) {
    const allIds = []
    const walk = (nodes) => {
      nodes.forEach((node) => {
        allIds.push(node.menuId)
        if (node.children?.length) walk(node.children)
      })
    }
    walk(menuTree.value)
    treeRef.value?.setCheckedKeys(allIds, false)
  } else {
    treeRef.value?.setCheckedKeys([], false)
  }
}

async function submitPerm() {
  // 半选父节点也要提交，后端才能共享父级路由
  const checked = treeRef.value?.getCheckedKeys() || []
  const halfChecked = treeRef.value?.getHalfCheckedKeys() || []
  permSubmitting.value = true
  try {
    await updateRoleMenu(currentRole.value.roleId, [...checked, ...halfChecked])
    ElMessage.success('权限保存成功')
    permVisible.value = false
  } catch (error) {
    // 拦截器已提示
  } finally {
    permSubmitting.value = false
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
}

.perm-toolbar {
  display: flex;
  gap: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
}

.perm-tree {
  max-height: 420px;
  overflow: auto;
}
</style>
