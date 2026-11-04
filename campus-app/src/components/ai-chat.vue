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
            <view v-for="(s, i) in m.sources" :key="i" class="source-item">
              <text class="source-name">{{ s.title || s.source || '知识库片段' }}</text>
              <text v-if="s.score !== undefined && s.score !== null" class="source-score">
                相关度 {{ Number(s.score).toFixed(2) }}
              </text>
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
