<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, FirstAidKit, Refresh, Scissor, Search, Suitcase } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { fetchMyPets } from '../api/pets'
import { fetchEnabledServiceItems } from '../api/serviceItems'
import { cancelMyOrder, createMyOrder, fetchMyOrders, payMyOrder } from '../api/orders'
import { cancelMyBoardingOrder, createMyBoardingOrder, fetchBoardingLocations, payMyBoardingOrder } from '../api/boarding'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const records = ref([])
const total = ref(0)
const pets = ref([])
const services = ref([])
const boardingLocations = ref([])

const query = reactive({
  orderNo: '',
  petName: '',
  status: '',
  paymentStatus: '',
  dateRange: [],
  page: 1,
  pageSize: 10,
})

const form = reactive({
  orderType: 'SERVICE',
  customerId: 0,
  petId: null,
  serviceItemIds: [],
  appointmentTime: '',
  locationId: '',
  plannedCheckInTime: '',
  plannedCheckOutTime: '',
  remark: '',
})

const rules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
}

const statusOptions = [
  { label: '待确认', value: 'PENDING' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已预约', value: 'RESERVED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '服务中', value: 'IN_SERVICE' },
  { label: '已入住', value: 'CHECKED_IN' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已拒绝', value: 'REJECTED' },
]

const statusTypes = {
  PENDING: 'warning',
  CONFIRMED: 'primary',
  RESERVED: 'warning',
  CANCELLED: 'info',
  IN_SERVICE: 'warning',
  CHECKED_IN: 'success',
  COMPLETED: 'success',
  REJECTED: 'danger',
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

const totalAmount = computed(() => services.value
  .filter((item) => form.serviceItemIds.includes(item.id))
  .reduce((sum, item) => sum + Number(item.price || 0), 0))

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

function statusName(value) {
  return statusOptions.find((item) => item.value === value)?.label || value
}

async function loadOrders() {
  loading.value = true
  try {
    const result = await fetchMyOrders({
      orderNo: query.orderNo,
      petName: query.petName,
      status: query.status,
      paymentStatus: query.paymentStatus,
      appointmentStart: query.dateRange?.[0],
      appointmentEnd: query.dateRange?.[1],
      page: query.page,
      pageSize: query.pageSize,
    })
    records.value = result.records
    total.value = result.total
    query.page = result.page
    query.pageSize = result.pageSize
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [petResult, serviceResult, locationResult] = await Promise.all([
    fetchMyPets(),
    fetchEnabledServiceItems(),
    fetchBoardingLocations({ status: 'ENABLED', cleanStatus: 'CLEAN', page: 1, pageSize: 200 }),
  ])
  pets.value = petResult.records.filter((item) => item.status !== 'DISABLED')
  services.value = serviceResult.records || []
  boardingLocations.value = locationResult.records || []
}

function resetQuery() {
  query.orderNo = ''
  query.petName = ''
  query.status = ''
  query.paymentStatus = ''
  query.dateRange = []
  query.page = 1
  loadOrders()
}

function resetForm() {
  Object.assign(form, {
    orderType: 'SERVICE',
    customerId: 0,
    petId: '',
    serviceItemIds: [],
    appointmentTime: '',
    locationId: '',
    plannedCheckInTime: '',
    plannedCheckOutTime: '',
    remark: '',
  })
  formRef.value?.resetFields()
}

function formatDateTime(value) {
  if (!value) {
    return ''
  }
  if (typeof value === 'string') {
    return value
  }
  const date = new Date(value)
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function saveOrder() {
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
      await createMyBoardingOrder({
        customerId: 0,
        petId: Number(form.petId),
        locationId: Number(form.locationId),
        plannedCheckInTime: formatDateTime(form.plannedCheckInTime),
        plannedCheckOutTime: formatDateTime(form.plannedCheckOutTime),
        totalAmount: null,
        remark: form.remark,
      })
      ElMessage.success('托管预约已提交')
    } else {
      await createMyOrder({
        customerId: 0,
        petId: Number(form.petId),
        serviceItemIds: form.serviceItemIds.map(Number),
        appointmentTime: formatDateTime(form.appointmentTime),
        remark: form.remark,
      })
      ElMessage.success('订单已提交，等待管理员确认')
    }
    resetForm()
    query.page = 1
    loadOrders()
  } finally {
    saving.value = false
  }
}

async function cancelOrder(row) {
  await ElMessageBox.confirm(`确定取消订单「${row.orderNo}」吗？`, '取消订单', { type: 'warning' })
  if (row.orderType === 'BOARDING') {
    await cancelMyBoardingOrder(row.id)
  } else {
    await cancelMyOrder(row.id)
  }
  ElMessage.success('订单已取消')
  loadOrders()
}

async function payOrder(row) {
  await ElMessageBox.confirm(`确认支付订单「${row.orderNo}」吗？`, '确认支付', { type: 'warning' })
  if (row.orderType === 'BOARDING') {
    await payMyBoardingOrder(row.id, { paymentMethod: 'MOCK' })
  } else {
    await payMyOrder(row.id, { paymentMethod: 'MOCK' })
  }
  ElMessage.success('支付成功')
  loadOrders()
}

function openOrderDetail(row) {
  if (row.orderType === 'BOARDING') {
    router.push('/my-boarding')
    return
  }
  router.push(`/my-orders/${row.id}`)
}

function canCancelOrder(row) {
  if (row.orderType === 'BOARDING') {
    return row.status === 'RESERVED'
  }
  return row.status === 'PENDING' || row.status === 'CONFIRMED'
}

function canPayOrder(row) {
  return row.status === 'COMPLETED' && (row.paymentStatus || 'UNPAID') !== 'PAID'
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

function handlePageChange(page) {
  query.page = page
  loadOrders()
}

function handlePageSizeChange(pageSize) {
  query.pageSize = pageSize
  query.page = 1
  loadOrders()
}

onMounted(() => {
  loadOptions()
  loadOrders()
})
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h1 class="page-title">订单管理</h1>
        <p class="muted">管理自己的宠物门店服务订单</p>
      </div>
    </div>

    <el-card shadow="never" class="create-order-card">
      <div class="panel-title">新建订单</div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="create-form">
        <div class="create-grid">
          <el-form-item label="订单类型" class="order-type-form-item">
            <el-radio-group v-model="form.orderType">
              <el-radio-button label="SERVICE">普通服务</el-radio-button>
              <el-radio-button label="BOARDING">宠物托管</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="宠物" prop="petId" required>
            <el-select v-model="form.petId" filterable placeholder="请选择宠物" class="wide-control">
              <el-option v-for="pet in pets" :key="pet.id" :label="`${pet.name} / ${pet.species}`" :value="pet.id" />
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
              <el-checkbox v-for="(service, index) in services" :key="service.id" :value="service.id" class="service-card" :class="serviceOptionClass(service.name)">
                <span class="service-icon" aria-hidden="true">
                  <el-icon><component :is="serviceIcons[index % serviceIcons.length]" /></el-icon>
                </span>
                <span class="service-name">{{ service.name }}</span>
                <span class="service-price">￥{{ Number(service.price || 0).toFixed(2) }}</span>
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="预计金额" class="amount-form-item">
            <strong class="amount">￥{{ (form.orderType === 'BOARDING' ? boardingAmount : totalAmount).toFixed(2) }}</strong>
          </el-form-item>
          <el-form-item label="备注" class="remark-form-item">
            <el-input v-model="form.remark" type="textarea" maxlength="500" show-word-limit placeholder="请输入备注（可选）" />
          </el-form-item>
          <el-form-item class="create-actions">
            <el-button type="primary" :loading="saving" @click="saveOrder">提交订单</el-button>
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
        <el-form-item label="宠物">
          <el-input v-model="query.petName" placeholder="宠物名称" clearable class="filter-input filter-input-sm" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态" class="status-select">
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
          <template #default="{ row }">￥{{ Number(row.totalAmount || 0).toFixed(2) }}</template>
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
          <template #default="{ row }"><el-tag :type="statusTypes[row.status]">{{ statusName(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openOrderDetail(row)">详情</el-button>
            <el-button v-if="canCancelOrder(row)" link type="warning" @click="cancelOrder(row)">取消</el-button>
            <el-button v-if="canPayOrder(row)" link type="primary" @click="payOrder(row)">支付</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total" :current-page="query.page" :page-size="query.pageSize" :page-sizes="[10, 20, 50]" @current-change="handlePageChange" @size-change="handlePageSizeChange" />
      </div>
    </el-card>

  </AppLayout>
</template>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.page-header p { margin: 6px 0 0; }
.table-panel,
.create-order-card {
  border-radius: 8px;
}
.create-order-card {
  margin-bottom: 16px;
}
.panel-title {
  margin: 0 0 18px;
  color: #111827;
  font-size: 18px;
  font-weight: 700;
}
.order-list-card :deep(.el-card__body) {
  padding: 20px 24px 0;
}
.create-order-card :deep(.el-card__body) {
  padding: 20px 24px 18px;
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
  grid-template-columns: auto auto auto auto auto;
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
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
.wide-control { width: 100%; }
.create-form { padding-top: 2px; }
.create-grid {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(260px, 1fr) minmax(280px, 1fr);
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
  gap: 7px;
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
.service-price { display: none; }
.amount {
  display: block;
  padding-top: 8px;
  color: #111827;
  font-size: 26px;
}
.amount-form-item :deep(.el-form-item__label::before),
.remark-form-item :deep(.el-form-item__label::before) {
  content: '*';
  visibility: hidden;
  margin-right: 4px;
}
.create-form :deep(.el-textarea__inner) {
  min-height: 74px !important;
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
