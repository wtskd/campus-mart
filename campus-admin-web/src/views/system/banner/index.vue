<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <div class="search-bar">
        <el-input
          v-model="query.title"
          placeholder="轮播标题"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <DictSelect v-model="query.status" dict-type="sys_normal_disable" placeholder="状态" width="140px" />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">首页轮播图</span>
        <el-button v-hasPerm="'item:banner:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增轮播
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="预览图" width="160" align="center">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" fit="cover" class="banner-thumb" :preview-src-list="[row.imageUrl]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="linkUrl" label="跳转地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'info' : 'success'" size="small">
              {{ row.status === 1 ? '停用' : '展示中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'item:banner:edit'" link type="primary" @click="openDialog(row)">
              编辑
            </el-button>
            <el-button v-hasPerm="'item:banner:edit'" link type="success" @click="toggleStatus(row)">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-button>
            <el-button v-hasPerm="'item:banner:remove'" link type="danger" @click="handleDelete(row)">
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

    <el-dialog v-model="dialogVisible" :title="form.bannerId ? '编辑轮播' : '新增轮播'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item prop="title" label="标题">
          <el-input v-model="form.title" placeholder="如：开学季 · 闲置好物专场" />
        </el-form-item>
        <el-form-item label="轮播图">
          <ImageUpload v-model="form.imageUrl" />
        </el-form-item>
        <el-form-item label="跳转类型">
          <el-select v-model="form.linkType" style="width: 100%">
            <el-option label="不跳转（仅展示）" value="none" />
            <el-option label="站内页面（/pages/...）" value="page" />
            <el-option label="商品详情（item:商品ID）" value="item" />
            <el-option label="一级分类（category:分类ID）" value="category" />
            <el-option label="搜索关键词（search:关键词）" value="search" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.linkType !== 'none'" label="跳转目标">
          <el-input v-model="form.linkValue" :placeholder="linkPlaceholder" />
          <div class="link-hint">{{ linkHint }}</div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="1" :max="99" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">启用</el-radio>
            <el-radio :value="1">停用</el-radio>
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
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import DictSelect from '@/components/DictSelect/index.vue'
import ImageUpload from '@/components/ImageUpload/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import { useTable } from '@/composables/useTable'
import { addBanner, deleteBanner, getBannerPage, updateBanner } from '@/api/item'

const { loading, list, total, query, getList, handleSearch, resetQuery, handlePagination } = useTable(
  getBannerPage,
  { title: '', status: '' }
)

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive(emptyForm())

function emptyForm() {
  return {
    bannerId: '',
    title: '',
    imageUrl: '',
    linkUrl: '',
    linkType: 'none',
    linkValue: '',
    sort: 1,
    status: 0,
    remark: ''
  }
}

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请上传轮播图', trigger: 'change' }]
}

/** 后台把 link_url 拆成「类型 + 目标」，避免运营手写前缀写错 */
function parseLink(link) {
  const value = String(link || '').trim()
  if (!value) return { linkType: 'none', linkValue: '' }
  if (value.indexOf('item:') === 0) return { linkType: 'item', linkValue: value.slice(5) }
  if (value.indexOf('category:') === 0) return { linkType: 'category', linkValue: value.slice(9) }
  if (value.indexOf('search:') === 0) return { linkType: 'search', linkValue: value.slice(7) }
  return { linkType: 'page', linkValue: value }
}

function buildLink(type, value) {
  const target = String(value || '').trim()
  if (!target || type === 'none') return ''
  if (type === 'item') return `item:${target}`
  if (type === 'category') return `category:${target}`
  if (type === 'search') return `search:${target}`
  return target.startsWith('/pages/') ? target : `/pages/${target.replace(/^\/+/, '')}`
}

const linkPlaceholder = computed(() => {
  const map = {
    page: '/pages/category/category?categoryId=2',
    item: '商品 ID，例如 3161',
    category: '一级分类 ID，例如 2',
    search: '关键词，例如 教材'
  }
  return map[form.linkType] || ''
})

const linkHint = computed(() => {
  const map = {
    page: '只支持站内页面（/pages/ 开头）；tabBar 页面（分类/发布/消息/我的）会自动用 switchTab',
    item: '用户端点击后直达该商品详情页',
    category: '用户端点击后进入分类页并选中该一级分类',
    search: '用户端点击后进入搜索页并自动搜索该关键词'
  }
  return map[form.linkType] || ''
})

function openDialog(row) {
  const base = emptyForm()
  const parsed = parseLink(row && row.linkUrl)
  Object.assign(form, base, row || {}, parsed)
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...form, linkUrl: buildLink(form.linkType, form.linkValue) }
    delete payload.linkType
    delete payload.linkValue
    if (form.bannerId) {
      await updateBanner(payload)
      ElMessage.success('修改成功')
    } else {
      await addBanner(payload)
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

async function toggleStatus(row) {
  try {
    await updateBanner({ ...row, status: row.status === 1 ? 0 : 1 })
    ElMessage.success(row.status === 1 ? '已启用' : '已停用')
    getList()
  } catch (error) {
    // 拦截器已提示
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除轮播「${row.title}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteBanner(row.bannerId)
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

.banner-thumb {
  width: 132px;
  height: 56px;
  border-radius: 4px;
}

.link-hint {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}
</style>
