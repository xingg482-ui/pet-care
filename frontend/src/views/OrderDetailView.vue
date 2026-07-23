<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppLayout from '../components/AppLayout.vue'
import { fetchOrder, updateAppointmentTime, updateOrderStatus } from '../api/orders'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const detail = ref(null)
const appointmentDialogVisible = ref(false)
const appointmentForm = reactive({ appointmentTime: '' })

const statusOptions = [
  { label: '待确认', value: 'PENDING' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '服务中', value: 'IN_SERVICE' },
  { label: '已完成', value: 'COMPLETED' },
]

const statusText = Object.fromEntries(statusOptions.map((item) => [item.value, item.label]))

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

const canEditAppointment = computed(() => ['PENDING', 'CONFIRMED'].includes(detail.value?.order?.status))

function formatDateTime(value) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await fetchOrder(route.params.id)
  } finally {
    loading.value = false
  }
}

async function changeStatus(action) {
  await ElMessageBox.confirm(`确定${action.label}该订单吗？`, '确认操作', { type: 'warning' })
  await updateOrderStatus(route.params.id, { status: action.status, remark: action.label })
  ElMessage.success('订单状态已更新')
  loadDetail()
}

function openAppointmentDialog() {
  appointmentForm.appointmentTime = detail.value.order.appointmentTime
  appointmentDialogVisible.value = true
}

async function saveAppointmentTime() {
  saving.value = true
  try {
    await updateAppointmentTime(route.params.id, {
      appointmentTime: formatDateTime(appointmentForm.appointmentTime),
    })
    ElMessage.success('预约时间已更新')
    appointmentDialogVisible.value = false
    loadDetail()
  } finally {
    saving.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <h1 class="page-title">订单详情</h1>
      <el-button @click="router.push('/orders')">返回列表</el-button>
    </div>

    <div v-loading="loading">
      <template v-if="detail">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header">
              <span>订单信息</span>
              <div>
                <el-button v-if="canEditAppointment" link type="primary" @click="openAppointmentDialog">修改预约时间</el-button>
                <el-button
                  v-for="action in actions[detail.order.status] || []"
                  :key="action.status"
                  link
                  type="primary"
                  @click="changeStatus(action)"
                >
                  {{ action.label }}
                </el-button>
              </div>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ statusText[detail.order.status] }}</el-descriptions-item>
            <el-descriptions-item label="客户">{{ detail.order.customerName }}</el-descriptions-item>
            <el-descriptions-item label="宠物">{{ detail.order.petName }}</el-descriptions-item>
            <el-descriptions-item label="预约时间">{{ detail.order.appointmentTime }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">￥{{ Number(detail.order.totalAmount).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.order.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="section-card">
          <template #header>服务项目明细</template>
          <el-table :data="detail.items" border>
            <el-table-column prop="serviceName" label="服务名称" />
            <el-table-column prop="unitPrice" label="单价" width="120">
              <template #default="{ row }">￥{{ Number(row.unitPrice).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="90" />
            <el-table-column prop="subtotal" label="小计" width="120">
              <template #default="{ row }">￥{{ Number(row.subtotal).toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="section-card">
          <template #header>状态记录</template>
          <el-timeline>
            <el-timeline-item v-for="log in detail.statusLogs" :key="log.id" :timestamp="log.createdAt">
              {{ statusText[log.oldStatus] || '创建' }} -> {{ statusText[log.newStatus] }}
              <span v-if="log.remark" class="muted"> / {{ log.remark }}</span>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </template>
    </div>

    <el-dialog v-model="appointmentDialogVisible" title="修改预约时间" width="420px">
      <el-date-picker
        v-model="appointmentForm.appointmentTime"
        type="datetime"
        format="YYYY-MM-DD HH:mm:ss"
        class="full-width"
      />
      <template #footer>
        <el-button @click="appointmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAppointmentTime">保存</el-button>
      </template>
    </el-dialog>
  </AppLayout>
</template>

<style scoped>
.page-header,
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-header {
  margin-bottom: 16px;
}

.section-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.full-width {
  width: 100%;
}
</style>
