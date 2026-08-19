<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, FirstAidKit, Refresh, Search, Scissor, Suitcase } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { fetchCustomers } from '../api/customers'
import {
  checkInBoardingOrder,
  confirmBoardingOrderPayment,
  confirmBoardingOrderPickedUp,
  createBoardingOrder,
  fetchBoardingLocations,
} from '../api/boarding'
import { confirmOrderPayment, createOrder, fetchOrders, updateOrderStatus } from '../api/orders'
import { fetchCustomerPets } from '../api/pets'
import { fetchEnabledServiceItems } from '../api/serviceItems'

const router = useRouter()
const loading = ref(false)
const baseLoading = ref(false)
const saving = ref(false)
const formRef = ref()
const records = ref([])
const total = ref(0)
const customers = ref([])
const pets = ref([])
const serviceItems = ref([])
const boardingLocations = ref([])

const query = reactive({
  orderNo: '',
  customerName: '',
  petName: '',
  status: '',
  paymentStatus: '',
  dateRange: [],
  page: 1,
  pageSize: 10,
})

const statusOptions = [
  { label: '待确认', value: 'PENDING' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已预约', value: 'RESERVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '服务中', value: 'IN_SERVICE' },
  { label: '已入住', value: 'CHECKED_IN' },
  { label: '已完成/已接回', value: 'COMPLETED' },
]

const form = reactive({
  orderType: 'SERVICE',
  customerId: '',
  petId: '',
  serviceItemIds: [],
  appointmentTime: '',
  locationId: '',
  plannedCheckInTime: '',
  plannedCheckOutTime: '',
  remark: '',
})

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
}

const customerOptions = computed(() => customers.value.map((item) => ({
  label: `${item.name} / ${item.phone || '-'}`,
  value: item.id,
})))

const totalAmount = computed(() => serviceItems.value
  .filter((item) => form.serviceItemIds.includes(item.id))
  .reduce((sum, item) => sum + Number(item.price), 0))

const selectedBoardingLocation = computed(() => boardingLocations.value.find((item) => item.id === form.locationId))

const boardingDays = computed(() => {
  if (!form.plannedCheckInTime || !form.plannedCheckOutTime) {
    return 0
  }
  const start = new Date(form.plannedCheckInTime)
  const end = new Date(form.plannedCheckOutTime)
  if (!Number.isFinite(start.getTime()) || !Number.isFinite(end.getTime()) || end <= start) {
    return 0
  }
  return Math.max(1, Math.ceil((end.getTime() - start.getTime()) / 86400000))
})

const boardingAmount = computed(() => boardingDays.value * Number(selectedBoardingLocation.value?.pricePerDay || 0))

const serviceIcons = [Suitcase, Scissor, FirstAidKit]

const statusText = Object.fromEntries(statusOptions.map((item) => [item.value, item.label]))

const statusTag = {
  PENDING: 'warning',
  CONFIRMED: 'primary',
  RESERVED: 'warning',
  REJECTED: 'danger',
  CANCELLED: 'info',
  IN_SERVICE: 'success',
  CHECKED_IN: 'success',
  COMPLETED: 'success',
}

const paymentStatusOptions = [
  { label: '未支付', value: 'UNPAID' },
  { label: '已支付', value: 'PAID' },
]

const paymentStatusText = Object.fromEntries(paymentStatusOptions.map((item) => [item.value, item.label]))

const paymentStatusTag = {
  UNPAID: 'warning',
  PAID: 'success',
}

function paymentMethodText(value) {
  const methods = {
    MOCK: '模拟支付',
    MANUAL: '人工确认',
  }
  return methods[value] || value || '-'
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
      paymentStatus: query.paymentStatus,
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

async function loadBaseData() {
  baseLoading.value = true
  try {
    const [customerResult, serviceItemResult] = await Promise.all([
      fetchCustomers({ status: 'ENABLED', page: 1, pageSize: 100 }),
      fetchEnabledServiceItems(),
    ])
    customers.value = customerResult.records
    serviceItems.value = serviceItemResult.records
    const locationResult = await fetchBoardingLocations({ status: 'ENABLED', cleanStatus: 'CLEAN', page: 1, pageSize: 200 })
    boardingLocations.value = locationResult.records || []
  } finally {
    baseLoading.value = false
  }
}

function resetQuery() {
  Object.assign(query, {
    orderNo: '',
    customerName: '',
    petName: '',
    status: '',
    paymentStatus: '',
    dateRange: [],
    page: 1,
  })
  loadOrders()
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, {
    orderType: 'SERVICE',
    customerId: '',
    petId: '',
    serviceItemIds: [],
    appointmentTime: '',
    locationId: '',
    plannedCheckInTime: '',
    plannedCheckOutTime: '',
    remark: '',
  })
  pets.value = []
}

async function changeStatus(row, action) {
  await ElMessageBox.confirm(`确定将订单「${row.orderNo}」${action.label}吗？`, '确认操作', { type: 'warning' })
  await updateOrderStatus(row.id, { status: action.status, remark: action.label })
  ElMessage.success('订单状态已更新')
  loadOrders()
}

async function startBoardingService(row) {
  await ElMessageBox.confirm(`确认开始「${row.petName}」的宠物托管服务吗？`, '开始服务', {
    type: 'warning',
    confirmButtonText: '开始服务',
  })
  await checkInBoardingOrder(row.id)
  ElMessage.success('已开始服务，客户端状态已同步为已入住')
  loadOrders()
}

function canConfirmPayment(row) {
  return row.status === 'COMPLETED' && (row.paymentStatus || 'UNPAID') === 'UNPAID'
}

function isPastDateTime(value) {
  if (!value) {
    return false
  }
  const date = new Date(String(value).replace(' ', 'T'))
  return Number.isFinite(date.getTime()) && date.getTime() <= Date.now()
}

function canConfirmBoardingPickedUp(row) {
  return row.orderType === 'BOARDING' && row.status === 'CHECKED_IN' && isPastDateTime(row.plannedCheckOutTime)
}

async function confirmPayment(row) {
  await ElMessageBox.confirm(`确认订单「${row.orderNo}」已完成收款吗？`, '确认已支付', { type: 'warning' })
  if (row.orderType === 'BOARDING') {
    await confirmBoardingOrderPayment(row.id, { paymentMethod: 'MANUAL' })
  } else {
    await confirmOrderPayment(row.id, { paymentMethod: 'MANUAL' })
  }
  ElMessage.success('已确认支付')
  loadOrders()
}

async function confirmBoardingPickedUp(row) {
  await ElMessageBox.confirm(`确认「${row.petName}」已被客户接回？确认后才能继续确认支付。`, '确认已接回', {
    type: 'warning',
    confirmButtonText: '确认已接回',
    cancelButtonText: '取消',
  })
  await confirmBoardingOrderPickedUp(row.id)
  ElMessage.success('已确认接回，可继续确认支付')
  loadOrders()
}

function openOrderDetail(row) {
  if (row.orderType === 'BOARDING') {
    router.push({ path: '/boarding', query: { tab: 'orders' } })
    return
  }
  router.push(`/orders/${row.id}`)
}

function serviceTagClass(name) {
  if (name === '基础洗护') {
    return 'service-tag-care'
  }
  if (name === '精致美容') {
    return 'service-tag-grooming'
  }
  if (name === '宠物托管' || /^托管\s*\d+\s*天$/.test(name)) {
    return 'service-tag-boarding'
  }
  return 'service-tag-default'
}

function serviceOptionClass(name) {
  return serviceTagClass(name).replace('service-tag-', 'service-option-')
}

function formatDateTime(value) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function submitOrder() {
  await formRef.value.validate()
  if (form.orderType === 'SERVICE' && !form.serviceItemIds.length) {
    ElMessage.warning('请选择服务项目')
    return
  }
  if (form.orderType === 'SERVICE' && !form.appointmentTime) {
    ElMessage.warning('请选择预约时间')
    return
  }
  if (form.orderType === 'BOARDING' && (!form.locationId || !form.plannedCheckInTime || !form.plannedCheckOutTime)) {
    ElMessage.warning('请完整填写托管位置、入住时间和退房时间')
    return
  }
  saving.value = true
  try {
    if (form.orderType === 'BOARDING') {
      await createBoardingOrder({
        customerId: Number(form.customerId),
        petId: Number(form.petId),
        locationId: Number(form.locationId),
        plannedCheckInTime: formatDateTime(form.plannedCheckInTime),
        plannedCheckOutTime: formatDateTime(form.plannedCheckOutTime),
        totalAmount: null,
        remark: form.remark,
      })
      ElMessage.success('托管订单已创建')
    } else {
      await createOrder({
        customerId: Number(form.customerId),
        petId: Number(form.petId),
        serviceItemIds: form.serviceItemIds.map(Number),
        appointmentTime: formatDateTime(form.appointmentTime),
        remark: form.remark,
      })
      ElMessage.success('订单已创建')
    }
    resetForm()
    query.page = 1
    loadOrders()
  } finally {
    saving.value = false
  }
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

watch(() => form.customerId, async (customerId) => {
  form.petId = ''
  pets.value = []
  if (customerId) {
    const result = await fetchCustomerPets(customerId, true)
    pets.value = result.records
  }
})

onMounted(() => {
  loadOrders()
  loadBaseData()
})
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h1 class="page-title">订单管理</h1>
      </div>
    </div>

    <el-card v-loading="baseLoading" shadow="never" class="create-order-card">
      <div class="panel-title">新建订单</div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="create-form">
        <div class="create-grid">
          <el-form-item label="订单类型" class="order-type-form-item">
            <el-radio-group v-model="form.orderType">
              <el-radio-button label="SERVICE">普通服务</el-radio-button>
              <el-radio-button label="BOARDING">宠物托管</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="客户" prop="customerId" required>
            <el-select v-model="form.customerId" filterable placeholder="请选择客户" class="wide-control">
              <el-option v-for="item in customerOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="宠物" prop="petId" required>
            <el-select v-model="form.petId" filterable placeholder="请选择宠物" class="wide-control" :disabled="!form.customerId">
              <el-option v-for="item in pets" :key="item.id" :label="`${item.name} / ${item.species}`" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.orderType === 'SERVICE'" label="预约时间" required>
            <el-date-picker
              v-model="form.appointmentTime"
              type="datetime"
              placeholder="请选择预约时间"
              format="YYYY-MM-DD HH:mm:ss"
              class="wide-control"
              :prefix-icon="Calendar"
            />
          </el-form-item>
          <el-form-item v-if="form.orderType === 'BOARDING'" label="托管位置" required>
            <el-select v-model="form.locationId" filterable placeholder="请选择托管位置" class="wide-control">
              <el-option
                v-for="item in boardingLocations"
                :key="item.id"
                :label="`${item.code} ${item.name} / ￥${Number(item.pricePerDay || 0).toFixed(2)}/天`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.orderType === 'BOARDING'" label="入住时间" required>
            <el-date-picker
              v-model="form.plannedCheckInTime"
              type="datetime"
              placeholder="请选择入住时间"
              format="YYYY-MM-DD HH:mm:ss"
              class="wide-control"
              :prefix-icon="Calendar"
            />
          </el-form-item>
          <el-form-item v-if="form.orderType === 'BOARDING'" label="退房时间" required>
            <el-date-picker
              v-model="form.plannedCheckOutTime"
              type="datetime"
              placeholder="请选择退房时间"
              format="YYYY-MM-DD HH:mm:ss"
              class="wide-control"
              :prefix-icon="Calendar"
            />
          </el-form-item>
          <el-form-item v-if="form.orderType === 'SERVICE'" label="服务项目" required class="service-form-item">
            <el-checkbox-group v-model="form.serviceItemIds" class="service-options">
              <el-checkbox v-for="(item, index) in serviceItems" :key="item.id" :value="item.id" class="service-card" :class="serviceOptionClass(item.name)">
                <span class="service-icon" aria-hidden="true">
                  <el-icon><component :is="serviceIcons[index % serviceIcons.length]" /></el-icon>
                </span>
                <span class="service-name">{{ item.name }}</span>
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="订单金额" class="amount-form-item">
            <strong class="amount">￥{{ (form.orderType === 'BOARDING' ? boardingAmount : totalAmount).toFixed(2) }}</strong>
          </el-form-item>
          <el-form-item label="备注" class="remark-form-item">
            <el-input v-model="form.remark" type="textarea" maxlength="500" show-word-limit placeholder="请输入备注（可选）" />
          </el-form-item>
          <el-form-item class="create-actions">
            <el-button type="primary" :loading="saving" @click="submitOrder">创建订单</el-button>
            <el-button @click="resetForm">取消</el-button>
          </el-form-item>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-panel order-list-card">
      <div class="panel-title">订单列表</div>
      <el-form :inline="true" :model="query" class="list-filter-form">
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="请输入订单号" clearable class="filter-input" />
        </el-form-item>
        <el-form-item label="客户">
          <el-input v-model="query.customerName" placeholder="客户姓名" clearable class="filter-input filter-input-sm" />
        </el-form-item>
        <el-form-item label="宠物">
          <el-input v-model="query.petName" placeholder="宠物名称" clearable class="filter-input filter-input-sm" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable class="status-select">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付状态">
          <el-select v-model="query.paymentStatus" placeholder="全部支付状态" clearable class="payment-status-select">
            <el-option v-for="item in paymentStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约日期">
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始"
            end-placeholder="结束"
            class="date-range"
          />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" :icon="Search" @click="query.page = 1; loadOrders()">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="records" border>
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column prop="customerName" label="客户姓名" min-width="100" />
        <el-table-column prop="petName" label="宠物名称" min-width="100" />
        <el-table-column prop="appointmentTime" label="预约时间" min-width="170" />
        <el-table-column label="服务内容" min-width="220">
          <template #default="{ row }">
            <div class="service-tags">
              <el-tag
                v-for="name in row.serviceNames || []"
                :key="name"
                effect="light"
                class="service-tag"
                :class="serviceTagClass(name)"
              >
                {{ name }}
              </el-tag>
              <span v-if="!row.serviceNames?.length" class="empty-text">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="订单金额" width="130">
          <template #default="{ row }">￥{{ Number(row.totalAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="paymentStatus" label="支付状态" width="110">
          <template #default="{ row }">
            <el-tag :type="paymentStatusTag[row.paymentStatus || 'UNPAID']">
              {{ paymentStatusText[row.paymentStatus || 'UNPAID'] || row.paymentStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付信息" min-width="190">
          <template #default="{ row }">
            <div v-if="(row.paymentStatus || 'UNPAID') === 'PAID'" class="payment-info">
              <strong>{{ row.paymentConfirmedByName || '-' }}</strong>
              <span>{{ row.paidAt || '-' }}</span>
            </div>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag[row.status]">{{ row.orderType === 'BOARDING' && row.status === 'COMPLETED' ? '已接回' : (statusText[row.status] || row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openOrderDetail(row)">详情</el-button>
            <el-button
              v-for="action in row.orderType === 'BOARDING' ? [] : actions[row.status] || []"
              :key="action.status"
              link
              type="primary"
              @click="changeStatus(row, action)"
            >
              {{ action.label }}
            </el-button>
            <el-button v-if="row.orderType === 'BOARDING' && row.status === 'RESERVED'" link type="primary" @click="startBoardingService(row)">开始服务</el-button>
            <el-button v-if="canConfirmBoardingPickedUp(row)" link type="primary" @click="confirmBoardingPickedUp(row)">确认已接回</el-button>
            <el-button v-if="canConfirmPayment(row)" link type="success" @click="confirmPayment(row)">确认已支付</el-button>
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
  margin-bottom: 18px;
}

.filter-input {
  width: 220px;
}

.filter-input-sm {
  width: 180px;
}

.status-select {
  width: 210px;
}

.payment-status-select {
  width: 180px;
}

.date-range {
  width: 310px;
}

.list-filter-form {
  display: grid;
  grid-template-columns: auto auto auto auto auto auto;
  gap: 10px 18px;
  align-items: center;
  margin-bottom: 14px;
}

.filter-actions {
  grid-column: 1 / -1;
  justify-self: end;
  width: 220px;
  margin-left: 0;
}

.filter-actions :deep(.el-form-item__content) {
  display: flex;
  flex-wrap: nowrap;
  gap: 12px;
  justify-content: flex-start;
  margin-left: 0 !important;
}

.filter-actions :deep(.el-button) {
  min-width: 94px;
  height: 42px;
}

.filter-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.payment-info {
  display: grid;
  gap: 4px;
  line-height: 18px;
}

.payment-info strong {
  color: #111827;
  font-size: 13px;
}

.payment-info span {
  color: #64748b;
  font-size: 12px;
}

.order-list-card,
.create-order-card {
  min-width: 0;
  border-radius: 8px;
}

.create-order-card {
  margin-bottom: 16px;
  scroll-margin-top: 20px;
}

.panel-title {
  margin: 0 0 18px;
  color: #111827;
  font-size: 18px;
  font-weight: 700;
}

.table-panel {
  border-radius: 8px;
}

.order-list-card :deep(.el-card__body) {
  padding: 20px 24px 0;
}

.create-order-card :deep(.el-card__body) {
  padding: 20px 24px 18px;
}

.create-form {
  padding-top: 2px;
}

.create-grid {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(260px, 1fr) minmax(300px, 1fr);
  gap: 0 42px;
  align-items: start;
  min-width: 0;
}

.create-form :deep(.el-form-item) {
  min-width: 0;
  margin-bottom: 18px;
}

.create-form :deep(.el-form-item__label) {
  justify-content: flex-start;
  padding-right: 10px;
  line-height: 22px;
  white-space: nowrap;
}

.wide-control {
  width: 100%;
}

.service-options {
  display: grid;
  grid-template-columns: repeat(3, 176px);
  gap: 14px;
  width: auto;
}

.service-card {
  width: 176px;
  height: 82px;
  margin: 0;
  padding: 0;
}

.service-card :deep(.el-checkbox__input) {
  display: none;
}

.service-card :deep(.el-checkbox__label) {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #e6e9ee;
  border-radius: 8px;
  color: #1f2937;
  white-space: normal;
  text-align: center;
  transition: border-color 180ms ease, box-shadow 180ms ease, background-color 180ms ease;
}

.service-card.is-checked :deep(.el-checkbox__label) {
  border-color: #f6a700;
  background: #fff9ec;
  box-shadow: 0 0 0 2px rgba(246, 167, 0, 0.14);
}

.service-card.service-option-care.is-checked :deep(.el-checkbox__label) {
  border-color: #bfdbfe;
  background: #edf5ff;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.14);
}

.service-card.service-option-grooming.is-checked :deep(.el-checkbox__label) {
  border-color: #fecaca;
  background: #fff1f0;
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.14);
}

.service-card.service-option-boarding.is-checked :deep(.el-checkbox__label) {
  border-color: #bbf7d0;
  background: #ecfdf5;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.14);
}

.service-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 31px;
  height: 31px;
  border-radius: 8px;
  background: #fff3d0;
  color: #f59e0b;
  font-size: 24px;
}

.service-option-care .service-icon {
  background: #edf5ff;
  color: #2563eb;
}

.service-option-grooming .service-icon {
  background: #fff1f0;
  color: #ef4444;
}

.service-option-boarding .service-icon {
  background: #ecfdf5;
  color: #16a34a;
}

.service-name {
  font-weight: 700;
}

.service-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.service-tag {
  margin: 0;
}

.service-tag-default {
  --el-tag-bg-color: #fff7e8;
  --el-tag-border-color: #ffd68a;
  --el-tag-text-color: #f59e0b;
}

.service-tag-care {
  --el-tag-bg-color: #edf5ff;
  --el-tag-border-color: #bfdbfe;
  --el-tag-text-color: #2563eb;
}

.service-tag-grooming {
  --el-tag-bg-color: #fff1f0;
  --el-tag-border-color: #fecaca;
  --el-tag-text-color: #ef4444;
}

.service-tag-boarding {
  --el-tag-bg-color: #ecfdf5;
  --el-tag-border-color: #bbf7d0;
  --el-tag-text-color: #16a34a;
}

.empty-text {
  color: #98a2b3;
}

.service-meta {
  display: none;
}

.amount {
  display: block;
  padding-top: 8px;
  font-size: 26px;
  color: #111827;
}

.service-form-item {
  grid-column: 1 / 3;
}

.amount-form-item {
  grid-column: 3;
  grid-row: 2;
  align-self: start;
}

.remark-form-item {
  grid-column: 1 / -1;
}

.amount-form-item :deep(.el-form-item__label::before),
.remark-form-item :deep(.el-form-item__label::before) {
  content: '*';
  visibility: hidden;
  margin-right: 4px;
}

.create-actions {
  grid-column: 1 / -1;
  justify-self: end;
  width: 220px;
  margin-left: 0;
}

.create-actions :deep(.el-form-item__content) {
  display: flex;
  flex-wrap: nowrap;
  gap: 12px;
  justify-content: flex-start;
  margin-left: 0 !important;
}

.create-actions :deep(.el-button) {
  min-width: 104px;
  height: 42px;
}

.create-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.create-form :deep(.el-textarea__inner) {
  min-height: 74px !important;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 1280px) {
  .create-grid,
  .list-filter-form {
    grid-template-columns: 1fr 1fr;
  }

  .service-form-item,
  .amount-form-item,
  .remark-form-item,
  .create-actions,
  .filter-actions {
    grid-column: 1 / -1;
    grid-row: auto;
  }

  .create-actions,
  .filter-actions {
    justify-self: end;
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .service-options {
    grid-template-columns: 1fr;
    width: 100%;
  }

  .service-card {
    width: 100%;
  }

  .create-grid,
  .list-filter-form {
    grid-template-columns: 1fr;
  }

  .create-actions,
  .filter-actions {
    width: 100%;
  }

  .create-actions :deep(.el-form-item__content),
  .filter-actions :deep(.el-form-item__content) {
    justify-content: flex-start;
  }
}
</style>
