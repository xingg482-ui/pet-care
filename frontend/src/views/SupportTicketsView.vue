<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Close, RefreshRight, Search } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { fetchSupportTicket, fetchSupportTickets, replySupportTicket, updateSupportTicketStatus } from '../api/supportTickets'

const loading = ref(false)
const replying = ref(false)
const conversations = ref([])
const current = ref(null)
const replyContent = ref('')
const messageListRef = ref()
const query = reactive({
  status: '',
  page: 1,
  pageSize: 50,
})

const statusLabels = {
  OPEN: '待回复',
  REPLIED: '已回复',
  CLOSED: '已关闭',
}

const statusTypes = {
  OPEN: 'warning',
  REPLIED: 'success',
  CLOSED: 'info',
}

const sortedConversations = computed(() => conversations.value)
const currentMessages = computed(() => {
  if (!current.value) return []
  return [
    {
      id: `ticket-${current.value.id}`,
      sender: 'customer',
      content: current.value.content,
      createdAt: current.value.createdAt,
    },
    ...(current.value.replies || []).map((reply) => ({
      id: `reply-${reply.id}`,
      sender: reply.replierRole === 'CUSTOMER' ? 'customer' : 'staff',
      content: reply.content,
      createdAt: reply.createdAt,
    })),
  ]
})

async function loadConversations() {
  loading.value = true
  try {
    const result = await fetchSupportTickets(query)
    conversations.value = result.records
    if (!current.value && conversations.value.length) {
      await selectConversation(conversations.value[0])
    }
  } finally {
    loading.value = false
  }
}

async function selectConversation(row) {
  current.value = await fetchSupportTicket(row.id)
  replyContent.value = ''
  await scrollMessagesToBottom()
}

async function submitReply() {
  if (!current.value || !replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    current.value = await replySupportTicket(current.value.id, replyContent.value)
    replyContent.value = ''
    ElMessage.success('已发送')
    await scrollMessagesToBottom()
    loadConversations()
  } finally {
    replying.value = false
  }
}

async function toggleClosed() {
  if (!current.value) return
  const nextStatus = current.value.status === 'CLOSED' ? 'OPEN' : 'CLOSED'
  current.value = await updateSupportTicketStatus(current.value.id, nextStatus)
  ElMessage.success(nextStatus === 'CLOSED' ? '会话已关闭' : '会话已重新打开')
  loadConversations()
}

async function scrollMessagesToBottom() {
  await nextTick()
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

function handleReplyKeydown(event) {
  if (event.shiftKey) {
    return
  }
  event.preventDefault()
  submitReply()
}

watch(() => currentMessages.value.length, scrollMessagesToBottom)

onMounted(loadConversations)
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h1 class="page-title">客服消息</h1>
        <p class="muted">客户登录前发起咨询，管理员在这里直接回复</p>
      </div>
    </div>

    <section class="wechat-shell">
      <aside class="conversation-list" v-loading="loading">
        <div class="conversation-toolbar">
          <div class="search-box">
            <el-icon><Search /></el-icon>
            <span>客服会话</span>
          </div>
          <el-button :icon="RefreshRight" circle @click="loadConversations" />
        </div>
        <div class="conversation-tabs">
          <button :class="{ active: query.status === '' }" type="button" @click="query.status = ''; loadConversations()">全部</button>
          <button :class="{ active: query.status === 'OPEN' }" type="button" @click="query.status = 'OPEN'; loadConversations()">待回复</button>
          <button :class="{ active: query.status === 'REPLIED' }" type="button" @click="query.status = 'REPLIED'; loadConversations()">已回复</button>
        </div>
        <button
          v-for="item in sortedConversations"
          :key="item.id"
          class="conversation-item"
          :class="{ active: current?.id === item.id }"
          type="button"
          @click="selectConversation(item)"
        >
          <span class="conversation-avatar">{{ item.contactName?.slice(0, 1) || '客' }}</span>
          <span class="conversation-main">
            <span class="conversation-title">
              <strong>{{ item.contactName }}</strong>
              <time>{{ item.updatedAt || item.createdAt }}</time>
            </span>
            <em>{{ item.content }}</em>
          </span>
          <span class="status-dot" :class="item.status">{{ statusLabels[item.status] }}</span>
        </button>
        <el-empty v-if="!sortedConversations.length && !loading" description="暂无会话" />
      </aside>

      <main class="chat-panel" v-if="current">
        <header class="chat-header">
          <div>
            <strong>{{ current.contactName }}</strong>
          </div>
          <el-button :icon="current.status === 'CLOSED' ? RefreshRight : Close" circle @click="toggleClosed" />
        </header>

        <div ref="messageListRef" class="message-list">
          <div class="chat-date">{{ current.createdAt }}</div>
          <div v-for="message in currentMessages" :key="message.id" class="message-row" :class="message.sender">
            <span v-if="message.sender === 'customer'" class="message-avatar">{{ current.contactName?.slice(0, 1) || '客' }}</span>
            <div class="bubble-wrap">
              <p>{{ message.content }}</p>
              <time>{{ message.createdAt }}</time>
            </div>
            <span v-if="message.sender === 'staff'" class="message-avatar staff-avatar">我</span>
          </div>
        </div>

        <footer class="chat-input">
          <div class="input-tools">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ current.status === 'CLOSED' ? '会话已关闭' : 'Enter 发送，Shift + Enter 换行' }}</span>
          </div>
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="4"
            maxlength="500"
            resize="none"
            placeholder="输入消息"
            :disabled="current.status === 'CLOSED'"
            @keydown.enter="handleReplyKeydown"
          />
          <div class="input-actions">
            <el-button type="primary" :loading="replying" :disabled="current.status === 'CLOSED'" @click="submitReply">发送</el-button>
          </div>
        </footer>
      </main>

      <main v-else class="chat-empty">
        <el-empty description="请选择一个会话" />
      </main>
    </section>
  </AppLayout>
</template>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header p { margin: 6px 0 0; }
.wechat-shell {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  height: calc(100vh - 190px);
  min-height: 640px;
  overflow: hidden;
  border: 1px solid var(--pc-border);
  border-radius: 8px;
  background: #f5f5f5;
}
.conversation-list {
  border-right: 1px solid var(--pc-border);
  background: #f7f7f7;
  overflow-y: auto;
}
.conversation-toolbar {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #ededed;
}
.search-box {
  height: 34px;
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border-radius: 4px;
  background: #ffffff;
  color: #667085;
}
.conversation-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  padding: 0 12px 10px;
  background: #ededed;
}
.conversation-tabs button {
  height: 28px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #606266;
  cursor: pointer;
}
.conversation-tabs button.active {
  background: #ffffff;
  color: #111827;
  font-weight: 700;
}
.conversation-item {
  width: 100%;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 0;
  border-bottom: 1px solid #e9e9e9;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.conversation-item.active,
.conversation-item:hover {
  background: #e5e5e5;
}
.conversation-avatar,
.message-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f59e0b;
  color: #fff;
  font-weight: 700;
}
.conversation-avatar { width: 44px; height: 44px; }
.conversation-main { min-width: 0; }
.conversation-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.conversation-title strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conversation-title time {
  flex: 0 0 auto;
  color: #98a2b3;
  font-size: 11px;
}
.conversation-main em {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conversation-main em {
  margin-top: 4px;
  color: var(--pc-muted);
  font-style: normal;
  font-size: 12px;
}
.status-dot {
  display: inline-flex;
  grid-column: 2;
  align-items: center;
  width: fit-content;
  margin-top: 6px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #eef4ff;
  color: #175cd3;
  font-size: 11px;
  line-height: 1.4;
  white-space: nowrap;
}
.status-dot.OPEN {
  background: #fff7e6;
  color: #b54708;
}
.status-dot.REPLIED {
  background: #ecfdf3;
  color: #027a48;
}
.status-dot.CLOSED {
  background: #f2f4f7;
  color: #667085;
}
.chat-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
}
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid var(--pc-border);
  background: #f5f5f5;
}
.message-list {
  overflow-y: auto;
  padding: 22px 26px;
  background: #f5f5f5;
}
.chat-date {
  width: fit-content;
  margin: 0 auto 20px;
  padding: 3px 8px;
  border-radius: 4px;
  background: #dadde1;
  color: #ffffff;
  font-size: 12px;
}
.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.message-row.staff {
  justify-content: flex-end;
}
.message-avatar {
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
}
.staff-avatar {
  background: #d97706;
}
.bubble-wrap {
  max-width: min(62%, 620px);
}
.bubble-wrap p {
  margin: 0;
  padding: 10px 12px;
  border-radius: 4px;
  background: #ffffff;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 1px 1px rgba(16, 24, 40, 0.04);
}
.message-row.staff .bubble-wrap p {
  background: #95ec69;
}
.bubble-wrap time {
  display: block;
  margin-top: 6px;
  color: var(--pc-muted);
  font-size: 12px;
}
.message-row.staff .bubble-wrap time {
  text-align: right;
}
.chat-input {
  padding: 10px 16px 14px;
  border-top: 1px solid var(--pc-border);
  background: #f5f5f5;
}
.input-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #667085;
  font-size: 12px;
}
.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
.input-actions .el-button {
  min-width: 92px;
  background: #07c160;
  border-color: #07c160;
}
.chat-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}
@media (max-width: 900px) {
  .wechat-shell {
    grid-template-columns: 1fr;
    height: auto;
  }
  .conversation-list {
    max-height: 260px;
    border-right: 0;
    border-bottom: 1px solid var(--pc-border);
  }
  .chat-panel {
    min-height: 560px;
  }
  .bubble-wrap {
    max-width: 76%;
  }
}
</style>
