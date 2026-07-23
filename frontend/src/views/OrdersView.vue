<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { fetchOrders, updateOrderStatus } from '../api/orders'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const total = ref(0)

const query = reactive({
  orderNo: '',
  customerName: '',
  petName: '',
  status: '',
  dateRange: [],
  page: 1,
  pageSize: 10,
})

const statusOptions = [
  { label: '待确认', value: 'PENDING' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '服务中', value: 'IN_SERVICE' },
  { label: '已完成', value: 'COMPLETED' },
]

const statusText = Object.fromEntries(statusOptions.map((item) => [item.value, item.label]))

const statusTag = {
  PENDING: 'warning',
  CONFIRMED: 'primary',
  REJECTED: 'danger',
  CANCELLED: 'info',
  IN_SERVICE: 'success',
  COMPLETED: 'success',
}

const actions = {
  PENDING: [
    { label: '确认', status: 'CONFIRMED' },
    { label: '拒绝', status: 'REJECTED' },
    { label: '取消', status: 'CANCELLED' },
  ],
  CONFIRMED: [
    { label: '开始服务', status: 'IN_SERVICE' },
    { label: '取消', status: 'CANCELLED' },
  ],
  IN_SERVICE: [{ label: '完成服务', status: 'COMPLETED' }],
}

async function loadOrders() {
  loading.value = true
  try {
    const params = {
      orderNo: query.orderNo,
      customerName: query.customerName,
      petName: query.petName,
      status: query.status,
      appointmentStart: query.dateRange?.[0],
      appointmentEnd: query.dateRange?.[1],
      page: query.page,
      pageSize: query.pageSize,
    }
    const result = await fetchOrders(params)
    records.value = result.records
    total.value = result.total
    query.page = result.page
    query.pageSize = result.pageSize
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, {
    orderNo: '',
    customerName: '',
    petName: '',
    status: '',
    dateRange: [],
    page: 1,
  })
  loadOrders()
}

async function changeStatus(row, action) {
  await ElMessageBox.confirm(`确定将订单「${row.orderNo}」${action.label}吗？`, '确认操作', { type: 'warning' })
  await updateOrderStatus(row.id, { status: action.status, remark: action.label })
  ElMessage.success('订单状态已更新')
  loadOrders()
}

function handlePageChange(page) {
  query.page = page
  loadOrders()
}

function handlePageSizeChange(pageSize) {
  query.pageSize = pageSize
  query.page = 1
  loadOrders()
}

onMounted(loadOrders)
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <h1 class="page-title">订单管理</h1>
      <el-button type="primary" :icon="Plus" @click="router.push('/orders/create')">新建订单</el-button>
    </div>

    <el-card shadow="never" class="filter-panel">
      <el-form :inline="true" :model="query">
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="请输入订单号" clearable />
        </el-form-item>
        <el-form-item label="客户">
          <el-input v-model="query.customerName" placeholder="客户姓名" clearable />
        </el-form-item>
        <el-form-item label="宠物">
          <el-input v-model="query.petName" placeholder="宠物名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable class="status-select">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约日期">
          <el-date-picker v-model="query.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="query.page = 1; loadOrders()">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-panel">
      <el-table v-loading="loading" :data="records" border>
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column prop="customerName" label="客户姓名" min-width="120" />
        <el-table-column prop="petName" label="宠物名称" min-width="120" />
        <el-table-column prop="appointmentTime" label="预约时间" min-width="170" />
        <el-table-column prop="totalAmount" label="订单金额" width="110">
          <template #default="{ row }">￥{{ Number(row.totalAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag[row.status]">{{ statusText[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/orders/${row.id}`)">详情</el-button>
            <el-button
              v-for="action in actions[row.status] || []"
              :key="action.status"
              link
              type="primary"
              @click="changeStatus(row, action)"
            >
              {{ action.label }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :current-page="query.page"
          :page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>
  </AppLayout>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.filter-panel,
.table-panel {
  border-radius: 8px;
}

.filter-panel {
  margin-bottom: 16px;
}

.status-select {
  width: 140px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
