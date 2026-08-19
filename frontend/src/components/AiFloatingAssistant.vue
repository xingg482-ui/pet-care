<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, MagicStick, Minus, Position, Promotion } from '@element-plus/icons-vue'
import { chatWithAi } from '../api/ai'

const CUSTOMER = 'CUSTOMER'

const route = useRoute()
const router = useRouter()
const expanded = ref(false)
const loading = ref(false)
const inputMessage = ref('')
const chatBodyRef = ref()
const messages = ref([])
const activeSessionId = ref(null)

const role = computed(() => localStorage.getItem('petCareRole') || 'SUPER_ADMIN')
const isCustomer = computed(() => role.value === CUSTOMER)
const fullPagePath = computed(() => (isCustomer.value ? '/my-ai-consult' : '/ai-consult'))
const hidden = computed(() => route.path === '/ai-consult' || route.path === '/my-ai-consult' || route.path === '/login')

const routeContextMap = {
  '/dashboard': { sourcePage: 'DASHBOARD', contextType: 'DASHBOARD', question: '帮我总结今天需要关注的经营事项' },
  '/customers': { sourcePage: 'CUSTOMERS', contextType: 'CUSTOMER_ANALYSIS', question: '哪些客户值得重点维护？' },
  '/pets': { sourcePage: 'PETS', contextType: 'PET_ADMIN', question: '宠物档案管理有哪些需要注意的地方？' },
  '/boarding': { sourcePage: 'BOARDING', contextType: 'BOARDING_ADMIN', question: '托管服务的标准流程和注意事项是什么？' },
  '/orders': { sourcePage: 'ORDERS', contextType: 'ORDER_ADMIN', question: '今天哪些订单需要优先处理？' },
  '/finance': { sourcePage: 'FINANCE', contextType: 'FINANCE', question: '分析本月营收、成本和净利润情况' },
  '/service-items': { sourcePage: 'SERVICE_ITEMS', contextType: 'SERVICE_ITEM', question: '哪些服务项目利润表现更好？' },
  '/my-home': { sourcePage: 'MY_HOME', contextType: 'CUSTOMER_HOME', question: '我可以咨询哪些养宠和服务问题？' },
  '/my-pets': { sourcePage: 'MY_PETS', contextType: 'PET', question: '根据我的宠物档案，有哪些日常护理建议？' },
  '/my-orders': { sourcePage: 'MY_ORDERS', contextType: 'ORDER', question: '我的订单当前状态是什么意思？' },
  '/my-boarding': { sourcePage: 'MY_BOARDING', contextType: 'BOARDING', question: '托管每天会做哪些照护？' },
  '/my-support': { sourcePage: 'MY_SUPPORT', contextType: 'SUPPORT', question: '我的情况适合 AI 解答还是创建工单？' },
}

const currentContext = computed(() => {
  const exact = routeContextMap[route.path]
  if (exact) return exact
  if (route.path.startsWith('/orders/')) {
    return { sourcePage: 'ORDER_DETAIL', contextType: 'ORDER', question: '这个订单详情里我应该重点关注什么？' }
  }
  if (route.path.startsWith('/my-orders/')) {
    return { sourcePage: 'MY_ORDER_DETAIL', contextType: 'ORDER', question: '我的订单详情和下一步操作是什么？' }
  }
  return isCustomer.value
    ? { sourcePage: 'CUSTOMER_PAGE', contextType: 'CUSTOMER_HELP', question: '我可以咨询哪些养宠和服务问题？' }
    : { sourcePage: 'ADMIN_PAGE', contextType: 'ADMIN_HELP', question: '帮我分析当前页面可以如何提升效率' }
})

const assistantTitle = computed(() => (isCustomer.value ? 'AI 养宠顾问' : 'AI 经营助手'))
const promptText = computed(() => currentContext.value.question)

watch(() => route.fullPath, () => {
  expanded.value = false
})

function ensureWelcome() {
  if (!messages.value.length) {
    messages.value = [{
      sender: 'assistant',
      content: isCustomer.value
        ? '有问题可以直接问我。我会优先用 FAQ 标准回答，必要时再调用 AI 补充。'
        : '可以问我服务 SOP、经营分析和利润提升建议。我会结合当前页面给出方向。',
    }]
  }
}

function openPanel() {
  ensureWelcome()
  expanded.value = true
}

function closePanel() {
  expanded.value = false
}

function scrollToBottom() {
  nextTick(() => {
    const el = chatBodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function askPrompt() {
  openPanel()
  inputMessage.value = promptText.value
  sendMessage()
}

function openFullPage() {
  router.push({
    path: fullPagePath.value,
    query: {
      q: promptText.value,
      sourcePage: currentContext.value.sourcePage,
      contextType: currentContext.value.contextType,
    },
  })
}

async function sendMessage() {
  const content = inputMessage.value.trim()
  if (!content || loading.value) return
  ensureWelcome()
  messages.value.push({ sender: 'user', content })
  inputMessage.value = ''
  loading.value = true
  scrollToBottom()
  try {
    const result = await chatWithAi({
      message: content,
      sessionId: activeSessionId.value,
      sourcePage: currentContext.value.sourcePage,
      contextType: currentContext.value.contextType,
    })
    activeSessionId.value = result.sessionId
    messages.value.push({
      sender: 'assistant',
      content: result.answer,
      source: result.source,
      riskLevel: result.riskLevel,
    })
  } catch (error) {
    messages.value.push({
      sender: 'assistant',
      content: 'AI 咨询暂时不可用，请稍后重试。紧急问题建议直接联系门店或专业人员。',
      error: true,
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}
</script>

<template>
  <div v-if="!hidden" class="ai-float">
    <section v-if="expanded" class="ai-mini-panel" aria-label="AI 咨询小窗">
      <header class="mini-header">
        <div>
          <strong>{{ assistantTitle }}</strong>
          <span>{{ loading ? '正在回答...' : '快捷咨询' }}</span>
        </div>
        <div class="mini-actions">
          <button type="button" title="打开完整咨询页" @click="openFullPage">
            <el-icon><Promotion /></el-icon>
          </button>
          <button type="button" title="最小化" @click="closePanel">
            <el-icon><Minus /></el-icon>
          </button>
        </div>
      </header>

      <button class="context-prompt" type="button" @click="askPrompt">
        <el-icon><MagicStick /></el-icon>
        <span>{{ promptText }}</span>
      </button>

      <div ref="chatBodyRef" class="mini-chat">
        <div v-for="(message, index) in messages" :key="index" :class="['mini-message', message.sender, { error: message.error }]">
          <p>{{ message.content }}</p>
          <small v-if="message.source === 'FAQ'">FAQ 标准回答</small>
          <small v-else-if="message.source === 'FAQ_DATA'">FAQ + 经营数据</small>
          <small v-else-if="message.source === 'DEEPSEEK'">DeepSeek</small>
        </div>
        <div v-if="loading" class="mini-message assistant">
          <p>正在整理回答...</p>
        </div>
      </div>

      <footer class="mini-input">
        <el-input
          v-model="inputMessage"
          maxlength="500"
          placeholder="输入问题"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <el-button type="primary" :icon="Position" :loading="loading" @click="sendMessage" />
      </footer>
    </section>

    <button v-if="!expanded" class="ai-float-button" type="button" @click="openPanel">
      <el-icon><ChatDotRound /></el-icon>
      <span>AI 咨询</span>
    </button>
  </div>
</template>

<style scoped>
.ai-float {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1200;
}

.ai-float-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 118px;
  height: 48px;
  padding: 0 18px;
  border: 1px solid #ffe1a3;
  border-radius: 999px;
  background: linear-gradient(180deg, #ffc53d 0%, #f5a900 100%);
  color: #ffffff;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 12px 28px rgba(245, 158, 11, 0.28);
}

.ai-float-button .el-icon {
  font-size: 20px;
}

.ai-mini-panel {
  width: min(380px, calc(100vw - 32px));
  border: 1px solid #f1dfbc;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.16);
  overflow: hidden;
}

.mini-header,
.mini-actions,
.context-prompt,
.mini-input {
  display: flex;
  align-items: center;
}

.mini-header {
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #fffaf0 0%, #f8fbff 100%);
  border-bottom: 1px solid #edf2f7;
}

.mini-header strong,
.mini-header span {
  display: block;
}

.mini-header strong {
  color: #111827;
  font-size: 16px;
}

.mini-header span {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.mini-actions {
  gap: 6px;
}

.mini-actions button {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 8px;
  background: #ffffff;
  color: #64748b;
  cursor: pointer;
}

.mini-actions button:hover {
  background: #fff7df;
  color: #f59e0b;
}

.context-prompt {
  width: calc(100% - 24px);
  gap: 8px;
  margin: 12px;
  padding: 10px 12px;
  border: 1px solid #ffe1a3;
  border-radius: 8px;
  background: #fffaf0;
  color: #92400e;
  text-align: left;
  line-height: 1.4;
  cursor: pointer;
}

.context-prompt span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-chat {
  display: grid;
  align-content: start;
  gap: 10px;
  height: 300px;
  padding: 0 12px 12px;
  overflow-y: auto;
}

.mini-message {
  max-width: 88%;
  justify-self: start;
  padding: 10px 12px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
  color: #1f2937;
  font-size: 13px;
  line-height: 1.55;
}

.mini-message.user {
  justify-self: end;
  border-color: #bfdbfe;
  background: #eff6ff;
}

.mini-message.error {
  border-color: #fecaca;
  background: #fff1f2;
}

.mini-message p {
  margin: 0;
  white-space: pre-wrap;
}

.mini-message small {
  display: inline-block;
  margin-top: 6px;
  color: #16a34a;
  font-size: 12px;
}

.mini-input {
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #edf2f7;
}

.mini-input :deep(.el-button--primary) {
  width: 44px;
  height: 40px;
  border-color: #f5b620 !important;
  background: linear-gradient(180deg, #ffc53d 0%, #f5a900 100%) !important;
}

@media (max-width: 640px) {
  .ai-float {
    right: 16px;
    bottom: 16px;
  }
}
</style>
