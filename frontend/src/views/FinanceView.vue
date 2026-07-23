<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Money, Refresh, Search, TrendCharts, Wallet } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { fetchFinanceServiceItems, fetchFinanceSummary } from '../api/finance'
import { fetchServiceItems } from '../api/serviceItems'

const loading = ref(false)
const summary = ref({
  week: { revenue: 0, cost: 0, profit: 0, profitRate: 0 },
  month: { revenue: 0, cost: 0, profit: 0, profitRate: 0 },
})
const serviceStats = ref([])
const serviceItems = ref([])

const query = reactive({
  dateRange: [],
  category: '',
  serviceItemId: '',
})

const categoryOptions = computed(() => [...new Set(serviceItems.value.map((item) => item.category).filter(Boolean))])

const metricCards = computed(() => [
  { title: '本周总营收', value: summary.value.week.revenue, icon: Money, tone: 'revenue' },
  { title: '本周总成本', value: summary.value.week.cost, icon: Wallet, tone: 'cost' },
  { title: '本周净利润', value: summary.value.week.profit, icon: TrendCharts, tone: 'profit', rate: summary.value.week.profitRate },
  { title: '本月总营收', value: summary.value.month.revenue, icon: Money, tone: 'revenue' },
  { title: '本月总成本', value: summary.value.month.cost, icon: Wallet, tone: 'cost' },
  { title: '本月净利润', value: summary.value.month.profit, icon: TrendCharts, tone: 'profit', rate: summary.value.month.profitRate },
])

function money(value) {
  return `￥${Number(value || 0).toFixed(2)}`
}

function buildParams() {
  return {
    startDate: query.dateRange?.[0] || '',
    endDate: query.dateRange?.[1] || '',
    category: query.category,
    serviceItemId: query.serviceItemId,
  }
}

async function loadFinance() {
  loading.value = true
  try {
    const [summaryResult, serviceStatsResult] = await Promise.all([
      fetchFinanceSummary(),
      fetchFinanceServiceItems(buildParams()),
    ])
    summary.value = summaryResult
    serviceStats.value = serviceStatsResult
  } finally {
    loading.value = false
  }
}

async function loadServiceItems() {
  const result = await fetchServiceItems({ page: 1, pageSize: 100 })
  serviceItems.value = result.records
}

function resetQuery() {
  query.dateRange = []
  query.category = ''
  query.serviceItemId = ''
  loadFinance()
}

onMounted(async () => {
  await loadServiceItems()
  await loadFinance()
})
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <h1 class="page-title">财务管理</h1>
      <el-button text type="primary" :icon="Refresh" @click="loadFinance">刷新</el-button>
    </div>

    <el-row v-loading="loading" :gutter="16">
      <el-col v-for="card in metricCards" :key="card.title" :xs="24" :sm="12" :lg="8">
        <el-card shadow="never" class="metric-card">
          <div class="metric-top">
            <span>{{ card.title }}</span>
            <el-icon :class="['metric-icon', card.tone]"><component :is="card.icon" /></el-icon>
          </div>
          <div :class="['metric-value', card.tone]">{{ money(card.value) }}</div>
          <div class="muted">
            <span v-if="card.rate !== undefined">利润率 {{ Number(card.rate || 0).toFixed(2) }}%</span>
            <span v-else>按订单创建时间统计</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="filter-panel">
      <el-form :inline="true" :model="query">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="date-range"
          />
        </el-form-item>
        <el-form-item label="服务分类">
          <el-select v-model="query.category" placeholder="全部分类" clearable class="filter-select">
            <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="服务项目">
          <el-select v-model="query.serviceItemId" placeholder="全部服务" clearable filterable class="filter-select">
            <el-option v-for="item in serviceItems" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadFinance">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-panel">
      <template #header>
        <div class="card-header">
          <span>服务项目利润表</span>
          <span class="muted">收入、成本、利润均来自订单明细快照</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="serviceStats" border empty-text="暂无财务数据">
        <el-table-column prop="serviceName" label="服务项目" min-width="150" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="orderCount" label="订单数" width="100" />
        <el-table-column prop="quantity" label="服务次数" width="100" />
        <el-table-column prop="revenue" label="营收" width="130">
          <template #default="{ row }">{{ money(row.revenue) }}</template>
        </el-table-column>
        <el-table-column prop="cost" label="成本" width="130">
          <template #default="{ row }">{{ money(row.cost) }}</template>
        </el-table-column>
        <el-table-column prop="profit" label="利润" width="130">
          <template #default="{ row }">
            <span class="profit-text">{{ money(row.profit) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="profitRate" label="利润率" width="120">
          <template #default="{ row }">{{ Number(row.profitRate || 0).toFixed(2) }}%</template>
        </el-table-column>
      </el-table>
    </el-card>
  </AppLayout>
</template>

<style scoped>
.page-header,
.metric-top,
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-header {
  margin-bottom: 16px;
}

.metric-card,
.filter-panel,
.table-panel {
  border-radius: 8px;
}

.metric-card {
  margin-bottom: 16px;
}

.metric-top {
  color: #6b7280;
}

.metric-icon {
  font-size: 20px;
}

.metric-value {
  margin: 12px 0 6px;
  font-size: 28px;
  font-weight: 700;
}

.revenue {
  color: #0f766e;
}

.cost {
  color: #b45309;
}

.profit,
.profit-text {
  color: #2563eb;
}

.filter-panel {
  margin-bottom: 16px;
}

.date-range {
  width: 260px;
}

.filter-select {
  width: 160px;
}
</style>
