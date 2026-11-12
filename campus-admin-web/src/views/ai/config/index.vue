<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">AI 服务配置</span>
        <div>
          <el-button :icon="Refresh" :loading="loading" @click="loadConfig">刷新</el-button>
          <el-button type="success" :icon="Connection" :loading="testing" @click="handleTest">
            连通性测试
          </el-button>
          <el-button type="primary" :icon="Check" :loading="saving" @click="handleSave">保存配置</el-button>
        </div>
      </div>

      <el-alert
        title="配置写入系统参数表 sys_config 的 ai.* 键；切换 provider 后无需重启（后端每次调用读取参数）。"
        type="info"
        :closable="false"
        show-icon
        class="tip"
      />

      <el-form v-loading="loading" :model="form" label-width="130px" class="config-form">
        <el-form-item label="服务提供方">
          <el-radio-group v-model="form.provider">
            <el-radio value="remote">remote（远程大模型 API）</el-radio>
            <el-radio value="local">local（本地 Ollama）</el-radio>
          </el-radio-group>
          <div class="hint">
            远程失败时后端自动降级到 local → mock 模板兜底，日志中 provider 字段与 status=2 可见
          </div>
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="form.model" placeholder="如 deepseek-chat / qwen2.5:7b" style="max-width: 320px" />
        </el-form-item>
        <el-form-item label="每日调用限额">
          <el-input-number v-model="form.dailyLimit" :min="0" :max="1000000" :step="100" />
          <span class="hint inline">0 表示不限；超过后后端返回 429「请求过于频繁」</span>
        </el-form-item>
        <el-form-item label="单次超时(ms)">
          <el-input-number v-model="form.timeoutMs" :min="1000" :max="120000" :step="1000" />
        </el-form-item>
        <el-form-item label="当前生效">
          <el-descriptions :column="2" size="small" border class="desc">
            <el-descriptions-item label="提示词场景">
              {{ sceneList.length ? sceneList.join('、') : '（未获取到场景列表）' }}
            </el-descriptions-item>
            <el-descriptions-item label="可用 provider">
              <el-tag v-for="p in providers" :key="p" size="small" class="tag">{{ p }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">在线试用（答辩演示）</el-divider>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-input v-model="demo.question" type="textarea" :rows="3" placeholder="智能客服：怎么发布商品？" />
          <el-button class="demo-btn" :loading="demo.chatLoading" @click="runChat">问一问（RAG）</el-button>
        </el-col>
        <el-col :span="8">
          <el-input v-model="demo.title" placeholder="商品标题，如：九成新 iPad Air" />
          <el-button class="demo-btn" :loading="demo.copyLoading" @click="runCopywrite">生成发布文案</el-button>
        </el-col>
        <el-col :span="8">
          <el-input v-model="demo.text" type="textarea" :rows="3" placeholder="待审核内容" />
          <el-button class="demo-btn" :loading="demo.modLoading" @click="runModerate">内容审核</el-button>
        </el-col>
      </el-row>
      <pre v-if="demo.result" class="demo-result">{{ demo.result }}</pre>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Connection, Refresh } from '@element-plus/icons-vue'
import { updateConfig, addConfig, getConfigPage } from '@/api/system'
import { aiChat, aiCopywrite, aiModerate, getAiSceneList } from '@/api/ai'
import request from '@/utils/request'

/**
 * AI 配置页：provider 切换 / 模型 / 日限额
 * 注意：契约 v1.1 未定义 AI 配置读写与连通性测试接口，这里复用系统参数接口（ai.* 键）保证口径不变，
 * 测试接口 POST /ai/config/test 由后端补充后即可生效（缺失时页面给出提示，不算致命错误）。
 */
const providers = ['remote', 'local', 'mock', 'rule']
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const sceneList = ref([])

const form = reactive({
  provider: 'remote',
  model: '',
  dailyLimit: 1000,
  timeoutMs: 15000
})

// 记录每个 ai.* 参数在 sys_config 中的 configId，保存时按 id 更新，不存在则新增
const configMap = reactive({})

const KEY = {
  provider: 'ai.provider',
  model: 'ai.model',
  dailyLimit: 'ai.daily.limit',
  timeoutMs: 'ai.request.timeout'
}

async function loadConfig() {
  loading.value = true
  try {
    const page = await getConfigPage({ current: 1, size: 100 })
    const list = page.records || []
    const aiItems = list.filter((item) => String(item.configKey || '').startsWith('ai.'))
    aiItems.forEach((item) => {
      configMap[item.configKey] = item
    })
    form.provider = configMap[KEY.provider]?.configValue || 'remote'
    form.model = configMap[KEY.model]?.configValue || ''
    form.dailyLimit = Number(configMap[KEY.dailyLimit]?.configValue ?? 1000)
    form.timeoutMs = Number(configMap[KEY.timeoutMs]?.configValue ?? 15000)
  } catch (error) {
    ElMessage.error('AI 配置读取失败：' + (error?.message || '接口异常'))
  } finally {
    loading.value = false
  }
}

function buildPayload(key, configValue) {
  const exist = configMap[key]
  if (exist) {
    return { ...exist, configValue: String(configValue) }
  }
  return {
    configName: key,
    configKey: key,
    configValue: String(configValue),
    configType: 'N',
    remark: 'AI 服务配置'
  }
}

async function saveOne(key, value) {
  const exist = configMap[key]
  if (exist) {
    await updateConfig(buildPayload(key, value))
  } else {
    const created = await addConfig(buildPayload(key, value))
    if (created && created.configId) configMap[key] = created
  }
}

async function handleSave() {
  saving.value = true
  try {
    await saveOne(KEY.provider, form.provider)
    await saveOne(KEY.model, form.model)
    await saveOne(KEY.dailyLimit, form.dailyLimit)
    await saveOne(KEY.timeoutMs, form.timeoutMs)
    ElMessage.success('AI 配置已保存')
  } catch (error) {
    ElMessage.error('保存失败：' + (error?.message || '接口异常'))
  } finally {
    saving.value = false
  }
}

async function handleTest() {
  testing.value = true
  try {
    // 契约暂无该接口：后端补充后即可真实探测 remote/local 连通性
    const res = await request.post('/ai/config/test', { provider: form.provider, model: form.model })
    ElMessage.success(`连通性正常：${res.provider || form.provider}，耗时 ${res.costMs ?? '-'}ms`)
  } catch (error) {
    const msg = error?.message || ''
    if (msg.includes('404') || msg.includes('不存在')) {
      ElMessage.warning('后端尚未实现 /ai/config/test（契约缺口），请补充后重试')
    } else {
      ElMessage.error('连通性测试失败：' + msg)
    }
  } finally {
    testing.value = false
  }
}

const demo = reactive({
  question: '怎么发布商品？',
  title: '',
  text: '',
  result: '',
  chatLoading: false,
  copyLoading: false,
  modLoading: false
})

async function runChat() {
  if (!demo.question) return ElMessage.warning('请输入问题')
  demo.chatLoading = true
  try {
    const res = await aiChat({ question: demo.question })
    demo.result = JSON.stringify(
      { answer: res.answer, sources: res.sources, provider: res.provider, costMs: res.costMs },
      null,
      2
    )
  } catch (error) {
    demo.result = '调用失败：' + (error?.message || '接口异常')
  } finally {
    demo.chatLoading = false
  }
}

async function runCopywrite() {
  if (!demo.title) return ElMessage.warning('请输入商品标题')
  demo.copyLoading = true
  try {
    const res = await aiCopywrite({ title: demo.title })
    demo.result = JSON.stringify(res, null, 2)
  } catch (error) {
    demo.result = '调用失败：' + (error?.message || '接口异常')
  } finally {
    demo.copyLoading = false
  }
}

async function runModerate() {
  if (!demo.text) return ElMessage.warning('请输入待审核内容')
  demo.modLoading = true
  try {
    // title/description 是审核接口的主字段，只传 content 会让规则与模型都对着空文本判断
    const res = await aiModerate({ title: '', description: demo.text, content: demo.text, scene: 'item' })
    demo.result = JSON.stringify(res, null, 2)
  } catch (error) {
    demo.result = '调用失败：' + (error?.message || '接口异常')
  } finally {
    demo.modLoading = false
  }
}

onMounted(async () => {
  try {
    const scenes = await getAiSceneList()
    sceneList.value = (scenes || []).map((s) => s.sceneName || s.scene || s)
  } catch (error) {
    sceneList.value = []
  }
  loadConfig()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.tip {
  margin-bottom: 16px;
}

.config-form {
  max-width: 820px;
}

.hint {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.hint.inline {
  margin-left: 10px;
}

.desc {
  width: 100%;
}

.tag {
  margin-right: 6px;
}

.demo-btn {
  margin-top: 10px;
}

.demo-result {
  margin-top: 14px;
  padding: 12px;
  background: #0b1020;
  color: #7ee787;
  border-radius: 6px;
  max-height: 260px;
  overflow: auto;
  font-size: 12px;
}
</style>
