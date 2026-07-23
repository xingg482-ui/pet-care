<script setup>
import { onMounted, ref } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { fetchDashboardSummary } from '../api/dashboard'

const loading = ref(false)
const summary = ref({
  customerCount: 0,
  petCount: 0,
  serviceItemCount: 0,
  pendingOrderCount: 0,
  todayAppointmentCount: 0,
})

const cards = [
  { label: '客户总数', key: 'customerCount' },
  { label: '宠物总数', key: 'petCount' },
  { label: '服务项目', key: 'serviceItemCount' },
  { label: '待确认订单', key: 'pendingOrderCount' },
  { label: '今日预约', key: 'todayAppointmentCount' },
]

async function loadSummary() {
  loading.value = true
  try {
    summary.value = await fetchDashboardSummary()
  } finally {
    loading.value = false
  }
}

onMounted(loadSummary)
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <h1 class="page-title">首页</h1>
      <el-button text type="primary" @click="loadSummary">刷新</el-button>
    </div>
    <el-row v-loading="loading" :gutter="16">
      <el-col v-for="card in cards" :key="card.key" :xs="24" :sm="12" :md="8" :lg="4">
        <el-card shadow="never" class="summary-card">
          <div class="summary-value">{{ summary[card.key] }}</div>
          <div class="muted">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>
  </AppLayout>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.summary-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.summary-value {
  margin-bottom: 6px;
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}
</style>
