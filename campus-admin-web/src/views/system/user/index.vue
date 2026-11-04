<template>
  <div class="page-container">
    <!-- 查询条件 -->
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input
          v-model="query.username"
          placeholder="用户名"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.nickname"
          placeholder="昵称"
          clearable
          style="width: 150px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.phone"
          placeholder="手机号"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-tree-select
          v-model="query.deptId"
          :data="deptTree"
          :props="deptProps"
          placeholder="所属学院"
          clearable
          check-strictly
          style="width: 190px"
        />
        <DictSelect v-model="query.status" dict-type="sys_normal_disable" placeholder="账号状态" width="140px" />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <div class="table-toolbar toolbar">
        <span class="title">用户列表</span>
        <div class="toolbar-actions">
          <el-button v-hasPerm="'system:user:add'" type="primary" :icon="Plus" @click="openDialog()">
            新增用户
          </el-button>
          <el-button
            v-hasPerm="'system:user:remove'"
            type="danger"
            plain
            :icon="Delete"
            :disabled="!selectedIds.length"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        row-key="userId"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="deptName" label="所属学院" min-width="130" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="性别" width="80" align="center">
          <template #default="{ row }">
            <DictTag dict-type="sys_user_gender" :value="row.gender" />
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="roleId in row.roleIds || []" :key="roleId" size="small" effect="plain" class="role-tag">
              {{ roleName(roleId) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="0"
              :inactive-value="1"
              :disabled="!userStore.hasPerm('system:user:edit')"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'system:user:edit'" link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button v-hasPerm="'system:user:resetPwd'" link type="warning" @click="openResetPwd(row)">
              重置密码
            </el-button>
            <el-button v-hasPerm="'system:user:edit'" link type="success" @click="openAssignRole(row)">
              分配角色
            </el-button>
            <el-button v-hasPerm="'system:user:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.userId ? '编辑用户' : '新增用户'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item prop="username" label="用户名">
              <el-input v-model="form.username" :disabled="!!form.userId" placeholder="登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="nickname" label="昵称">
              <el-input v-model="form.nickname" placeholder="显示名称" />
            </el-form-item>
          </el-col>
          <el-col v-if="!form.userId" :span="12">
            <el-form-item prop="password" label="初始密码">
              <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属学院">
              <el-tree-select
                v-model="form.deptId"
                :data="deptTree"
                :props="deptProps"
                check-strictly
                clearable
                placeholder="请选择学院"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="11 位手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="常用邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio value="1">男</el-radio>
                <el-radio value="2">女</el-radio>
                <el-radio value="0">未知</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="0">正常</el-radio>
                <el-radio :value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
                <el-option
                  v-for="role in roleList"
                  :key="role.roleId"
                  :label="role.roleName"
                  :value="role.roleId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="pwdVisible" title="重置密码" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <span>{{ pwdUser.nickname }}（{{ pwdUser.username }}）</span>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdValue" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSubmitting" @click="submitResetPwd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="roleVisible" title="分配角色" width="460px">
      <p class="assign-tip">为用户「{{ currentUser.nickname }}」分配角色：</p>
      <el-checkbox-group v-model="assignRoleIds">
        <el-checkbox v-for="role in roleList" :key="role.roleId" :value="role.roleId" class="role-checkbox">
          {{ role.roleName }}（{{ role.roleKey }}）
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="submitAssignRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus, Refresh, Search } from '@element-plus/icons-vue'
import DictSelect from '@/components/DictSelect/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import { useTable } from '@/composables/useTable'
import {
  addUser,
  changeUserStatus,
  deleteUser,
  getDeptTree,
  getRoleList,
  getUser,
  getUserPage,
  resetUserPwd,
  updateUser
} from '@/api/system'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const { loading, list, total, query, getList, handleSearch, resetQuery, handlePagination } = useTable(
  getUserPage,
  { username: '', nickname: '', phone: '', status: '', deptId: '' }
)

const deptTree = ref([])
const roleList = ref([])
const deptProps = { label: 'deptName', value: 'deptId', children: 'children' }

const selectedIds = ref([])

function onSelectionChange(rows) {
  selectedIds.value = rows.map((row) => row.userId)
}

function roleName(roleId) {
  const role = roleList.value.find((item) => String(item.roleId) === String(roleId))
  return role ? role.roleName : roleId
}

function handleReset() {
  resetQuery()
}

// ---------------- 新增 / 编辑 ----------------
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive(emptyForm())

function emptyForm() {
  return {
    userId: '',
    username: '',
    nickname: '',
    password: '',
    deptId: '',
    phone: '',
    email: '',
    sex: '0',
    status: 0,
    roleIds: [],
    remark: ''
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

async function openDialog(row) {
  Object.assign(form, emptyForm())
  if (row) {
    try {
      const data = await getUser(row.userId)
      Object.assign(form, {
        ...emptyForm(),
        ...data,
        roleIds: (data?.roleIds || []).map(String)
      })
    } catch (error) {
      return
    }
  }
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (form.userId) {
      await updateUser(form)
      ElMessage.success('修改成功')
    } else {
      await addUser(form)
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

// ---------------- 删除 ----------------
function removeUsers(ids) {
  ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个用户吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteUser(ids.join(','))
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

function handleDelete(row) {
  removeUsers([row.userId])
}

function handleBatchDelete() {
  if (selectedIds.value.length) removeUsers(selectedIds.value)
}

// ---------------- 启用 / 停用 ----------------
async function handleStatusChange(row) {
  try {
    await changeUserStatus(row.userId, row.status)
    ElMessage.success(row.status === 0 ? '已启用' : '已停用')
  } catch (error) {
    // 失败回滚开关状态
    row.status = row.status === 0 ? 1 : 0
  }
}

// ---------------- 重置密码 ----------------
const pwdVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdValue = ref('')
const pwdUser = ref({})

function openResetPwd(row) {
  pwdUser.value = row
  pwdValue.value = ''
  pwdVisible.value = true
}

async function submitResetPwd() {
  if (!pwdValue.value || pwdValue.value.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  pwdSubmitting.value = true
  try {
    await resetUserPwd({ userId: pwdUser.value.userId, password: pwdValue.value })
    ElMessage.success('密码已重置')
    pwdVisible.value = false
  } catch (error) {
    // 拦截器已提示
  } finally {
    pwdSubmitting.value = false
  }
}

// ---------------- 分配角色 ----------------
// 契约里没有单独的「分配角色」接口，用户对象本身带 roleIds，
// 因此这里复用 PUT /system/user 完成角色绑定。
const roleVisible = ref(false)
const roleSubmitting = ref(false)
const assignRoleIds = ref([])
const currentUser = ref({})

function openAssignRole(row) {
  currentUser.value = row
  assignRoleIds.value = (row.roleIds || []).map(String)
  roleVisible.value = true
}

async function submitAssignRole() {
  roleSubmitting.value = true
  try {
    await updateUser({ ...currentUser.value, roleIds: assignRoleIds.value })
    ElMessage.success('角色分配成功')
    roleVisible.value = false
    getList()
  } catch (error) {
    // 拦截器已提示
  } finally {
    roleSubmitting.value = false
  }
}

onMounted(async () => {
  try {
    const [depts, roles] = await Promise.all([getDeptTree(), getRoleList()])
    deptTree.value = depts || []
    roleList.value = roles || []
  } catch (error) {
    // 下拉数据加载失败不阻塞表格
  }
})
</script>

<style scoped>
.toolbar {
  padding: 0 0 12px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.role-tag {
  margin-right: 4px;
}

.assign-tip {
  margin: 0 0 12px;
  color: #606266;
}

.role-checkbox {
  display: block;
  margin-bottom: 6px;
}
</style>
