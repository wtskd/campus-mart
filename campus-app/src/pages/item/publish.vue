<!-- 发布/编辑商品：图片上传 + 分类成色 + AI 一键文案 -->
<template>
  <view class="page">
    <view v-if="isEdit" class="edit-tip">正在编辑商品「{{ form.itemId }}」，保存后需重新审核</view>

    <!-- 图片上传 -->
    <view class="card">
      <view class="card-title-row">
        <text class="card-title">商品图片</text>
        <text class="card-tip">最多 9 张，第一张为封面</text>
      </view>
      <view class="img-list">
        <view v-for="(img, i) in images" :key="i" class="img-item">
          <image class="img" :src="mediaUrl(img)" mode="aspectFill" @tap="previewImage(i)" />
          <text v-if="i === 0" class="cover-tag">封面</text>
          <text class="del" @tap.stop="removeImage(i)">✕</text>
        </view>
        <view v-if="images.length < 9" class="img-add" @tap="chooseImage">
          <text class="plus">＋</text>
          <text class="add-text">添加图片</text>
        </view>
      </view>
    </view>

    <!-- 基本信息 -->
    <view class="card">
      <view class="card-title-row">
        <text class="card-title">基本信息</text>
        <view class="ai-btn" @tap="aiCopywriting">
          <text>✨ {{ aiLoading ? 'AI 撰写中…' : 'AI 帮我写' }}</text>
        </view>
      </view>

      <view class="form-item">
        <text class="label">标题<text class="required">*</text></text>
        <input v-model="form.title" class="input" maxlength="40" placeholder="例如：iPhone 12 128G 国行 95新" />
      </view>

      <view class="form-item">
        <text class="label">一句话卖点</text>
        <input v-model="form.summary" class="input" maxlength="60" placeholder="例如：无拆修，配件齐全" />
      </view>

      <view class="form-item" @tap="openCategoryPicker">
        <text class="label">分类<text class="required">*</text></text>
        <view class="picker-value" :class="{ placeholder: !categoryLabel }">
          {{ categoryLabel || '请选择分类' }}
        </view>
        <text class="arrow">›</text>
      </view>

      <view class="form-item" @tap="openConditionPicker">
        <text class="label">成色<text class="required">*</text></text>
        <view class="picker-value" :class="{ placeholder: !form.itemCondition }">
          {{ form.itemCondition ? conditionText(form.itemCondition) : '请选择成色' }}
        </view>
        <text class="arrow">›</text>
      </view>

      <view class="form-item">
        <text class="label">售价<text class="required">*</text></text>
        <input v-model="form.price" class="input" type="digit" placeholder="0.00" />
        <text class="unit">元</text>
      </view>

      <view class="form-item">
        <text class="label">原价</text>
        <input v-model="form.originalPrice" class="input" type="digit" placeholder="选填，用于展示折扣" />
        <text class="unit">元</text>
      </view>

      <view class="form-item" @tap="openTradePicker">
        <text class="label">交易类型</text>
        <view class="picker-value">{{ tradeTypeText(form.tradeType) }}</view>
        <text class="arrow">›</text>
      </view>

      <view class="form-item">
        <text class="label">交易地点</text>
        <input v-model="form.location" class="input" maxlength="40" placeholder="例如：东区宿舍楼下" />
      </view>

      <view class="form-item">
        <text class="label">标签</text>
        <input v-model="tagsText" class="input" maxlength="60" placeholder="用逗号分隔，如：国行,无拆修" />
      </view>
    </view>

    <!-- 描述 -->
    <view class="card">
      <view class="card-title-row">
        <text class="card-title">详细描述<text class="required">*</text></text>
        <text class="counter">{{ (form.description || '').length }}/500</text>
      </view>
      <textarea
        v-model="form.description"
        class="textarea"
        maxlength="500"
        placeholder="描述商品的新旧程度、入手时间、配件情况、可议价空间等，信息越详细越好卖～"
      />
      <view v-if="aiMeta" class="ai-meta">{{ aiMeta }}</view>
    </view>

    <!-- 分类选择弹层：先选大类，再选小类 -->
    <view v-if="catVisible" class="cat-mask" @tap="catVisible = false">
      <view class="cat-sheet" @tap.stop>
        <view class="cat-head">
          <text v-if="catLevel === 2" class="cat-back" @tap.stop="backToRoots">‹ 返回</text>
          <text class="cat-title">{{ catLevel === 1 ? '选择大类' : catRoot.categoryName }}</text>
          <text class="cat-close" @tap.stop="catVisible = false">✕</text>
        </view>
        <scroll-view class="cat-body" scroll-y>
          <template v-if="catLevel === 1">
            <view v-for="r in catRoots" :key="r.categoryId" class="cat-row" @tap="pickRoot(r)">
              <view class="cat-row-main">
                <text class="cat-name">{{ r.categoryName }}</text>
                <text class="cat-sub">{{ (r.children || []).length }} 个小类</text>
              </view>
              <text class="cat-arrow">›</text>
            </view>
          </template>
          <template v-else>
            <view
              v-for="s in catSubs"
              :key="s.categoryId"
              class="cat-row"
              @tap="chooseCategory(s)"
            >
              <text class="cat-name">{{ s.categoryName }}</text>
              <text v-if="String(form.categoryId) === String(s.categoryId)" class="cat-check">✓</text>
            </view>
          </template>
        </scroll-view>
      </view>
    </view>

    <view class="submit-wrap">
      <view class="cm-btn cm-btn-primary submit" @tap="submit">{{ isEdit ? '保存修改' : '立即发布' }}</view>
      <text class="submit-tip">发布后进入平台审核，通过后自动上架（AI 会自动检测违规内容）</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getCategoryTree, getItemDetail, publishItem, updateItem } from '@/api/item'
import { aiCopywrite } from '@/api/ai'
import { uploadImage } from '@/api/file'
import { conditionText, tradeTypeText, mediaUrl } from '@/utils/format'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const isEdit = ref(false)
const images = ref([])
const categoryOptions = ref([]) // [{ id, label }]
const categoryTree = ref([]) // 原始分类树
const catVisible = ref(false) // 分类选择弹层
const catLevel = ref(1) // 1 选大类 2 选小类
const catRoot = ref(null) // 已选大类
const aiLoading = ref(false)
const aiMeta = ref('')

const form = ref({
  itemId: '',
  title: '',
  summary: '',
  description: '',
  categoryId: '',
  price: '',
  originalPrice: '',
  itemCondition: 9,
  tradeType: 1,
  location: '',
  tags: []
})
const tagsText = ref('')

const categoryLabel = computed(() => {
  const hit = categoryOptions.value.find((c) => String(c.id) === String(form.value.categoryId))
  return hit ? hit.label : ''
})

const catRoots = computed(() => categoryTree.value)
const catSubs = computed(() => (catRoot.value && catRoot.value.children) || [])

const conditionLabels = ['1成新', '2成新', '3成新', '4成新', '5成新', '6成新', '7成新', '8成新', '9成新', '全新']
// 与后端 biz_item.trade_type 对齐：1 出售 / 2 求购 / 3 交换
const tradeLabels = ['出售', '求购', '交换']

/** 加载分类树（同时保留拍平结构给文案提示用） */
async function loadCategories() {
  try {
    const tree = await getCategoryTree()
    const list = Array.isArray(tree) ? tree : (tree && tree.records) || []
    categoryTree.value = list
    const options = []
    list.forEach((root) => {
      options.push({ id: String(root.categoryId), label: root.categoryName })
      ;(root.children || []).forEach((sub) => {
        options.push({ id: String(sub.categoryId), label: `${root.categoryName} / ${sub.categoryName}` })
      })
    })
    categoryOptions.value = options
  } catch (e) {
    categoryTree.value = []
    categoryOptions.value = []
  }
}

/** 打开分类选择：第一步只展示大类，选完再展示该大类下的小类 */
function openCategoryPicker() {
  if (!categoryTree.value.length) {
    uni.showToast({ title: '分类加载失败，请稍后重试', icon: 'none' })
    return
  }
  catLevel.value = 1
  catRoot.value = null
  catVisible.value = true
}

/** 选大类：有子分类则进入第二步，没有子分类直接采用该大类 */
function pickRoot(root) {
  catRoot.value = root
  if ((root.children || []).length) {
    catLevel.value = 2
  } else {
    chooseCategory(root)
  }
}

/** 落到最终分类（小类，或没有子分类的大类） */
function chooseCategory(node) {
  form.value.categoryId = String(node.categoryId)
  catVisible.value = false
}

function backToRoots() {
  catLevel.value = 1
  catRoot.value = null
}

function openConditionPicker() {
  uni.showActionSheet({
    itemList: conditionLabels,
    success: (res) => {
      form.value.itemCondition = res.tapIndex + 1
    }
  })
}

function openTradePicker() {
  uni.showActionSheet({
    itemList: tradeLabels,
    success: (res) => {
      form.value.tradeType = res.tapIndex + 1
    }
  })
}

function chooseImage() {
  const remain = 9 - images.value.length
  uni.chooseImage({
    count: remain,
    sizeType: ['compressed'],
    success: async (res) => {
      const paths = res.tempFilePaths || []
      for (const p of paths) {
        try {
          uni.showLoading({ title: '上传中...', mask: true })
          const data = await uploadImage(p)
          if (data && data.url) images.value.push(data.url)
        } catch (e) {
          uni.showToast({ title: '图片上传失败', icon: 'none' })
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}

function removeImage(index) {
  images.value.splice(index, 1)
}

function previewImage(index) {
  uni.previewImage({ current: index, urls: images.value.map((u) => mediaUrl(u)) })
}

/** AI 一键文案：调用 /ai/copywrite，回填标题/卖点/描述/标签 */
async function aiCopywriting() {
  if (!form.value.title && !form.value.summary) {
    uni.showToast({ title: '先填写标题或关键词，AI 才有素材', icon: 'none' })
    return
  }
  if (aiLoading.value) return
  aiLoading.value = true
  aiMeta.value = ''
  try {
    const keywords = [form.value.title, form.value.summary].filter(Boolean).join(' ')
    const categoryName = categoryLabel.value || ''
    const data = await aiCopywrite({
      keywords,
      category: categoryName,
      condition: form.value.itemCondition,
      originalPrice: Number(form.value.originalPrice) || null,
      price: Number(form.value.price) || null,
      remark: form.value.description || ''
    })
    if (data) {
      if (data.title) form.value.title = data.title
      if (data.summary) form.value.summary = data.summary
      if (data.description) form.value.description = data.description
      if (Array.isArray(data.tags) && data.tags.length) {
        form.value.tags = data.tags
        tagsText.value = data.tags.join(',')
      }
      if (data.suggestPrice && !form.value.price) form.value.price = String(data.suggestPrice)
      const provider = data.provider ? `provider: ${data.provider}` : ''
      const cost = data.costMs ? `${data.costMs}ms` : ''
      aiMeta.value = provider ? `已由 AI 生成（${provider}${cost ? ' · ' + cost : ''}），可直接修改` : '已由 AI 生成，可直接修改'
      uni.showToast({ title: '文案已生成', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: 'AI 暂时不可用，请手动填写', icon: 'none' })
  } finally {
    aiLoading.value = false
  }
}

function buildPayload() {
  const tags = tagsText.value
    ? tagsText.value.split(/[,，\s]+/).filter(Boolean).slice(0, 5)
    : form.value.tags || []
  const payload = {
    title: form.value.title.trim(),
    summary: form.value.summary.trim(),
    description: form.value.description.trim(),
    categoryId: String(form.value.categoryId),
    price: Number(form.value.price),
    originalPrice: form.value.originalPrice ? Number(form.value.originalPrice) : null,
    itemCondition: Number(form.value.itemCondition),
    tradeType: Number(form.value.tradeType),
    coverUrl: images.value[0] || '',
    images: images.value.slice(),
    tags,
    location: form.value.location.trim(),
    contactType: 0
  }
  if (isEdit.value) payload.itemId = form.value.itemId
  return payload
}

function validate() {
  if (!images.value.length) return '请至少上传 1 张商品图片'
  if (!form.value.title || form.value.title.trim().length < 5) return '标题至少 5 个字'
  if (!form.value.categoryId) return '请选择商品分类'
  if (!form.value.itemCondition) return '请选择成色'
  if (!form.value.price || Number(form.value.price) <= 0) return '请填写正确的售价'
  if (form.value.originalPrice && Number(form.value.originalPrice) < Number(form.value.price)) {
    return '原价不能低于售价'
  }
  if (!form.value.description || form.value.description.trim().length < 10) {
    return '详细描述至少 10 个字'
  }
  return ''
}

async function submit() {
  if (!userStore.requireLogin()) return
  const err = validate()
  if (err) {
    uni.showToast({ title: err, icon: 'none' })
    return
  }
  try {
    const payload = buildPayload()
    if (isEdit.value) await updateItem(payload)
    else await publishItem(payload)
    uni.showToast({ title: isEdit.value ? '修改成功，等待重新审核' : '发布成功，等待审核', icon: 'none' })
    resetForm()
    setTimeout(() => {
      uni.redirectTo({ url: '/pages/item/my' })
    }, 800)
  } catch (e) {
    /* 已统一 toast */
  }
}

function resetForm() {
  images.value = []
  form.value = {
    itemId: '',
    title: '',
    summary: '',
    description: '',
    categoryId: '',
    price: '',
    originalPrice: '',
    itemCondition: 9,
    tradeType: 2,
    location: '',
    tags: []
  }
  tagsText.value = ''
  isEdit.value = false
  aiMeta.value = ''
}

/** 编辑模式：从商品详情回填表单 */
async function loadForEdit(itemId) {
  try {
    const data = await getItemDetail(itemId)
    if (!data) return
    isEdit.value = true
    images.value = (data.images && data.images.length ? data.images.slice() : data.coverUrl ? [data.coverUrl] : [])
    form.value = {
      itemId: data.itemId,
      title: data.title || '',
      summary: data.summary || '',
      description: data.description || '',
      categoryId: data.categoryId ? String(data.categoryId) : '',
      price: data.price !== undefined && data.price !== null ? String(data.price) : '',
      originalPrice: data.originalPrice !== undefined && data.originalPrice !== null ? String(data.originalPrice) : '',
      itemCondition: Number(data.itemCondition) || 9,
      tradeType: data.tradeType !== undefined && data.tradeType !== null ? Number(data.tradeType) : 2,
      location: data.location || '',
      tags: Array.isArray(data.tags) ? data.tags.slice() : []
    }
    tagsText.value = (form.value.tags || []).join(',')
    if (!categoryOptions.value.length) await loadCategories()
  } catch (e) {
    uni.showToast({ title: '商品信息加载失败', icon: 'none' })
  }
}

onShow(() => {
  userStore.initFromStorage()
  if (!categoryOptions.value.length) loadCategories()
  const pending = uni.getStorageSync('pendingEditItemId')
  if (pending) {
    uni.removeStorageSync('pendingEditItemId')
    loadForEdit(pending)
  } else if (!isEdit.value) {
    // 非编辑进入时保持空白表单
    if (!form.value.itemId && !images.value.length && !form.value.title) {
      // 无需处理，保持默认值
    }
  }
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $cm-page-bg;
  padding-bottom: 60rpx;
}
.edit-tip {
  margin: 20rpx 24rpx 0;
  padding: 16rpx 20rpx;
  background: #fff7e6;
  color: #ff9f0a;
  font-size: 23rpx;
  border-radius: $cm-radius-sm;
}
.card {
  margin: 20rpx 24rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: $cm-radius;
}
.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}
.card-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $cm-text;
}
.card-tip {
  font-size: 21rpx;
  color: $cm-text-3;
}
.counter {
  font-size: 21rpx;
  color: $cm-text-4;
}
.ai-btn {
  padding: 8rpx 22rpx;
  background: linear-gradient(135deg, #eef4ff, #f4ecff);
  color: $cm-primary;
  border-radius: 999rpx;
  font-size: 23rpx;
}
.img-list {
  display: flex;
  flex-wrap: wrap;
  .img-item {
    position: relative;
    width: 200rpx;
    height: 200rpx;
    margin: 0 14rpx 14rpx 0;
  }
  .img {
    width: 200rpx;
    height: 200rpx;
    border-radius: $cm-radius-sm;
    background: #eef0f3;
  }
  .cover-tag {
    position: absolute;
    left: 0;
    top: 0;
    padding: 2rpx 12rpx;
    background: rgba(43, 127, 255, 0.9);
    color: #fff;
    font-size: 18rpx;
    border-bottom-right-radius: 10rpx;
  }
  .del {
    position: absolute;
    right: 0;
    top: 0;
    width: 40rpx;
    height: 40rpx;
    line-height: 40rpx;
    text-align: center;
    background: rgba(0, 0, 0, 0.45);
    color: #fff;
    font-size: 22rpx;
    border-bottom-left-radius: 10rpx;
  }
  .img-add {
    width: 200rpx;
    height: 200rpx;
    border: 2rpx dashed $cm-border;
    border-radius: $cm-radius-sm;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #fafbfc;
    .plus {
      font-size: 48rpx;
      color: $cm-text-4;
      line-height: 1;
    }
    .add-text {
      margin-top: 10rpx;
      font-size: 21rpx;
      color: $cm-text-3;
    }
  }
}
.form-item {
  display: flex;
  align-items: center;
  min-height: 92rpx;
  border-bottom: 1rpx solid $cm-border;
  &:last-child {
    border-bottom: none;
  }
  .label {
    width: 150rpx;
    flex: none;
    font-size: 26rpx;
    color: $cm-text;
  }
  .required {
    color: $cm-danger;
    margin-left: 4rpx;
  }
  .input {
    flex: 1;
    font-size: 26rpx;
    color: $cm-text;
  }
  .picker-value {
    flex: 1;
    font-size: 26rpx;
    color: $cm-text;
    &.placeholder {
      color: $cm-text-4;
    }
  }
  .arrow {
    color: $cm-text-4;
    font-size: 28rpx;
  }
  .unit {
    font-size: 24rpx;
    color: $cm-text-3;
    margin-left: 8rpx;
  }
}
.textarea {
  width: 100%;
  height: 240rpx;
  font-size: 26rpx;
  line-height: 40rpx;
  color: $cm-text;
}
.ai-meta {
  margin-top: 12rpx;
  padding: 12rpx 16rpx;
  background: #f4f0ff;
  color: #7d5bff;
  font-size: 21rpx;
  border-radius: $cm-radius-sm;
}
.submit-wrap {
  padding: 20rpx 24rpx;
  .submit {
    height: 88rpx;
    line-height: 88rpx;
    border-radius: 999rpx;
    font-size: 30rpx;
  }
  .submit-tip {
    display: block;
    margin-top: 16rpx;
    text-align: center;
    font-size: 21rpx;
    color: $cm-text-4;
  }
}

/* 分类选择弹层：先大类，再小类 */
.cat-mask {
  position: fixed;
  top: 0;
  bottom: 0;
  left: var(--window-left, 0);
  right: var(--window-right, 0);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(17, 20, 26, 0.45);
}
.cat-sheet {
  width: calc(100% - 96rpx);
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
}
.cat-head {
  position: relative;
  flex: none;
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1rpx solid $cm-border;
  .cat-title {
    font-size: 30rpx;
    font-weight: 600;
    color: $cm-text;
  }
  .cat-back {
    position: absolute;
    left: 24rpx;
    font-size: 26rpx;
    color: $cm-primary;
  }
  .cat-close {
    position: absolute;
    right: 24rpx;
    font-size: 26rpx;
    color: $cm-text-3;
  }
}
.cat-body {
  flex: 1;
  height: 54vh;
}
.cat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid $cm-border;
  .cat-row-main {
    display: flex;
    align-items: baseline;
  }
  .cat-name {
    font-size: 28rpx;
    color: $cm-text;
  }
  .cat-sub {
    margin-left: 12rpx;
    font-size: 22rpx;
    color: $cm-text-4;
  }
  .cat-arrow {
    font-size: 30rpx;
    color: $cm-text-4;
  }
  .cat-check {
    font-size: 28rpx;
    color: $cm-primary;
  }
}
</style>
