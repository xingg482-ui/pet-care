<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Bell,
  Calendar,
  CircleCheck,
  ChatDotRound,
  Collection,
  DocumentChecked,
  House,
  InfoFilled,
  Right,
  MagicStick,
  Shop,
  User,
  Wallet,
} from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { fetchMyBoardingCareUpdates, fetchMyBoardingOrders, payMyBoardingOrder } from '../api/boarding'
import { fetchMyOrders, payMyOrder } from '../api/orders'
import { fetchMyPets } from '../api/pets'

const router = useRouter()
const loading = ref(false)
const payingId = ref('')
const pets = ref([])
const serviceOrders = ref([])
const boardingOrders = ref([])
const careUpdates = ref([])
const customerAccountName = computed(() => localStorage.getItem('petCareDisplayName') || localStorage.getItem('petCareUsername') || '客户')

const shortcuts = [
  { title: '我的宠物', subtitle: '档案与头像', path: '/my-pets', icon: Collection, tone: 'green' },
  { title: '订单管理', subtitle: '服务预约', path: '/my-orders', icon: DocumentChecked, tone: 'blue' },
  { title: '宠物托管', subtitle: '房态预约', path: '/my-boarding', icon: Shop, tone: 'amber' },
  { title: '联系客服', subtitle: '咨询与反馈', path: '/my-support', icon: ChatDotRound, tone: 'rose' },
  { title: '我的资料', subtitle: '账号信息', path: '/profile', icon: User, tone: 'slate' },
]

const serviceStatusLabels = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  CANCELLED: '已取消',
  IN_SERVICE: '服务中',
  COMPLETED: '已完成',
  REJECTED: '已拒绝',
}

const boardingStatusLabels = {
  RESERVED: '待入住',
  CHECKED_IN: '已入住',
  CANCELLED: '已取消',
  COMPLETED: '已接回',
}

const activeServiceStatuses = ['PENDING', 'CONFIRMED', 'IN_SERVICE']
const activeBoardingStatuses = ['RESERVED', 'CHECKED_IN']

const summary = computed(() => ({
  petCount: pets.value.length,
  pendingOrderCount: orderAlerts.value.length,
  activeBoardingCount: activeBoardingOrders.value.length,
  pendingCareTaskCount: careUpdates.value.reduce((sum, item) => sum + Number(item.pendingTaskCount || 0), 0),
}))

const orderAlerts = computed(() => {
  const serviceAlerts = serviceOrders.value
    .map((order) => ({
      id: `service-${order.id}`,
      orderId: order.id,
      orderType: 'SERVICE',
      orderNo: order.orderNo,
      petName: order.petName,
      title: serviceNames(order),
      appointmentTime: order.appointmentTime,
      status: order.status,
      statusText: serviceStatusLabels[order.status] || order.status,
      paymentStatus: order.paymentStatus || 'UNPAID',
      totalAmount: order.totalAmount,
      alertType: serviceAlertType(order),
      route: '/my-orders',
    }))
    .filter((order) => order.alertType)

  const boardingAlerts = boardingOrders.value
    .filter((order) => activeBoardingStatuses.includes(order.status) || isUnpaidCompleted(order))
    .map((order) => ({
      id: `boarding-${order.id}`,
      orderId: order.id,
      orderType: 'BOARDING',
      orderNo: order.boardingNo,
      petName: order.petName,
      title: `宠物托管${order.chargeDays ? ` ${order.chargeDays} 天` : ''}`,
      appointmentTime: order.plannedCheckInTime,
      status: order.status,
      statusText: boardingStatusLabels[order.status] || order.status,
      paymentStatus: order.paymentStatus || 'UNPAID',
      totalAmount: order.totalAmount,
      alertType: boardingAlertType(order),
      route: '/my-boarding',
    }))
    .filter((order) => order.alertType)

  return [...serviceAlerts, ...boardingAlerts]
    .sort((left, right) => alertSortValue(left) - alertSortValue(right))
    .slice(0, 5)
})

const activeBoardingOrders = computed(() => boardingOrders.value
  .filter((order) => activeBoardingStatuses.includes(order.status))
  .sort((a, b) => boardingSortValue(a) - boardingSortValue(b))
  .slice(0, 4))

const careUpdateMap = computed(() => careUpdates.value.reduce((map, item) => {
  map[item.boardingOrderId] = item
  return map
}, {}))

function serviceNames(order) {
  return order.serviceNames?.length ? order.serviceNames.join(' / ') : '门店服务'
}

function parseDateTime(value) {
  if (!value) return null
  const date = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

function formatDateTime(value) {
  return String(value || '').slice(0, 16)
}

function todayText() {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function money(value) {
  return `￥${Number(value || 0).toFixed(2)}`
}

function serviceAlertType(order) {
  if (isUnpaidCompleted(order)) {
    return 'UNPAID'
  }
  if (activeServiceStatuses.includes(order.status)) {
    return parseDateTime(order.appointmentTime) > new Date() ? 'UPCOMING' : 'UNFINISHED'
  }
  return ''
}

function isUnpaidCompleted(order) {
  return order.status === 'COMPLETED' && (order.paymentStatus || 'UNPAID') !== 'PAID'
}

function boardingAlertType(order) {
  if (isUnpaidCompleted(order)) return 'UNPAID'
  if (order.status === 'RESERVED') return 'UPCOMING'
  if (order.status === 'CHECKED_IN') return 'UNFINISHED'
  return ''
}

function diffParts(targetValue) {
  const target = parseDateTime(targetValue)
  if (!target) return null
  const minutes = Math.ceil((target.getTime() - Date.now()) / 60000)
  const absoluteMinutes = Math.abs(minutes)
  return {
    minutes,
    hours: Math.ceil(absoluteMinutes / 60),
    days: Math.ceil(absoluteMinutes / 1440),
  }
}

function appointmentText(order) {
  if (order.alertType === 'UNPAID') {
    return order.orderType === 'BOARDING' ? '已接回，待支付' : '已完成，待支付'
  }
  if (order.alertType === 'UNFINISHED') {
    return order.orderType === 'BOARDING' ? '已入住' : '进行中'
  }
  const diff = diffParts(order.appointmentTime)
  if (!diff) return '待赴约'
  if (diff.minutes <= 0) return '预约时间已到'
  if (diff.minutes <= 1440) return `还有 ${diff.hours} 小时赴约`
  return `还有 ${diff.days} 天赴约`
}

function pickupText(order) {
  if (order.status === 'RESERVED') {
    const diff = diffParts(order.plannedCheckInTime)
    if (!diff) return '待入住'
    if (diff.minutes <= 0) return '已到计划入住时间'
    if (diff.minutes <= 1440) return `还有 ${diff.hours} 小时入住`
    return `还有 ${diff.days} 天入住`
  }
  const diff = diffParts(order.plannedCheckOutTime)
  if (!diff) return '待接回'
  if (diff.minutes <= 0) return '已到计划接回时间'
  if (diff.minutes <= 1440) return `还有 ${diff.hours} 小时接回`
  return `还有 ${diff.days} 天接回`
}

function alertSortValue(order) {
  const priority = { UNPAID: 0, UNFINISHED: 1, UPCOMING: 2 }[order.alertType] ?? 9
  const date = parseDateTime(order.appointmentTime)?.getTime() || Number.MAX_SAFE_INTEGER
  return priority * 100000000000000 + date
}

function boardingSortValue(order) {
  const priority = order.status === 'CHECKED_IN' ? 0 : 1
  const date = parseDateTime(order.plannedCheckOutTime)?.getTime() || parseDateTime(order.plannedCheckInTime)?.getTime() || Number.MAX_SAFE_INTEGER
  return priority * 100000000000000 + date
}

function alertTagType(order) {
  if (order.alertType === 'UNPAID') return 'warning'
  if (order.alertType === 'UNFINISHED') return 'primary'
  return 'success'
}

function boardingTagType(status) {
  return status === 'CHECKED_IN' ? 'primary' : 'warning'
}

function careProgressText(order) {
  const update = careUpdateMap.value[order.id]
  if (!update) return '今日暂无照护任务'
  return `${update.completedTaskCount || 0}/${update.totalTaskCount || 0} 已完成`
}

function careProgressPercent(order) {
  const update = careUpdateMap.value[order.id]
  const total = Number(update?.totalTaskCount || 0)
  if (!total) return 0
  return Math.round((Number(update.completedTaskCount || 0) / total) * 100)
}

function careTasks(order) {
  return careUpdateMap.value[order.id]?.tasks || []
}

function taskStatusText(status) {
  return status === 'DONE' ? '已完成' : '待处理'
}

function go(path) {
  router.push(path)
}

async function payAlert(order) {
  payingId.value = order.id
  try {
    if (order.orderType === 'BOARDING') {
      await payMyBoardingOrder(order.orderId, { paymentMethod: 'MOCK' })
    } else {
      await payMyOrder(order.orderId, { paymentMethod: 'MOCK' })
    }
    ElMessage.success('支付成功')
    await loadHome()
  } finally {
    payingId.value = ''
  }
}

async function loadHome() {
  loading.value = true
  try {
    const [petResult, orderResult, boardingResult, careResult] = await Promise.all([
      fetchMyPets(),
      fetchMyOrders({ page: 1, pageSize: 50 }),
      fetchMyBoardingOrders({ page: 1, pageSize: 50 }),
      fetchMyBoardingCareUpdates({ date: todayText() }),
    ])
    pets.value = petResult.records || []
    serviceOrders.value = orderResult.records || []
    boardingOrders.value = boardingResult.records || []
    careUpdates.value = careResult || []
  } finally {
    loading.value = false
  }
}

onMounted(loadHome)
</script>

<template>
  <AppLayout>
    <div class="home-page" v-loading="loading">
      <section class="home-hero">
        <div>
          <p class="customer-greeting">亲爱的{{ customerAccountName }}</p>
          <h1>欢迎回来，今天的安排都在这里</h1>
          <p class="hero-subtitle">
            {{ summary.petCount }} 只宠物 · {{ summary.pendingOrderCount }} 个待关注订单 · {{ summary.activeBoardingCount }} 个托管安排 · {{ summary.pendingCareTaskCount }} 个照护待处理
          </p>
        </div>
        <el-button :icon="Calendar" type="primary" @click="go('/my-orders')">预约服务</el-button>
      </section>

      <section class="shortcut-grid" aria-label="快捷入口">
        <button v-for="item in shortcuts" :key="item.path" :class="['shortcut-card', `shortcut-card--${item.tone}`]" type="button" @click="go(item.path)">
          <span class="shortcut-icon"><el-icon><component :is="item.icon" /></el-icon></span>
          <span class="shortcut-text">
            <strong>{{ item.title }}</strong>
            <small>{{ item.subtitle }}</small>
          </span>
          <el-icon class="shortcut-arrow"><Right /></el-icon>
        </button>
      </section>

      <section class="ai-tip-card">
        <div class="ai-tip-visual">
          <span class="ai-orbit ai-orbit-one"></span>
          <span class="ai-orbit ai-orbit-two"></span>
          <el-icon><MagicStick /></el-icon>
        </div>
        <div class="ai-tip-content">
          <h2>AI 养宠顾问</h2>
          <p>服务流程、订单状态、托管照护、疫苗驱虫问题都可以先问我。</p>
        </div>
        <el-button
          type="warning"
          plain
          @click="router.push({ path: '/my-ai-consult', query: { q: '我可以咨询哪些养宠和服务问题？', sourcePage: 'MY_HOME', contextType: 'CUSTOMER_HOME' } })"
        >
          立即咨询
        </el-button>
      </section>

      <section class="dashboard-row">
        <div class="section-heading">
          <div>
            <h2>订单提醒</h2>
            <p>未完成、待支付和即将赴约的订单</p>
          </div>
          <el-button text :icon="Right" @click="go('/my-orders')">全部订单</el-button>
        </div>
        <div v-if="orderAlerts.length" class="order-grid">
          <article v-for="order in orderAlerts" :key="order.id" class="order-card" @click="go(order.route)">
            <div class="card-topline">
              <el-tag :type="alertTagType(order)" effect="light">{{ appointmentText(order) }}</el-tag>
              <span>{{ order.orderNo }}</span>
            </div>
            <h3>{{ order.petName || '我的宠物' }}</h3>
            <p>{{ order.title }}</p>
            <div class="order-meta">
              <span><el-icon><Calendar /></el-icon>{{ formatDateTime(order.appointmentTime) || '时间待确认' }}</span>
              <span><el-icon><Wallet /></el-icon>{{ money(order.totalAmount) }}</span>
            </div>
            <div class="card-footer">
              <span>{{ order.statusText }}</span>
              <el-button
                v-if="order.alertType === 'UNPAID'"
                link
                type="primary"
                :loading="payingId === order.id"
                @click.stop="payAlert(order)"
              >
                去支付
              </el-button>
              <strong v-else>查看详情</strong>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无待处理订单">
          <el-button type="primary" @click="go('/my-orders')">去预约服务</el-button>
        </el-empty>
      </section>

      <section class="dashboard-row">
        <div class="section-heading">
          <div>
            <h2>宠物托管状态</h2>
            <p>待入住、托管中和接回提醒</p>
          </div>
          <el-button text :icon="Right" @click="go('/my-boarding')">查看托管</el-button>
        </div>
        <div v-if="activeBoardingOrders.length" class="boarding-grid">
          <article v-for="order in activeBoardingOrders" :key="order.id" class="boarding-card">
            <div class="boarding-main">
              <span class="boarding-icon"><el-icon><Shop /></el-icon></span>
              <div>
                <div class="card-topline">
                  <el-tag :type="boardingTagType(order.status)" effect="light">{{ boardingStatusLabels[order.status] || order.status }}</el-tag>
                  <span>{{ order.boardingNo }}</span>
                </div>
                <h3>{{ order.petName || '我的宠物' }}</h3>
                <p>{{ order.areaName || '-' }} · {{ order.locationCode || order.locationName || '-' }}</p>
              </div>
            </div>
            <div class="pickup-box">
              <strong>{{ pickupText(order) }}</strong>
              <span>{{ order.status === 'CHECKED_IN' ? `计划 ${formatDateTime(order.plannedCheckOutTime)} 接回` : `计划 ${formatDateTime(order.plannedCheckInTime)} 入住` }}</span>
            </div>
            <div class="care-preview">
              <div>
                <span>今日照护任务</span>
                <strong>{{ careProgressText(order) }}</strong>
              </div>
              <el-progress class="care-progress" :percentage="careProgressPercent(order)" :show-text="false" />
            </div>
            <div v-if="careTasks(order).length" class="task-timeline">
              <div v-for="task in careTasks(order)" :key="task.id" :class="['task-line', { done: task.status === 'DONE' }]">
                <span class="task-dot"><el-icon v-if="task.status === 'DONE'"><CircleCheck /></el-icon></span>
                <span class="task-time">{{ task.taskTime }}</span>
                <strong>{{ task.taskName }}</strong>
                <small>{{ taskStatusText(task.status) }}</small>
              </div>
            </div>
            <el-button v-else class="care-action" :icon="InfoFilled" @click="go('/my-boarding')">查看托管详情</el-button>
          </article>
        </div>
        <el-empty v-else description="当前没有托管中的宠物">
          <el-button type="primary" :icon="House" @click="go('/my-boarding')">查看房态</el-button>
        </el-empty>
      </section>
    </div>
  </AppLayout>
</template>

<style scoped>
.home-page {
  min-height: calc(100vh - 132px);
}

.home-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
  padding: 22px 24px;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #ffffff;
}

.home-hero h1 {
  margin: 0;
  color: #111827;
  font-size: 24px;
  line-height: 1.25;
  letter-spacing: 0;
}

.customer-greeting {
  margin: 0 0 8px;
  color: #f59e0b;
  font-size: 17px;
  line-height: 24px;
  font-weight: 800;
}

.hero-subtitle {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.ai-tip-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  margin-bottom: 18px;
  padding: 18px 22px;
  border: 1px solid #f8d99c;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(255, 250, 240, 0.98) 0%, rgba(255, 255, 255, 0.96) 58%, rgba(239, 246, 255, 0.9) 100%);
}

.ai-tip-visual {
  position: relative;
  width: 74px;
  height: 58px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #fff7df;
  color: #f59e0b;
  font-size: 30px;
  overflow: hidden;
}

.ai-orbit {
  position: absolute;
  display: block;
  border: 1px solid rgba(245, 158, 11, 0.24);
  border-radius: 50%;
}

.ai-orbit-one {
  width: 52px;
  height: 52px;
}

.ai-orbit-two {
  width: 86px;
  height: 34px;
  transform: rotate(-18deg);
}

.ai-tip-visual .el-icon {
  position: relative;
  z-index: 1;
}

.ai-tip-content h2 {
  margin: 0;
  color: #111827;
  font-size: 20px;
  letter-spacing: 0;
}

.ai-tip-content p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
}

.shortcut-card {
  min-height: 96px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #ffffff;
  color: #111827;
  text-align: left;
  cursor: pointer;
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 150ms ease;
}

.shortcut-card:hover {
  border-color: #f6c453;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.shortcut-icon {
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 22px;
}

.shortcut-card--green .shortcut-icon { background: #ecfdf5; color: #059669; }
.shortcut-card--blue .shortcut-icon { background: #eff6ff; color: #2563eb; }
.shortcut-card--amber .shortcut-icon { background: #fffbeb; color: #d97706; }
.shortcut-card--rose .shortcut-icon { background: #fff1f2; color: #e11d48; }
.shortcut-card--slate .shortcut-icon { background: #f1f5f9; color: #475569; }

.shortcut-text {
  min-width: 0;
  flex: 1;
  display: grid;
  gap: 4px;
}

.shortcut-text strong {
  font-size: 16px;
  line-height: 1.2;
}

.shortcut-text small {
  color: #64748b;
  font-size: 13px;
}

.shortcut-arrow {
  color: #94a3b8;
}

.dashboard-row {
  margin-bottom: 18px;
  padding: 20px;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #ffffff;
}

.section-heading,
.card-topline,
.card-footer,
.order-meta,
.boarding-main,
.care-preview {
  display: flex;
  align-items: center;
}

.section-heading {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.section-heading h2 {
  margin: 0;
  color: #111827;
  font-size: 18px;
  letter-spacing: 0;
}

.section-heading p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.order-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.order-card,
.boarding-card {
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
}

.order-card {
  padding: 16px;
  cursor: pointer;
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 150ms ease;
}

.order-card:hover {
  border-color: #bfdbfe;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.08);
  transform: translateY(-1px);
}

.card-topline {
  justify-content: space-between;
  gap: 12px;
  color: #94a3b8;
  font-size: 12px;
}

.order-card h3,
.boarding-card h3 {
  margin: 14px 0 6px;
  color: #111827;
  font-size: 18px;
  letter-spacing: 0;
}

.order-card p,
.boarding-card p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.order-meta {
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
  color: #475569;
  font-size: 13px;
}

.order-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.card-footer {
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #edf2f7;
  color: #64748b;
  font-size: 13px;
}

.card-footer strong {
  color: #2563eb;
}

.boarding-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.boarding-card {
  padding: 18px;
}

.boarding-main {
  gap: 14px;
}

.boarding-icon {
  width: 52px;
  height: 52px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #fff7ed;
  color: #ea580c;
  font-size: 25px;
}

.pickup-box {
  margin-top: 16px;
  padding: 14px;
  border-radius: 8px;
  background: #f8fafc;
}

.pickup-box strong,
.pickup-box span {
  display: block;
}

.pickup-box strong {
  color: #111827;
  font-size: 18px;
}

.pickup-box span {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.care-preview {
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
}

.care-progress {
  width: 132px;
  flex: 0 0 auto;
}

.care-preview span,
.care-preview strong {
  display: block;
}

.care-preview span {
  color: #64748b;
  font-size: 12px;
}

.care-preview strong {
  margin-top: 3px;
  color: #111827;
  font-size: 14px;
}

.task-timeline {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.task-line {
  display: grid;
  grid-template-columns: 18px 44px minmax(0, 1fr) 52px;
  align-items: center;
  gap: 8px;
  min-height: 30px;
  padding: 6px 8px;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
}

.task-line.done {
  background: #f0fdf4;
  color: #166534;
}

.task-dot {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #cbd5e1;
  border-radius: 50%;
  background: #ffffff;
  color: #16a34a;
}

.task-line.done .task-dot {
  border-color: #86efac;
}

.task-line strong {
  min-width: 0;
  color: inherit;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-line small {
  color: inherit;
  text-align: right;
}

.care-action {
  width: 100%;
  margin-top: 14px;
}

@media (max-width: 1180px) {
  .shortcut-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .order-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .home-hero,
  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .shortcut-grid,
  .order-grid,
  .boarding-grid {
    grid-template-columns: 1fr;
  }

  .ai-tip-card {
    grid-template-columns: 1fr;
    justify-items: start;
  }
}
</style>
