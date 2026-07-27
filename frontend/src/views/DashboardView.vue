<script setup>
import * as echarts from 'echarts'
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Collection, House, Refresh, Tickets, TrendCharts, User } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import {
  fetchDashboardRevenueTrend,
  fetchDashboardServiceRevenue,
  fetchDashboardSummary,
  fetchDashboardTodayActiveOrders,
} from '../api/dashboard'
import { updateOrderStatus } from '../api/orders'

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
})
const revenueTrend = ref([])
const serviceRevenue = ref([])
const todayActiveOrders = ref([])

const statusText = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  REJECTED: '已拒绝',
  CANCELLED: '已取消',
  IN_SERVICE: '服务中',
  COMPLETED: '已完成',
}

const cards = [
  { label: '客户总数', key: 'customerCount', icon: User, tone: 'green' },
  { label: '宠物总数', key: 'petCount', icon: Collection, tone: 'blue' },
  { label: '服务项目', key: 'serviceItemCount', icon: Tickets, tone: 'amber' },
  { label: '待确认订单', key: 'pendingOrderCount', icon: TrendCharts, tone: 'rose' },
  { label: '今日预约', key: 'todayAppointmentCount', icon: House, tone: 'teal' },
]

function money(value) {
  return `￥${Number(value || 0).toFixed(2)}`
}

async function loadDashboard() {
  loading.value = true
  try {
    const [summaryResult, trendResult, serviceResult, activeOrderResult] = await Promise.all([
      fetchDashboardSummary(),
      fetchDashboardRevenueTrend(),
      fetchDashboardServiceRevenue(),
      fetchDashboardTodayActiveOrders(),
    ])
    summary.value = summaryResult
    revenueTrend.value = trendResult
    serviceRevenue.value = serviceResult
    todayActiveOrders.value = activeOrderResult
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
    color: ['#0f766e', '#b45309', '#2563eb'],
    title: hasData ? undefined : emptyTitle('暂无营收趋势数据'),
    tooltip: { trigger: 'axis' },
    legend: { top: 0, data: ['营收', '成本', '利润'] },
    grid: { left: 42, right: 20, top: 48, bottom: 34 },
    xAxis: { type: 'category', data: revenueTrend.value.map((item) => item.date.slice(5)) },
    yAxis: { type: 'value' },
    series: [
      { name: '营收', type: 'line', smooth: true, data: revenueTrend.value.map((item) => Number(item.revenue || 0)) },
      { name: '成本', type: 'line', smooth: true, data: revenueTrend.value.map((item) => Number(item.cost || 0)) },
      { name: '利润', type: 'line', smooth: true, data: revenueTrend.value.map((item) => Number(item.profit || 0)) },
    ],
  }, true)
}

function renderServiceChart() {
  const chart = chartOf(serviceChartRef)
  const data = serviceRevenue.value.map((item) => ({ name: item.name, value: Number(item.value || 0) }))
  chart.setOption({
    color: ['#0f766e', '#f59e0b', '#2563eb', '#ec4899', '#8b5cf6', '#64748b'],
    title: data.length ? undefined : emptyTitle('暂无服务收入数据'),
    tooltip: { trigger: 'item', formatter: '{b}<br/>收入: ￥{c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      name: '服务收入',
      type: 'pie',
      radius: ['45%', '68%'],
      center: ['50%', '44%'],
      data,
      label: { formatter: '{b}' },
    }],
  }, true)
}

async function completeOrder(row) {
  await ElMessageBox.confirm(`确定将订单「${row.orderNo}」标记为已完成吗？`, '确认操作', { type: 'warning' })
  await updateOrderStatus(row.id, { status: 'COMPLETED', remark: '首页快捷完成' })
  ElMessage.success('订单已完成')
  loadDashboard()
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
    <div class="page-header">
      <h1 class="page-title">首页</h1>
      <el-button text type="primary" :icon="Refresh" @click="loadDashboard">刷新</el-button>
    </div>

    <el-row v-loading="loading" :gutter="16" class="summary-row">
      <el-col v-for="card in cards" :key="card.key" :xs="24" :sm="12" :md="8" :lg="4">
        <el-card shadow="never" class="summary-card">
          <div class="summary-top">
            <span>{{ card.label }}</span>
            <el-icon :class="['summary-icon', card.tone]"><component :is="card.icon" /></el-icon>
          </div>
          <div class="summary-value">{{ summary[card.key] }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-header">
          <span>今日未完成预约</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="todayActiveOrders" border empty-text="今日暂无未完成预约">
        <el-table-column prop="appointmentTime" label="预约时间" min-width="170" />
        <el-table-column prop="customerName" label="客户" width="120" />
        <el-table-column prop="petName" label="服务宠物" width="120" />
        <el-table-column prop="serviceNames" label="服务项目" min-width="180" show-overflow-tooltip />
        <el-table-column prop="totalAmount" label="金额" width="110">
          <template #default="{ row }">{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag>{{ statusText[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/orders/${row.id}`)">查看详情</el-button>
            <el-button v-if="row.status === 'IN_SERVICE'" link type="success" @click="completeOrder(row)">已完成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="chart-card">
          <template #header>最近 7 天营收趋势</template>
          <div ref="trendChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="chart-card">
          <template #header>服务项目收入占比</template>
          <div ref="serviceChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </AppLayout>
</template>

<style scoped>
.page-header,
.summary-top,
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-header {
  margin-bottom: 16px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  column-gap: 16px;
}

.summary-row :deep(.el-col) {
  display: block;
  max-width: none;
  flex: none;
  width: 100%;
}

.summary-card,
.section-card,
.chart-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.summary-top {
  color: var(--pc-muted);
  font-size: 13px;
}

.summary-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #f5f8fa;
  font-size: 17px;
}

.summary-value {
  margin-top: 10px;
  font-size: 27px;
  line-height: 34px;
  font-weight: 700;
  color: var(--pc-text);
}

.green {
  color: #0f766e;
}

.blue {
  color: #2563eb;
}

.amber {
  color: #b45309;
}

.rose {
  color: #be123c;
}

.teal {
  color: #0d9488;
}

.chart {
  width: 100%;
  height: 320px;
}

@media (max-width: 1180px) {
  .summary-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .summary-row {
    grid-template-columns: 1fr;
  }
}
</style>
