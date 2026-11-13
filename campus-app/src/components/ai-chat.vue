<!-- AI 智能客服 / 商品问答：调 POST /ai/chat，展示 answer + sources 引用来源 -->
<template>
  <view class="ai-chat">
    <view v-if="showHeader" class="header">
      <view class="h-title">
        <text class="avatar">🤖</text>
        <view>
          <text class="name">{{ title }}</text>
          <text class="sub">基于平台知识库的 RAG 智能问答</text>
        </view>
      </view>
    </view>

    <scroll-view class="body" :style="{ height }" scroll-y :scroll-into-view="scrollTarget">
      <view v-for="(m, index) in messages" :id="'msg-' + index" :key="index" class="msg" :class="m.role">
        <view class="bubble">
          <text v-if="m.loading" class="loading">AI 正在思考…</text>
          <text v-else class="text">{{ m.text }}</text>

          <!-- 引用来源 -->
          <view v-if="m.sources && m.sources.length" class="sources">
            <text class="sources-title">📚 引用来源</text>
            <view
              v-for="(s, i) in m.sources"
              :key="i"
              class="source-item"
              hover-class="source-hover"
              :hover-stay-time="60"
              @tap="openSource(s)"
            >
              <text class="source-name">{{ s.title || s.source || '知识库片段' }}</text>
              <text v-if="s.score !== undefined && s.score !== null" class="source-score">
                相关度 {{ Number(s.score).toFixed(2) }}
              </text>
              <text class="source-arrow">›</text>
            </view>
          </view>

          <view v-if="m.meta" class="meta">
            <text>{{ m.meta }}</text>
          </view>

          <!-- 请求失败时的重试入口 -->
          <text v-if="m.failed" class="retry" @tap="retry(m)">回答失败，点此重试</text>
        </view>
      </view>

      <view v-if="suggestions && suggestions.length && messages.length <= 1" class="suggestions">
        <text
          v-for="(s, i) in suggestions"
          :key="i"
          class="sug-item"
          @tap="send(s)"
        >{{ s }}</text>
      </view>
    </scroll-view>

    <view class="input-bar">
      <input
        v-model="question"
        class="input"
        :placeholder="placeholder"
        confirm-type="send"
        :disabled="sending"
        @confirm="send()"
      />
      <view class="send-btn" :class="{ disabled: sending }" @tap="send()">发送</view>
    </view>

    <!-- 引用来源详情：点来源条目打开，展示知识库条目正文摘要 -->
    <view v-if="sourceVisible" class="src-mask" @tap="closeSource">
      <view class="src-sheet" @tap.stop>
        <view class="src-head">
          <text class="src-title">{{ activeSource.title || '知识库片段' }}</text>
          <text class="src-close" @tap="closeSource">✕</text>
        </view>
        <view class="src-meta">
          <text v-if="activeSource.knowledgeId">知识条目 #{{ activeSource.knowledgeId }}</text>
          <text v-if="activeSource.score !== undefined && activeSource.score !== null">
            · 相关度 {{ Number(activeSource.score).toFixed(2) }}
          </text>
        </view>
        <scroll-view class="src-body" scroll-y>
          <text class="src-text">
            {{ activeSource.snippet || '这条知识还没有摘要内容，可在后台「AI 管理 → 知识库」查看全文。' }}
          </text>
        </scroll-view>
        <view class="src-tip">内容来自平台知识库，AI 回答基于该条目检索增强生成</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { aiChat } from '@/api/ai'

const props = defineProps({
  title: { type: String, default: 'AI 智能客服' },
  welcome: {
    type: String,
    default: '你好，我是校园易站 AI 助手 🤖 商品发布、交易流程、订单问题都可以问我。'
  },
  suggestions: { type: Array, default: () => [] },
  placeholder: { type: String, default: '输入你的问题…' },
  height: { type: String, default: '760rpx' },
  showHeader: { type: Boolean, default: true }
})

const sourceVisible = ref(false)
const activeSource = ref({})

function openSource(source) {
  if (!source) return
  activeSource.value = source
  sourceVisible.value = true
}

function closeSource() {
  sourceVisible.value = false
}

const messages = ref([
  { role: 'ai', text: props.welcome, sources: [], meta: '' }
])
const question = ref('')
const sending = ref(false)
const scrollTarget = ref('')

function scrollToBottom() {
  nextTick(() => {
    scrollTarget.value = 'msg-' + (messages.value.length - 1)
  })
}

/** 单次提问的客户端超时（毫秒）：后端还有自己的 30s 读超时，这里必须比它更早放弃，避免一直转圈 */
const CLIENT_TIMEOUT = 35000

async function send(text) {
  const content = String(text || question.value || '').trim()
  if (!content || sending.value) return
  question.value = ''
  messages.value.push({ role: 'user', text: content, sources: [] })
  messages.value.push({ role: 'ai', text: '', loading: true, sources: [], meta: '', question: content })
  // 必须通过响应式代理（messages.value[i]）改字段：直接改 push 进去的原始对象不会触发视图更新，
  // 之前的「AI 正在思考…」一直不消失就是这个原因之一
  const aiMsg = messages.value[messages.value.length - 1]
  sending.value = true
  scrollToBottom()

  let timer = null
  try {
    const data = await Promise.race([
      aiChat({ question: content }),
      new Promise((resolve, reject) => {
        timer = setTimeout(() => reject(new Error('AI_TIMEOUT')), CLIENT_TIMEOUT)
      })
    ])
    aiMsg.loading = false
    aiMsg.failed = false
    aiMsg.text = (data && data.answer) || '暂时没有找到相关答案，可以换个说法再问一次～'
    const rawSources = (data && (data.sources || data.references)) || []
    aiMsg.sources = Array.isArray(rawSources) ? rawSources.filter((s) => s && typeof s === 'object') : []
    const provider = data && data.provider
    const cost = data && data.costMs
    const tags = []
    if (provider) tags.push(provider)
    if (provider === 'mock' || provider === 'rule') tags.push('本地模板回答')
    if (cost) tags.push(`${cost}ms`)
    aiMsg.meta = tags.join(' · ')
  } catch (e) {
    aiMsg.loading = false
    const timeout = e && e.message === 'AI_TIMEOUT'
    aiMsg.text = timeout
      ? 'AI 响应超时了，请点下方重试，或稍后再问一次。'
      : 'AI 客服暂时不可用，请稍后再试，或直接查看帮助说明。'
    aiMsg.failed = true
    aiMsg.meta = ''
  } finally {
    if (timer) clearTimeout(timer)
    sending.value = false
    scrollToBottom()
  }
}

/** 失败消息重试：删掉这条失败气泡，用同样的问题再问一次 */
function retry(m) {
  const q = m && m.question
  const index = messages.value.indexOf(m)
  if (index >= 0) messages.value.splice(index, 1)
  if (q) send(q)
}

function reset() {
  messages.value = [{ role: 'ai', text: props.welcome, sources: [], meta: '' }]
  question.value = ''
  scrollToBottom()
}

defineExpose({ send, reset })
</script>

<style lang="scss" scoped>
.ai-chat {
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
  border-radius: $cm-radius;
  overflow: hidden;
}
.header {
  padding: 24rpx;
  background: linear-gradient(135deg, #2b7fff, #6aa8ff);
  .h-title {
    display: flex;
    align-items: center;
  }
  .avatar {
    font-size: 52rpx;
    margin-right: 16rpx;
  }
  .name {
    display: block;
    color: #fff;
    font-size: 30rpx;
    font-weight: 600;
  }
  .sub {
    display: block;
    color: rgba(255, 255, 255, 0.85);
    font-size: 20rpx;
    margin-top: 4rpx;
  }
}
.body {
  padding: 20rpx;
  box-sizing: border-box;
}
.msg {
  display: flex;
  margin-bottom: 20rpx;
  &.user {
    justify-content: flex-end;
    .bubble {
      background: $cm-primary;
      color: #fff;
      border-top-right-radius: 4rpx;
    }
  }
  &.ai {
    justify-content: flex-start;
    .bubble {
      background: #fff;
      color: $cm-text;
      border-top-left-radius: 4rpx;
    }
  }
  .bubble {
    max-width: 86%;
    padding: 18rpx 22rpx;
    border-radius: $cm-radius;
    font-size: 26rpx;
    line-height: 40rpx;
    word-break: break-word;
  }
  .loading {
    color: $cm-text-3;
  }
  .sources {
    margin-top: 14rpx;
    padding-top: 12rpx;
    border-top: 1rpx dashed $cm-border;
  }
  .sources-title {
    display: block;
    font-size: 22rpx;
    color: $cm-text-3;
    margin-bottom: 8rpx;
  }
  .source-item {
    display: flex;
    justify-content: space-between;
    font-size: 22rpx;
    color: $cm-primary;
    padding: 4rpx 0;
  }
  .source-name {
    flex: 1;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  .source-score {
    color: $cm-text-4;
    margin-left: 12rpx;
  }
  .source-arrow {
    margin-left: 8rpx;
    color: $cm-text-4;
    font-size: 24rpx;
  }
  .source-hover {
    background-color: rgba(64, 158, 255, 0.08);
    border-radius: 8rpx;
  }

  /* 引用来源详情弹层 */
  .src-mask {
    position: fixed;
    left: var(--window-left, 0);
    right: var(--window-right, 0);
    top: 0;
    bottom: 0;
    z-index: 99;
    background-color: rgba(0, 0, 0, 0.45);
    display: flex;
    align-items: flex-end;
  }
  .src-sheet {
    width: 100%;
    background-color: #ffffff;
    border-radius: 24rpx 24rpx 0 0;
    padding: 28rpx 30rpx calc(28rpx + env(safe-area-inset-bottom));
  }
  .src-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .src-title {
    flex: 1;
    font-size: 30rpx;
    font-weight: 600;
    color: $cm-text;
  }
  .src-close {
    padding: 0 8rpx;
    font-size: 28rpx;
    color: $cm-text-4;
  }
  .src-meta {
    margin-top: 8rpx;
    font-size: 22rpx;
    color: $cm-text-4;
  }
  .src-body {
    margin-top: 18rpx;
    max-height: 46vh;
  }
  .src-text {
    font-size: 26rpx;
    line-height: 44rpx;
    color: $cm-text-2;
  }
  .src-tip {
    margin-top: 20rpx;
    padding-top: 16rpx;
    border-top: 1rpx dashed $cm-border;
    font-size: 20rpx;
    color: $cm-text-4;
  }
  .meta {
    margin-top: 8rpx;
    font-size: 20rpx;
    color: $cm-text-4;
  }
  .retry {
    display: inline-block;
    margin-top: 10rpx;
    font-size: 22rpx;
    color: $cm-primary;
    text-decoration: underline;
  }
}
.suggestions {
  padding: 10rpx 0 20rpx;
  display: flex;
  flex-direction: column;
  .sug-item {
    margin-top: 12rpx;
    padding: 14rpx 20rpx;
    background: #fff;
    border-radius: 999rpx;
    font-size: 24rpx;
    color: $cm-primary;
    border: 1rpx solid $cm-primary-light;
  }
}
.input-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 20rpx;
  background: #fff;
  border-top: 1rpx solid $cm-border;
  .input {
    flex: 1;
    height: 68rpx;
    background: $cm-page-bg;
    border-radius: 999rpx;
    padding: 0 24rpx;
    font-size: 26rpx;
  }
  .send-btn {
    margin-left: 16rpx;
    padding: 0 28rpx;
    height: 68rpx;
    line-height: 68rpx;
    background: $cm-primary;
    color: #fff;
    border-radius: 999rpx;
    font-size: 26rpx;
    &.disabled {
      opacity: 0.6;
    }
  }
}
</style>
