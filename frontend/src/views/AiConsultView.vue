<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Delete, MagicStick, Position, Service, UserFilled } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { chatWithAi, fetchAiBusinessSummary, fetchAiFaqs, fetchAiSession, fetchAiSessions } from '../api/ai'
import { createMySupportTicket } from '../api/supportTickets'

const CUSTOMER = 'CUSTOMER'

const route = useRoute()
const role = computed(() => localStorage.getItem('petCareRole') || 'SUPER_ADMIN')
const isCustomer = computed(() => role.value === CUSTOMER)
const loading = ref(false)
const faqLoading = ref(false)
const businessLoading = ref(false)
const sessionsLoading = ref(false)
const creatingTicket = ref(false)
const inputMessage = ref('')
const faqList = ref([])
const businessSummary = ref(null)
const sessions = ref([])
const activeSessionId = ref(null)
const messages = ref([
  {
    sender: 'assistant',
    content: isCustomer.value
      ? '你好，我是 AI 养宠顾问。你可以问我服务流程、订单状态、托管照护和日常养宠问题。'
      : '你好，我是 AI 经营助手。你可以问我服务 SOP、优质客户、优质项目、财务分析和利润提升建议。',
  },
])
const chatBodyRef = ref()

const pageTitle = computed(() => (isCustomer.value ? 'AI 养宠顾问' : 'AI 经营助手'))
const pageSubtitle = computed(() => (isCustomer.value
  ? '快速解答服务、订单、托管和科学养宠问题'
  : '辅助整理服务 SOP、经营分析和利润提升思路'))
const sourcePage = computed(() => route.query.sourcePage || (isCustomer.value ? 'MY_AI_CONSULT' : 'AI_CONSULT'))
const contextType = computed(() => route.query.contextType || (isCustomer.value ? 'CUSTOMER_HELP' : 'ADMIN_HELP'))

const fallbackQuestions = computed(() => (isCustomer.value
  ? [
      '洗澡美容服务包含哪些流程？',
      '我的订单状态是什么意思？',
      '托管期间每天会做哪些照护？',
      '疫苗后多久可以洗澡？',
    ]
  : [
      '如何识别优质客户？',
      '哪些服务项目更值得推广？',
      '本月财务状况应该怎么看？',
      '如何提高赚取利润的效率？',
    ]))

const suggestedQuestions = computed(() => {
  const questions = faqList.value.slice(0, 6).map((item) => item.question)
  return questions.length ? questions : fallbackQuestions.value
})

const faqGroups = computed(() => faqList.value.reduce((groups, faq) => {
  const category = faq.category || '推荐问题'
  if (!groups[category]) {
    groups[category] = []
  }
  groups[category].push(faq)
  return groups
}, {}))

const featureCards = computed(() => (isCustomer.value
  ? [
      { title: '服务流程', text: '洗护、美容、托管内容解释' },
      { title: '订单状态', text: '待确认、服务中、待支付说明' },
      { title: '科学养宠', text: '疫苗、驱虫、体重记录提醒' },
    ]
  : [
      { title: '服务 SOP', text: '统一员工服务说明和话术' },
      { title: '经营分析', text: '客户、项目、订单方向建议' },
      { title: '利润提升', text: '成本、定价、套餐优化思路' },
    ]))

function scrollToBottom() {
  nextTick(() => {
    const el = chatBodyRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function useQuestion(question) {
  inputMessage.value = question
  sendMessage()
}

function clearChat() {
  messages.value = [messages.value[0]]
  activeSessionId.value = null
}

async function sendMessage() {
  const content = inputMessage.value.trim()
  if (!content || loading.value) return
  messages.value.push({ sender: 'user', content })
  inputMessage.value = ''
  loading.value = true
  scrollToBottom()
  try {
    const result = await chatWithAi({
      message: content,
      sessionId: activeSessionId.value,
      sourcePage: sourcePage.value,
      contextType: contextType.value,
    })
    activeSessionId.value = result.sessionId
    messages.value.push({
      sender: 'assistant',
      content: result.answer,
      riskLevel: result.riskLevel,
      source: result.source,
      suggestedQuestions: result.suggestedQuestions || [],
    })
    loadSessions()
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

function copyMessage(content) {
  navigator.clipboard?.writeText(content)
  ElMessage.success('已复制回答')
}

function money(value) {
  return `￥${Number(value || 0).toFixed(2)}`
}

function percent(value) {
  return `${Number(value || 0).toFixed(2)}%`
}

async function loadFaqs() {
  faqLoading.value = true
  try {
    faqList.value = await fetchAiFaqs()
  } finally {
    faqLoading.value = false
  }
}

async function loadBusinessSummary() {
  if (isCustomer.value) return
  businessLoading.value = true
  try {
    businessSummary.value = await fetchAiBusinessSummary()
  } finally {
    businessLoading.value = false
  }
}

async function loadSessions() {
  sessionsLoading.value = true
  try {
    sessions.value = await fetchAiSessions()
  } finally {
    sessionsLoading.value = false
  }
}

async function openSession(session) {
  const detail = await fetchAiSession(session.id)
  activeSessionId.value = detail.id
  messages.value = detail.messages.map((message) => ({
    sender: message.sender === 'USER' ? 'user' : 'assistant',
    content: message.content,
    source: message.source,
    riskLevel: message.riskLevel,
  }))
  scrollToBottom()
}

function chatSummaryForTicket() {
  const recent = messages.value.slice(-6).map((message) => {
    const sender = message.sender === 'user' ? '我' : 'AI'
    return `${sender}：${message.content}`
  }).join('\n')
  const content = `我需要人工客服协助处理以下 AI 咨询内容：\n${recent}`
  return content.length > 500 ? `${content.slice(0, 497)}...` : content
}

async function createSupportFromChat() {
  if (!isCustomer.value) return
  creatingTicket.value = true
  try {
    await createMySupportTicket({ content: chatSummaryForTicket() })
    ElMessage.success('已转人工客服，可在“联系客服”中继续沟通')
  } finally {
    creatingTicket.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadFaqs(), loadBusinessSummary(), loadSessions()])
  const queryQuestion = String(route.query.q || '').trim()
  if (queryQuestion) {
    inputMessage.value = queryQuestion
    if (route.query.auto === '1') {
      sendMessage()
    }
  }
})
</script>

<template>
  <AppLayout>
    <div class="ai-page">
      <section class="ai-hero">
        <div>
          <p class="eyebrow">{{ isCustomer ? 'CUSTOMER ASSISTANT' : 'ADMIN ASSISTANT' }}</p>
          <h1>{{ pageTitle }}</h1>
          <p>{{ pageSubtitle }}</p>
        </div>
        <span class="hero-icon"><el-icon><MagicStick /></el-icon></span>
      </section>

      <section class="feature-grid">
        <article v-for="card in featureCards" :key="card.title" class="feature-card">
          <strong>{{ card.title }}</strong>
          <span>{{ card.text }}</span>
        </article>
      </section>

      <section v-if="!isCustomer" v-loading="businessLoading" class="business-context">
        <div class="business-head">
          <div>
            <h2>经营数据上下文</h2>
            <p>AI 回答经营问题时会参考这些系统摘要</p>
          </div>
          <small>{{ businessSummary?.generatedAt || '-' }}</small>
        </div>
        <div class="business-grid">
          <article>
            <span>本周净利润</span>
            <strong>{{ money(businessSummary?.week?.profit) }}</strong>
            <small>利润率 {{ percent(businessSummary?.week?.profitRate) }}</small>
          </article>
          <article>
            <span>本月净利润</span>
            <strong>{{ money(businessSummary?.month?.profit) }}</strong>
            <small>利润率 {{ percent(businessSummary?.month?.profitRate) }}</small>
          </article>
          <article>
            <span>优质客户候选</span>
            <strong>{{ businessSummary?.topCustomers?.[0]?.customerName || '暂无数据' }}</strong>
            <small>{{ businessSummary?.topCustomers?.[0] ? `已支付 ${money(businessSummary.topCustomers[0].paidAmount)}` : '近 30 天已支付客户' }}</small>
          </article>
          <article>
            <span>优质项目候选</span>
            <strong>{{ businessSummary?.topServiceItems?.[0]?.serviceName || '暂无数据' }}</strong>
            <small>{{ businessSummary?.topServiceItems?.[0] ? `利润 ${money(businessSummary.topServiceItems[0].profit)}` : '近 30 天项目利润' }}</small>
          </article>
        </div>
      </section>

      <section class="consult-shell">
        <aside class="question-panel">
          <div class="panel-title">
            <el-icon><ChatDotRound /></el-icon>
            推荐问题
          </div>
          <div v-loading="faqLoading" class="question-list">
            <button v-for="question in suggestedQuestions" :key="question" type="button" @click="useQuestion(question)">
              {{ question }}
            </button>
          </div>
          <el-collapse v-if="Object.keys(faqGroups).length" class="faq-collapse">
            <el-collapse-item v-for="(faqs, category) in faqGroups" :key="category" :title="category" :name="category">
              <button v-for="faq in faqs" :key="faq.id" class="faq-item" type="button" @click="useQuestion(faq.question)">
                {{ faq.question }}
              </button>
            </el-collapse-item>
          </el-collapse>
          <div class="session-block">
            <div class="panel-title compact">
              <el-icon><ChatDotRound /></el-icon>
              最近咨询
            </div>
            <div v-loading="sessionsLoading" class="session-list">
              <button v-for="session in sessions" :key="session.id" type="button" @click="openSession(session)">
                <strong>{{ session.title || 'AI 咨询' }}</strong>
                <span>{{ session.updatedAt }}</span>
              </button>
              <p v-if="!sessions.length && !sessionsLoading">暂无咨询历史</p>
            </div>
          </div>
          <div class="notice-box">
            <strong>使用提示</strong>
            <span v-if="isCustomer">健康和用药建议仅供参考，异常情况请咨询兽医。</span>
            <span v-else>经营建议为辅助判断，具体数据请以系统财务和订单页面为准。</span>
          </div>
        </aside>

        <div class="chat-panel">
          <div class="chat-header">
            <div>
              <h2>{{ pageTitle }}</h2>
              <p>{{ loading ? 'AI 正在整理回答...' : '已连接 DeepSeek API' }}</p>
            </div>
            <div class="chat-header-actions">
              <el-button v-if="isCustomer" :icon="Service" :loading="creatingTicket" text type="warning" @click="createSupportFromChat">转人工</el-button>
              <el-button :icon="Delete" text @click="clearChat">清空</el-button>
            </div>
          </div>

          <div ref="chatBodyRef" class="chat-body">
            <div v-for="(message, index) in messages" :key="index" :class="['message-row', message.sender, { error: message.error }]">
              <span class="message-avatar">
                <el-icon v-if="message.sender === 'assistant'"><MagicStick /></el-icon>
                <el-icon v-else><UserFilled /></el-icon>
              </span>
              <div class="message-bubble">
                <p>{{ message.content }}</p>
                <div v-if="message.sender === 'assistant' && index > 0" class="message-actions">
                  <el-tag v-if="message.source === 'FAQ'" type="success" effect="light">FAQ 标准回答</el-tag>
                  <el-tag v-else-if="message.source === 'FAQ_DATA'" type="success" effect="light">FAQ + 经营数据</el-tag>
                  <el-tag v-else-if="message.source === 'DEEPSEEK'" type="info" effect="light">DeepSeek</el-tag>
                  <el-tag v-if="message.riskLevel === 'MEDICAL_NOTICE'" type="warning" effect="light">健康风险提示</el-tag>
                  <button type="button" @click="copyMessage(message.content)">复制</button>
                </div>
              </div>
            </div>
            <div v-if="loading" class="message-row assistant">
              <span class="message-avatar"><el-icon><MagicStick /></el-icon></span>
              <div class="message-bubble loading-bubble">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 4 }"
              maxlength="1000"
              show-word-limit
              placeholder="输入你想咨询的问题"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <el-button type="primary" :icon="Position" :loading="loading" @click="sendMessage">发送</el-button>
          </div>
        </div>
      </section>
    </div>
  </AppLayout>
</template>

<style scoped>
.ai-page {
  min-height: calc(100vh - 132px);
}

.ai-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
  padding: 24px;
  border: 1px solid #f2dfbd;
  border-radius: 8px;
  background: linear-gradient(135deg, #fffaf0 0%, #ffffff 58%, #f0f9ff 100%);
}

.ai-hero h1 {
  margin: 0;
  color: #111827;
  font-size: 28px;
  line-height: 1.2;
  letter-spacing: 0;
}

.ai-hero p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.eyebrow {
  margin: 0 0 8px !important;
  color: #f59e0b !important;
  font-size: 12px !important;
  font-weight: 800;
  letter-spacing: 0;
}

.hero-icon {
  width: 64px;
  height: 64px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fff7df;
  color: #f59e0b;
  font-size: 32px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.business-context {
  margin-bottom: 16px;
  padding: 18px;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #ffffff;
}

.business-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.business-head h2 {
  margin: 0;
  color: #111827;
  font-size: 18px;
  letter-spacing: 0;
}

.business-head p,
.business-head small {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.business-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.business-grid article {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
}

.business-grid span,
.business-grid small {
  color: #64748b;
  font-size: 12px;
}

.business-grid strong {
  min-width: 0;
  overflow: hidden;
  color: #111827;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feature-card {
  display: grid;
  gap: 6px;
  padding: 16px 18px;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #ffffff;
}

.feature-card strong {
  color: #111827;
  font-size: 16px;
}

.feature-card span {
  color: #64748b;
  font-size: 13px;
}

.consult-shell {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
}

.question-panel,
.chat-panel {
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #ffffff;
}

.question-panel {
  align-self: start;
  display: grid;
  gap: 10px;
  padding: 18px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  color: #111827;
  font-size: 16px;
  font-weight: 800;
}

.panel-title.compact {
  margin-top: 10px;
  font-size: 15px;
}

.question-list {
  display: grid;
  gap: 10px;
  min-height: 52px;
}

.question-panel button {
  width: 100%;
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
  color: #475569;
  text-align: left;
  line-height: 1.35;
  cursor: pointer;
  transition: border-color 180ms ease, background-color 180ms ease, color 180ms ease;
}

.question-panel button:hover {
  border-color: #f6c453;
  background: #fffaf0;
  color: #111827;
}

.faq-collapse {
  margin-top: 4px;
  border-top: 1px solid #edf2f7;
}

.faq-collapse :deep(.el-collapse-item__header) {
  color: #475569;
  font-weight: 700;
}

.faq-collapse :deep(.el-collapse-item__content) {
  display: grid;
  gap: 8px;
  padding-bottom: 12px;
}

.session-block {
  margin-top: 6px;
  padding-top: 10px;
  border-top: 1px solid #edf2f7;
}

.session-list {
  display: grid;
  gap: 8px;
  min-height: 40px;
}

.session-list button {
  min-height: 48px;
  display: grid;
  gap: 4px;
}

.session-list button strong,
.session-list button span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-list button span,
.session-list p {
  margin: 0;
  color: #94a3b8;
  font-size: 12px;
}

.question-panel button.faq-item {
  min-height: 36px;
  padding: 8px 10px;
  background: #ffffff;
  font-size: 13px;
}

.notice-box {
  display: grid;
  gap: 6px;
  margin-top: 8px;
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.notice-box strong {
  color: #111827;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  min-height: 620px;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-bottom: 1px solid #edf2f7;
}

.chat-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-header h2 {
  margin: 0;
  color: #111827;
  font-size: 18px;
  letter-spacing: 0;
}

.chat-header p {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.chat-body {
  flex: 1;
  display: grid;
  align-content: start;
  gap: 16px;
  min-height: 0;
  max-height: 520px;
  padding: 20px;
  overflow-y: auto;
  background: #fbfcfe;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fff7df;
  color: #f59e0b;
}

.message-row.user .message-avatar {
  background: #eff6ff;
  color: #2563eb;
}

.message-bubble {
  max-width: min(720px, 78%);
  padding: 12px 14px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #ffffff;
  color: #1f2937;
  line-height: 1.7;
  white-space: pre-wrap;
}

.message-row.user .message-bubble {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.message-row.error .message-bubble {
  border-color: #fecaca;
  background: #fff1f2;
}

.message-bubble p {
  margin: 0;
}

.message-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.message-actions button {
  border: 0;
  background: transparent;
  color: #f59e0b;
  font-weight: 700;
  cursor: pointer;
}

.loading-bubble {
  display: inline-flex;
  gap: 6px;
  width: auto;
}

.loading-bubble span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f59e0b;
  animation: typing 900ms infinite ease-in-out;
}

.loading-bubble span:nth-child(2) {
  animation-delay: 120ms;
}

.loading-bubble span:nth-child(3) {
  animation-delay: 240ms;
}

.chat-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 112px;
  gap: 12px;
  padding: 16px 18px;
  border-top: 1px solid #edf2f7;
  background: #ffffff;
}

.chat-input :deep(.el-button--primary) {
  min-height: 54px;
  border-color: #f5b620 !important;
  background: linear-gradient(180deg, #ffc53d 0%, #f5a900 100%) !important;
}

@keyframes typing {
  0%, 80%, 100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

@media (max-width: 980px) {
  .consult-shell,
  .feature-grid,
  .business-grid {
    grid-template-columns: 1fr;
  }

  .chat-panel {
    min-height: 560px;
  }
}

@media (max-width: 640px) {
  .ai-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .chat-input {
    grid-template-columns: 1fr;
  }

  .message-bubble {
    max-width: 84%;
  }
}
</style>
