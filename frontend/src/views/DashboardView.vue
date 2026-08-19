<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Checked, Collection, Money, Plus, Refresh, Search, Shop, TrendCharts, User } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import {
  fetchDashboardRevenueTrend,
  fetchDashboardServiceRevenue,
  fetchDashboardSummary,
  fetchDashboardTodayTasks,
} from '../api/dashboard'
import { confirmOrderPayment, updateOrderStatus } from '../api/orders'
import {
  checkInBoardingOrder,
  completeBoardingCareTask,
  confirmBoardingOrderPayment,
  confirmBoardingOrderPickedUp,
} from '../api/boarding'

const router = useRouter()
const loading = ref(false)
const trendChartRef = ref()
const serviceChartRef = ref()
const charts = []

const summary = ref({
  customerCount: 0,
  petCount: 0,
  serviceItemCount: 0,
  pendingOrderCount: 0,
  todayAppointmentCount: 0,
  boardingAvailableLocationCount: 0,
})
const revenueTrend = ref([])
const serviceRevenue = ref([])
const todayActiveOrders = ref([])
const appointmentFilter = ref('ALL')
const searchKeyword = ref('')
const lastUpdatedAt = ref('')

const statusText = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  REJECTED: '已拒绝',
  CANCELLED: '已取消',
  IN_SERVICE: '服务中',
  COMPLETED: '已完成',
  RESERVED: '待入住',
  CHECKED_IN: '已入住',
}

const paymentStatusText = {
  UNPAID: '未支付',
  PAID: '已支付',
}

const statusClass = {
  PENDING: 'status-pending',
  CONFIRMED: 'status-confirmed',
  IN_SERVICE: 'status-service',
  RESERVED: 'status-pending',
  CHECKED_IN: 'status-service',
  COMPLETED: 'status-done',
  CANCELLED: 'status-muted',
  REJECTED: 'status-muted',
  UNPAID: 'status-unpaid',
}

const todayRevenue = computed(() => {
  const last = revenueTrend.value[revenueTrend.value.length - 1]
  return Number(last?.revenue || 0)
})

const dashboardCards = computed(() => [
  { label: '寄养空位', value: summary.value.boardingAvailableLocationCount, icon: Shop, tone: 'orange', path: '/boarding' },
  { label: '待确认订单', value: summary.value.pendingOrderCount, icon: Checked, tone: 'deep-orange', path: '/orders' },
  { label: '今日营收', value: money(todayRevenue.value), icon: Money, tone: 'green', path: '/finance' },
  { label: '客户总数', value: summary.value.customerCount, icon: User, tone: 'blue', path: '/customers' },
  { label: '宠物总数', value: summary.value.petCount, icon: Collection, tone: 'purple', path: '/pets' },
])

const filterTabs = computed(() => [
  { label: '全部', value: 'ALL', count: todayActiveOrders.value.length },
  { label: '服务预约', value: 'SERVICE_ORDER', count: todayActiveOrders.value.filter((item) => item.taskType === 'SERVICE_ORDER').length },
  { label: '托管服务', value: 'BOARDING_SERVICE', count: todayActiveOrders.value.filter(isBoardingServiceTask).length },
  { label: '收款管理', value: 'PAYMENT', count: todayActiveOrders.value.filter(isPaymentTask).length },
  { label: '照护任务', value: 'BOARDING_CARE', count: todayActiveOrders.value.filter((item) => item.taskType === 'BOARDING_CARE').length },
])

const displayedOrders = computed(() => todayActiveOrders.value.filter((item) => {
  const matchStatus = appointmentFilter.value === 'ALL'
    || item.taskType === appointmentFilter.value
    || (appointmentFilter.value === 'BOARDING_SERVICE' && isBoardingServiceTask(item))
    || (appointmentFilter.value === 'PAYMENT' && isPaymentTask(item))
  const keyword = searchKeyword.value.trim().toLowerCase()
  const matchKeyword = !keyword || [item.customerName, item.petName, item.subject].some((value) => String(value || '').toLowerCase().includes(keyword))
  return matchStatus && matchKeyword
}))

const serviceRevenueTotal = computed(() => serviceRevenue.value.reduce((sum, item) => sum + Number(item.value || 0), 0))

const serviceRevenueRows = computed(() => serviceRevenue.value.map((item, index) => {
  const value = Number(item.value || 0)
  return {
    ...item,
    value,
    rate: serviceRevenueTotal.value ? ((value / serviceRevenueTotal.value) * 100).toFixed(1) : '0.0',
    colorClass: `revenue-dot-${index % 3}`,
  }
}))

function money(value) {
  return `￥${Number(value || 0).toFixed(2)}`
}

function formatDateTime(value) {
  const date = value instanceof Date ? value : new Date(value)
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function taskDateTime(value) {
  const [date = '-', time = ''] = String(value || '').split(' ')
  return {
    date,
    time: time || '-',
  }
}

function isBoardingServiceTask(item) {
  return item.taskType === 'BOARDING_CHECK_IN' || item.taskType === 'BOARDING_PICK_UP'
}

function isPaymentTask(item) {
  return item.taskType === 'SERVICE_PAYMENT' || item.taskType === 'BOARDING_PAYMENT'
}

function isUnpaidTask(item) {
  return (item.paymentStatus || 'UNPAID') === 'UNPAID'
}

async function loadDashboard() {
  loading.value = true
  try {
    const [summaryResult, trendResult, serviceResult, activeOrderResult] = await Promise.all([
      fetchDashboardSummary(),
      fetchDashboardRevenueTrend(),
      fetchDashboardServiceRevenue(),
      fetchDashboardTodayTasks(),
    ])
    summary.value = summaryResult
    revenueTrend.value = trendResult
    serviceRevenue.value = serviceResult
    todayActiveOrders.value = activeOrderResult
    lastUpdatedAt.value = formatDateTime(new Date())
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function chartOf(elRef) {
  const chart = echarts.getInstanceByDom(elRef.value) || echarts.init(elRef.value)
  if (!charts.includes(chart)) {
    charts.push(chart)
  }
  return chart
}

function renderCharts() {
  renderTrendChart()
  renderServiceChart()
}

function emptyTitle(text) {
  return { text, left: 'center', top: 'middle', textStyle: { color: '#9ca3af', fontSize: 14, fontWeight: 400 } }
}

function renderTrendChart() {
  const chart = chartOf(trendChartRef)
  const hasData = revenueTrend.value.some((item) => Number(item.revenue || 0) || Number(item.cost || 0) || Number(item.profit || 0))
  chart.setOption({
    color: ['#f6b400', '#4f97e8', '#39b77a'],
    title: hasData ? undefined : emptyTitle('暂无营收趋势数据'),
    tooltip: { trigger: 'axis', backgroundColor: '#ffffff', borderColor: '#edf0f5', textStyle: { color: '#111827' } },
    legend: { top: 0, data: ['营收', '成本', '利润'], textStyle: { color: '#52606f' } },
    grid: { left: 42, right: 24, top: 54, bottom: 34 },
    xAxis: { type: 'category', data: revenueTrend.value.map((item) => item.date.slice(5)), axisLine: { lineStyle: { color: '#d9dee8' } }, axisTick: { show: false }, axisLabel: { color: '#8a94a6' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef1f6' } }, axisLabel: { color: '#8a94a6' } },
    series: [
      { name: '营收', type: 'line', smooth: true, symbol: 'circle', symbolSize: 7, data: revenueTrend.value.map((item) => Number(item.revenue || 0)), label: { show: true, color: '#111827', fontSize: 11 }, lineStyle: { width: 2 }, areaStyle: { color: 'rgba(246, 180, 0, 0.08)' } },
      { name: '成本', type: 'line', smooth: true, symbol: 'circle', symbolSize: 7, data: revenueTrend.value.map((item) => Number(item.cost || 0)), label: { show: true, color: '#111827', fontSize: 11 }, lineStyle: { width: 2 } },
      { name: '利润', type: 'line', smooth: true, symbol: 'circle', symbolSize: 7, data: revenueTrend.value.map((item) => Number(item.profit || 0)), label: { show: true, color: '#111827', fontSize: 11 }, lineStyle: { width: 2 }, areaStyle: { color: 'rgba(57, 183, 122, 0.08)' } },
    ],
  }, true)
}

function renderServiceChart() {
  const chart = chartOf(serviceChartRef)
  const data = serviceRevenue.value.map((item) => ({ name: item.name, value: Number(item.value || 0) }))
  chart.setOption({
    color: ['#ffdc73', '#ffd19a', '#e8d4aa', '#ffb173', '#f5c052', '#f8e3a1'],
    title: data.length ? undefined : emptyTitle('暂无服务收入数据'),
    tooltip: { trigger: 'item', formatter: '{b}<br/>收入: ￥{c} ({d}%)', backgroundColor: '#ffffff', borderColor: '#edf0f5', textStyle: { color: '#111827' } },
    legend: { show: false },
    graphic: data.length ? [{
      type: 'text',
      left: 'center',
      top: 'middle',
      style: {
        text: `总收入\n${money(serviceRevenueTotal.value)}`,
        textAlign: 'center',
        fill: '#111827',
        fontSize: 22,
        fontWeight: 700,
        lineHeight: 32,
      },
    }] : [],
    series: [{
      name: '服务收入',
      type: 'pie',
      radius: ['46%', '72%'],
      center: ['50%', '50%'],
      data,
      label: { show: false },
      labelLine: { show: false },
    }],
  }, true)
}

async function completeOrder(row) {
  if (row.taskType === 'BOARDING_CARE') {
    await ElMessageBox.confirm(`确认完成照护任务「${row.subject}」吗？`, '确认操作', { type: 'warning' })
    await completeBoardingCareTask(row.id)
    ElMessage.success('照护任务已完成')
    loadDashboard()
    return
  }
  await ElMessageBox.confirm(`确定将订单「${row.taskNo}」标记为已完成吗？`, '确认操作', { type: 'warning' })
  await updateOrderStatus(row.id, { status: 'COMPLETED', remark: '首页快捷完成' })
  ElMessage.success('订单已完成')
  loadDashboard()
}

async function startBoardingService(row) {
  await ElMessageBox.confirm(`确认开始「${row.petName}」的宠物托管服务吗？`, '开始服务', {
    type: 'warning',
    confirmButtonText: '开始服务',
  })
  await checkInBoardingOrder(row.id)
  ElMessage.success('已开始服务，客户端状态已同步为已入住')
  loadDashboard()
}

async function confirmBoardingPickedUp(row) {
  await ElMessageBox.confirm(`确认「${row.petName}」已被客户接回？确认后才能继续确认支付。`, '确认已接回', {
    type: 'warning',
    confirmButtonText: '确认已接回',
  })
  await confirmBoardingOrderPickedUp(row.id)
  ElMessage.success('已确认接回，可继续确认支付')
  loadDashboard()
}

async function confirmPayment(row) {
  await ElMessageBox.confirm(`确认订单「${row.taskNo}」已完成收款吗？`, '确认已支付', { type: 'warning' })
  if (row.taskType === 'BOARDING_PAYMENT') {
    await confirmBoardingOrderPayment(row.id, { paymentMethod: 'MANUAL' })
  } else {
    await confirmOrderPayment(row.id, { paymentMethod: 'MANUAL' })
  }
  ElMessage.success('已确认支付')
  loadDashboard()
}

function openTask(row) {
  if (row.taskType === 'BOARDING_CARE') {
    router.push({ path: '/boarding', query: { tab: 'care' } })
    return
  }
  if (row.taskType === 'BOARDING_CHECK_IN' || row.taskType === 'BOARDING_PICK_UP' || row.taskType === 'BOARDING_PAYMENT') {
    router.push('/boarding')
    return
  }
  router.push(row.targetPath)
}

function resizeCharts() {
  charts.forEach((chart) => chart.resize())
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  charts.forEach((chart) => chart.dispose())
})
</script>

<template>
  <AppLayout>
    <div class="dashboard-head">
      <div>
        <h1 class="page-title">首页</h1>
      </div>
      <div class="head-actions">
        <el-button type="primary" :icon="Plus" @click="router.push('/orders/create')">新建订单</el-button>
        <el-button :icon="Refresh" @click="loadDashboard">刷新</el-button>
        <span class="updated-dot"></span>
        <span class="updated-text">数据更新于 {{ lastUpdatedAt || '-' }}</span>
      </div>
    </div>

    <el-row v-loading="loading" :gutter="16" class="summary-row">
      <el-col v-for="card in dashboardCards" :key="card.label" :xs="24" :sm="12" :md="8" :lg="4">
        <el-card shadow="never" :class="['summary-card', card.tone, { clickable: card.path }]" @click="card.path && router.push(card.path)">
          <div class="summary-content">
            <el-icon :class="['summary-icon', card.tone]"><component :is="card.icon" /></el-icon>
            <div>
              <div class="summary-label">{{ card.label }}</div>
              <div class="summary-value">{{ card.value }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="appointment-header">
          <h2>待处理任务</h2>
          <div class="appointment-tools">
            <el-input v-model="searchKeyword" :prefix-icon="Search" placeholder="搜索客户 / 宠物 / 任务" clearable class="appointment-search" />
            <el-date-picker value="2025-05-18" type="date" placeholder="选择日期" class="appointment-date" />
            <el-button link type="warning">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
          </div>
        </div>
        <div class="appointment-tabs">
          <button
            v-for="tab in filterTabs"
            :key="tab.value"
            :class="{ active: appointmentFilter === tab.value }"
            type="button"
            @click="appointmentFilter = tab.value"
          >
            {{ tab.label }} {{ tab.count }}
          </button>
        </div>
      </template>
      <el-table v-loading="loading" :data="displayedOrders" border empty-text="暂无待处理任务">
        <el-table-column label="任务时间" min-width="170">
          <template #default="{ row }">
            <div class="task-time">
              <strong>{{ taskDateTime(row.taskTime).time }}</strong>
              <span class="task-date">{{ taskDateTime(row.taskTime).date }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="客户" min-width="150">
          <template #default="{ row }">
            <div class="name-main">{{ row.customerName }}</div>
            <div class="name-sub">{{ row.customerPhone || '' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="宠物" min-width="150">
          <template #default="{ row }">
            <div class="name-main">{{ row.petName }}</div>
            <div class="name-sub">{{ row.petBreed || '' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="任务内容" min-width="180" show-overflow-tooltip />
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag :class="['status-tag', row.taskType === 'BOARDING_CARE' ? 'status-service' : 'status-pending']">
              {{
                row.taskType === 'BOARDING_CARE'
                  ? '照护任务'
                  : (row.taskType === 'BOARDING_CHECK_IN'
                    ? '托管服务'
                    : (row.taskType === 'BOARDING_PICK_UP' ? '托管服务' : (isPaymentTask(row) ? '收款管理' : '服务预约')))
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :class="['status-tag', statusClass[row.status === 'COMPLETED' && isUnpaidTask(row) ? 'UNPAID' : row.status] || 'status-pending']">
              {{ row.taskType === 'BOARDING_CARE' ? '待处理' : (row.status === 'COMPLETED' && isUnpaidTask(row) ? paymentStatusText.UNPAID : (row.taskType === 'BOARDING_PAYMENT' ? '已接回' : (statusText[row.status] || '待处理'))) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button class="table-action primary" @click="openTask(row)">查看</el-button>
              <el-button v-if="row.taskType === 'BOARDING_CHECK_IN' && row.status === 'RESERVED'" class="table-action" @click="startBoardingService(row)">开始服务</el-button>
              <el-button v-if="row.taskType === 'BOARDING_PICK_UP'" class="table-action" @click="confirmBoardingPickedUp(row)">确认已接回</el-button>
              <el-button v-if="row.status === 'IN_SERVICE' || row.taskType === 'BOARDING_CARE'" class="table-action" @click="completeOrder(row)">完成</el-button>
              <el-button v-if="isPaymentTask(row) && row.status === 'COMPLETED' && isUnpaidTask(row)" class="table-action" @click="confirmPayment(row)">确认已支付</el-button>
              <el-button class="table-action ghost">取消</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-title">
              <span>最近 7 天营收趋势</span>
              <small>单位：元</small>
            </div>
          </template>
          <div ref="trendChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="chart-card revenue-card">
          <template #header>服务项目收入占比</template>
          <div class="revenue-split">
            <div ref="serviceChartRef" class="donut-chart"></div>
            <div class="revenue-list">
              <div v-for="row in serviceRevenueRows" :key="row.name" class="revenue-row">
                <span :class="['revenue-dot', row.colorClass]"></span>
                <span class="revenue-name">{{ row.name }}</span>
                <span class="revenue-money">{{ money(row.value) }}</span>
                <strong>{{ row.rate }}%</strong>
              </div>
              <div class="revenue-row total">
                <span>合计</span>
                <span></span>
                <span>{{ money(serviceRevenueTotal) }}</span>
                <strong>100%</strong>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </AppLayout>
</template>

<style scoped>
.dashboard-head,
.head-actions,
.summary-content,
.appointment-header,
.appointment-tools,
.appointment-tabs,
.chart-title,
.revenue-split,
.revenue-row {
  display: flex;
  align-items: center;
}

.dashboard-head {
  justify-content: space-between;
  margin-bottom: 22px;
}

.dashboard-head p {
  margin: 6px 0 0;
  color: #8a94a6;
  font-size: 14px;
}

.head-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  max-width: 560px;
}

.head-actions :deep(.el-button--primary) {
  height: 44px;
  min-width: 148px;
  border-color: #f5b620 !important;
  background: linear-gradient(180deg, #ffc53d 0%, #f5a900 100%) !important;
  box-shadow: 0 10px 24px rgba(245, 166, 35, 0.22) !important;
}

.head-actions :deep(.el-button:not(.el-button--primary)) {
  height: 44px;
  min-width: 118px;
}

.updated-dot {
  width: 8px;
  height: 8px;
  margin-left: auto;
  border-radius: 50%;
  background: #edf0f5;
}

.updated-text {
  color: #a2aaba;
  font-size: 12px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  column-gap: 24px;
  margin-bottom: 18px;
}

.summary-row :deep(.el-col) {
  display: block;
  max-width: none;
  flex: none;
  width: 100%;
}

.summary-card {
  min-height: 112px;
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 180ms ease;
}

.summary-card.clickable {
  cursor: pointer;
}

.summary-card.clickable:hover {
  border-color: #f4c04d;
  box-shadow: 0 14px 30px rgba(17, 24, 39, 0.08);
  transform: translateY(-1px);
}

.summary-card.clickable:active {
  transform: translateY(0);
}

.summary-card :deep(.el-card__body) {
  height: 100%;
  padding: 18px 24px;
  display: flex;
  align-items: center;
}

.summary-card::before {
  display: none;
}

.summary-content {
  gap: 22px;
  width: 100%;
  justify-content: center;
}

.summary-label {
  color: #697386;
  font-size: 14px;
  font-weight: 600;
}

.summary-icon {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  font-size: 28px;
}

.summary-icon.orange {
  color: #f59e0b;
  background: #fff7df;
}

.summary-icon.deep-orange {
  color: #f97316;
  background: #fff1e8;
}

.summary-icon.green {
  color: #2fa66a;
  background: #e9f7ef;
}

.summary-icon.blue {
  color: #4f97e8;
  background: #edf6ff;
}

.summary-icon.purple {
  color: #8b5bd6;
  background: #f3ecff;
}

.summary-value {
  margin: 6px 0 0;
  color: #111827;
  font-size: 30px;
  line-height: 36px;
  font-weight: 700;
}

.summary-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #98a2b3;
  font-size: 13px;
  white-space: nowrap;
}

.summary-meta strong {
  color: #f97316;
  font-weight: 700;
}

.summary-meta strong.down {
  color: #28a76f;
}

.section-card,
.chart-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.section-card :deep(.el-card__header) {
  padding: 16px 18px 10px;
}

.appointment-header {
  justify-content: space-between;
  gap: 16px;
}

.appointment-header h2,
.chart-title span {
  margin: 0;
  color: #111827;
  font-size: 18px;
  font-weight: 700;
}

.appointment-tools {
  align-items: center;
  gap: 14px;
}

.appointment-search,
.appointment-date {
  display: inline-flex;
  align-items: center;
  height: 44px;
  vertical-align: top;
}

.appointment-search {
  width: 230px;
}

.appointment-date {
  width: 170px;
}

.appointment-tools :deep(.el-input),
.appointment-tools :deep(.el-date-editor) {
  height: 44px;
  line-height: 44px;
}

.appointment-search :deep(.el-input__wrapper),
.appointment-date.el-date-editor.el-input__wrapper,
.appointment-tools :deep(.el-date-editor.el-input__wrapper) {
  height: 44px;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
}

.appointment-tabs {
  gap: 12px;
  margin-top: 18px;
}

.appointment-tabs button {
  height: 34px;
  padding: 0 18px;
  border: 1px solid #edf0f5;
  border-radius: 999px;
  background: #ffffff;
  color: #475467;
  font-weight: 600;
  cursor: pointer;
  transition: color 180ms ease, background-color 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.appointment-tabs button.active {
  border-color: #ffe1a3;
  background: #fff8e8;
  color: #f97316;
  box-shadow: 0 6px 14px rgba(245, 158, 11, 0.08);
}

.section-card :deep(.el-table) {
  border-radius: 8px;
}

.section-card :deep(.el-table th.el-table__cell) {
  background: #fbfbfc;
}

.name-main {
  color: #111827;
  font-weight: 600;
}

.task-time {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #111827;
}

.task-date {
  color: #64748b;
  font-size: 14px;
  line-height: 18px;
  font-weight: 600;
}

.task-time strong {
  color: #111827;
  font-size: 18px;
  line-height: 22px;
  font-weight: 800;
}

.name-sub {
  margin-top: 2px;
  color: #8a94a6;
  font-size: 12px;
}

.status-tag {
  border: 0;
  border-radius: 6px;
}

.status-pending {
  --el-tag-bg-color: #fff7df;
  --el-tag-text-color: #f59e0b;
}

.status-service,
.status-confirmed,
.status-done {
  --el-tag-bg-color: #eaf8ef;
  --el-tag-text-color: #1f9d61;
}

.status-muted {
  --el-tag-bg-color: #f3f4f6;
  --el-tag-text-color: #98a2b3;
}

.status-unpaid {
  --el-tag-bg-color: #fff7df;
  --el-tag-text-color: #f59e0b;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.table-action {
  height: 30px;
  padding: 0 16px;
  border-color: #e5e7eb;
  background: #ffffff;
  color: #475467;
}

.table-action + .table-action {
  margin-left: 0;
}

.table-action.primary {
  border-color: #ffe1a3;
  background: #fffaf0;
  color: #f59e0b;
}

.table-action.ghost {
  color: #98a2b3;
}

.chart-title {
  justify-content: space-between;
}

.chart-title small {
  color: #8a94a6;
  font-size: 12px;
  font-weight: 600;
}

.chart {
  width: 100%;
  height: 340px;
}

.revenue-card :deep(.el-card__body) {
  padding: 18px 24px 24px;
}

.revenue-split {
  gap: 26px;
}

.donut-chart {
  width: 52%;
  min-width: 260px;
  height: 340px;
}

.revenue-list {
  flex: 1;
}

.revenue-row {
  display: grid;
  grid-template-columns: 14px 1fr 90px 58px;
  gap: 10px;
  min-height: 48px;
  align-items: center;
  border-bottom: 1px solid #edf0f5;
  color: #475467;
  font-size: 14px;
}

.revenue-row strong {
  color: #111827;
  text-align: right;
}

.revenue-row.total {
  margin-top: 10px;
  border-bottom: 0;
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.revenue-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.revenue-dot-0 {
  background: #ffdc73;
}

.revenue-dot-1 {
  background: #ffd19a;
}

.revenue-dot-2 {
  background: #e8d4aa;
}

@media (max-width: 1180px) {
  .summary-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .dashboard-head,
  .appointment-header,
  .revenue-split {
    align-items: stretch;
    flex-direction: column;
  }

  .appointment-tools {
    flex-wrap: wrap;
  }

  .summary-row {
    grid-template-columns: 1fr;
    row-gap: 10px;
  }

  .summary-card {
    min-height: 96px;
  }

  .summary-card :deep(.el-card__body) {
    padding: 14px 20px;
  }

  .summary-content {
    justify-content: center;
  }

  .donut-chart {
    width: 100%;
  }
}
</style>
