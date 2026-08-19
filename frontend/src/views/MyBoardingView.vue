<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight, Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { fetchMyPets } from '../api/pets'
import {
  cancelMyBoardingOrder,
  createMyBoardingOrder,
  fetchBoardingAreas,
  fetchBoardingLocations,
  fetchBoardingRoomStatus,
  fetchMyBoardingOrders,
  payMyBoardingOrder,
  updateMyBoardingOrderSchedule,
} from '../api/boarding'

const loading = ref(false)
const matrixLoading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const records = ref([])
const total = ref(0)
const pets = ref([])
const locations = ref([])
const areas = ref([])
const activeTab = ref('calendar')
const suppressMatrixClick = ref(false)
const draggedOrder = ref(null)

const today = new Date()
const matrix = ref({
  startDate: '',
  endDate: '',
  days: 7,
  dates: [],
  summary: {
    todayPendingCheckIn: 0,
    todayPendingCheckOut: 0,
    todayOccupiedCapacity: 0,
    availableCapacity: 0,
  },
  rows: [],
})

const matrixQuery = reactive({
  startDate: formatDate(today),
  days: 7,
  areaId: '',
  keyword: '',
  status: 'ALL',
})

const matrixSelection = reactive({
  active: false,
  rowId: null,
  startDate: '',
  endDate: '',
})

const query = reactive({
  status: '',
  startDate: '',
  endDate: '',
  page: 1,
  pageSize: 10,
})

const form = reactive({
  customerId: 0,
  petId: null,
  locationId: null,
  plannedCheckInTime: '',
  plannedCheckOutTime: '',
  totalAmount: null,
  remark: '',
})

const rules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  locationId: [{ required: true, message: '请选择托管位置', trigger: 'change' }],
  plannedCheckInTime: [{ required: true, message: '请选择入住时间', trigger: 'change' }],
  plannedCheckOutTime: [{ required: true, message: '请选择离店时间', trigger: 'change' }],
}

const statusOptions = [
  { label: '已预约', value: 'RESERVED' },
  { label: '已入住', value: 'CHECKED_IN' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '已接回', value: 'COMPLETED' },
]

const matrixStatusOptions = [
  { label: '全部位置', value: 'ALL' },
  { label: '期间有空档', value: 'AVAILABLE' },
  { label: '期间有占用', value: 'OCCUPIED' },
]

const statusTypes = {
  RESERVED: 'warning',
  CHECKED_IN: 'primary',
  CANCELLED: 'info',
  COMPLETED: 'success',
}

const paymentStatusText = {
  UNPAID: '未支付',
  PAID: '已支付',
}

const paymentStatusTypes = {
  UNPAID: 'warning',
  PAID: 'success',
}

const selectedLocation = computed(() => locations.value.find((item) => item.id === form.locationId))
const estimatedDays = computed(() => {
  if (!form.plannedCheckInTime || !form.plannedCheckOutTime) return 0
  const start = new Date(form.plannedCheckInTime.replace(' ', 'T')).getTime()
  const end = new Date(form.plannedCheckOutTime.replace(' ', 'T')).getTime()
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return 0
  return Math.max(1, Math.ceil((end - start) / 86400000))
})
const estimatedAmount = computed(() => Number(selectedLocation.value?.pricePerDay || 0) * estimatedDays.value)

function pad(value) {
  return String(value).padStart(2, '0')
}

function formatDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function formatDateTime(value) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

function dateLabel(value) {
  const date = new Date(`${value}T00:00:00`)
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
  return `${date.getMonth() + 1}/${date.getDate()} ${week}`
}

function addDays(value, days) {
  const date = new Date(`${value}T00:00:00`)
  date.setDate(date.getDate() + days)
  return formatDate(date)
}

function dateOnly(value) {
  return String(value || '').slice(0, 10)
}

function timeOnly(value) {
  return String(value || '').slice(11, 16)
}

function statusName(value) {
  return statusOptions.find((item) => item.value === value)?.label || value
}

function orderStatusLabel(value) {
  return statusName(value)
}

function loadEverything() {
  loadOptions()
  loadOrders()
  loadMatrix()
}

async function loadOrders() {
  loading.value = true
  try {
    const result = await fetchMyBoardingOrders(query)
    records.value = result.records
    total.value = result.total
    query.page = result.page
    query.pageSize = result.pageSize
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [petResult, locationResult, areaResult] = await Promise.all([
    fetchMyPets(),
    fetchBoardingLocations({ status: 'ENABLED', cleanStatus: 'CLEAN', page: 1, pageSize: 100 }),
    fetchBoardingAreas(),
  ])
  pets.value = petResult.records
  locations.value = locationResult.records
  areas.value = areaResult.filter((area) => area.status === 'ENABLED')
}

async function loadMatrix() {
  matrixLoading.value = true
  try {
    const params = { ...matrixQuery }
    if (!params.areaId) {
      delete params.areaId
    }
    if (!params.keyword) {
      delete params.keyword
    }
    matrix.value = await fetchBoardingRoomStatus(params)
  } finally {
    matrixLoading.value = false
  }
}

function moveRange(offset) {
  const date = new Date(`${matrixQuery.startDate}T00:00:00`)
  date.setDate(date.getDate() + offset)
  matrixQuery.startDate = formatDate(date)
  loadMatrix()
}

function resetMatrixToToday() {
  matrixQuery.startDate = formatDate(new Date())
  loadMatrix()
}

function resetQuery() {
  Object.assign(query, { status: '', startDate: '', endDate: '', page: 1 })
  loadOrders()
}

function resetForm() {
  Object.assign(form, {
    customerId: 0,
    petId: null,
    locationId: null,
    plannedCheckInTime: '',
    plannedCheckOutTime: '',
    totalAmount: null,
    remark: '',
  })
}

function openCreateDialog(row, cell, dates = []) {
  if (suppressMatrixClick.value) {
    suppressMatrixClick.value = false
    return
  }
  if (cell && cell.status !== 'AVAILABLE') {
    ElMessage.info(cellDisplayText(cell))
    return
  }
  resetForm()
  if (row) {
    form.locationId = row.locationId
  }
  if (dates.length) {
    form.plannedCheckInTime = `${dates[0]} 10:00:00`
    form.plannedCheckOutTime = `${addDays(dates[dates.length - 1], 1)} 18:00:00`
  } else if (cell) {
    form.plannedCheckInTime = `${cell.date} 10:00:00`
    form.plannedCheckOutTime = `${addDays(cell.date, 1)} 18:00:00`
  }
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

function orderedSelectionDates() {
  if (!matrixSelection.startDate || !matrixSelection.endDate) {
    return []
  }
  return [matrixSelection.startDate, matrixSelection.endDate].sort()
}

function selectedDateRange() {
  const [start, end] = orderedSelectionDates()
  if (!start || !end) {
    return []
  }
  const dates = []
  const current = new Date(`${start}T00:00:00`)
  const last = new Date(`${end}T00:00:00`)
  while (current <= last) {
    dates.push(formatDate(current))
    current.setDate(current.getDate() + 1)
  }
  return dates
}

function isCellSelected(row, cell) {
  return matrixSelection.active && matrixSelection.rowId === row.locationId && selectedDateRange().includes(cell.date)
}

function isRangeAvailable(row, dates) {
  return dates.every((date) => row.cells.find((cell) => cell.date === date)?.status === 'AVAILABLE')
}

function startMatrixSelection(row, cell, event) {
  if (cell.status !== 'AVAILABLE' || event?.button !== 0) {
    return
  }
  matrixSelection.active = true
  matrixSelection.rowId = row.locationId
  matrixSelection.startDate = cell.date
  matrixSelection.endDate = cell.date
  suppressMatrixClick.value = false
}

function moveMatrixSelection(row, cell) {
  if (!matrixSelection.active || matrixSelection.rowId !== row.locationId) {
    return
  }
  matrixSelection.endDate = cell.date
  suppressMatrixClick.value = matrixSelection.startDate !== matrixSelection.endDate
}

function finishMatrixSelection(row) {
  if (!matrixSelection.active || matrixSelection.rowId !== row.locationId) {
    clearMatrixSelection()
    return
  }
  const dates = selectedDateRange()
  const shouldOpenDialog = dates.length > 1
  clearMatrixSelection()
  if (!shouldOpenDialog) {
    return
  }
  if (!isRangeAvailable(row, dates)) {
    ElMessage.warning('请选择连续空闲时段')
    return
  }
  resetForm()
  form.locationId = row.locationId
  form.plannedCheckInTime = `${dates[0]} 10:00:00`
  form.plannedCheckOutTime = `${addDays(dates[dates.length - 1], 1)} 18:00:00`
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

function clearMatrixSelection() {
  matrixSelection.active = false
  matrixSelection.rowId = null
  matrixSelection.startDate = ''
  matrixSelection.endDate = ''
}

function startOrderDrag(order, event) {
  if (!order || order.status !== 'RESERVED') {
    event?.preventDefault()
    return
  }
  draggedOrder.value = order
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', String(order.id))
}

function clearOrderDrag() {
  draggedOrder.value = null
}

function canDropOrder(row, cell) {
  return Boolean(draggedOrder.value && cell.status === 'AVAILABLE' && row.locationId)
}

async function dropOrderOnCell(row, cell) {
  if (!canDropOrder(row, cell)) {
    clearOrderDrag()
    return
  }
  const order = draggedOrder.value
  clearOrderDrag()
  const checkIn = new Date(order.plannedCheckInTime.replace(' ', 'T'))
  const checkOut = new Date(order.plannedCheckOutTime.replace(' ', 'T'))
  const targetStart = new Date(`${cell.date}T${timeOnly(order.plannedCheckInTime)}:00`)
  const targetEnd = new Date(targetStart.getTime() + (checkOut.getTime() - checkIn.getTime()))
  try {
    await updateMyBoardingOrderSchedule(order.id, {
      locationId: Number(row.locationId),
      plannedCheckInTime: formatDateTime(targetStart),
      plannedCheckOutTime: formatDateTime(targetEnd),
    })
    ElMessage.success('托管预约时间已调整')
    await Promise.all([loadOrders(), loadMatrix(), loadOptions()])
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '目标时段不可用')
  }
}

function cellDisplayText(cell) {
  if (cell.status === 'AVAILABLE') {
    return ''
  }
  if (cell.status === 'UNAVAILABLE') {
    return '暂不可用'
  }
  if (cell.status === 'NEEDS_CLEANING') {
    return '待清洁'
  }
  if (cell.order) {
    return `${cell.order.petName || '已预约'} · ${orderStatusLabel(cell.order.status)}`
  }
  return '已占用'
}

function cellSubText(cell) {
  if (cell.order) {
    return `${timeOnly(cell.order.plannedCheckInTime)} 入住 / ${timeOnly(cell.order.plannedCheckOutTime)} 离店`
  }
  if (cell.status === 'AVAILABLE') {
    return ''
  }
  return ''
}

function orderVisibleDates(order) {
  if (!order) {
    return []
  }
  return matrix.value.dates.filter((date) => {
    const dayStart = new Date(`${date}T00:00:00`)
    const dayEnd = new Date(`${date}T00:00:00`)
    dayEnd.setDate(dayEnd.getDate() + 1)
    const checkIn = new Date(order.plannedCheckInTime.replace(' ', 'T'))
    const checkOut = new Date(order.plannedCheckOutTime.replace(' ', 'T'))
    return checkIn < dayEnd && checkOut > dayStart
  })
}

function isOrderStartCell(cell) {
  return Boolean(cell.order && orderVisibleDates(cell.order)[0] === cell.date)
}

function bookingBarStyle(cell) {
  const span = Math.max(1, orderVisibleDates(cell.order).length)
  return { width: `calc(${span * 100}% + ${(span - 1)}px)` }
}

function bookingBarTitle(order) {
  return order?.petName || '已预约'
}

function bookingBarMeta(order) {
  if (!order) {
    return ''
  }
  return `${orderStatusLabel(order.status)} · 入住 ${timeOnly(order.plannedCheckInTime)} · 离店 ${timeOnly(order.plannedCheckOutTime)}`
}

function matrixCellClasses(row, cell) {
  return [
    'matrix-cell',
    `matrix-cell--${cell.status.toLowerCase()}`,
    {
      'is-selecting': isCellSelected(row, cell),
      'is-drop-target': draggedOrder.value && cell.status === 'AVAILABLE',
      'is-booking-start': isOrderStartCell(cell),
      'is-booking-continuation': cell.order && !isOrderStartCell(cell),
    },
  ]
}

async function saveBoarding() {
  await formRef.value.validate()
  saving.value = true
  try {
    await createMyBoardingOrder({
      ...form,
      totalAmount: null,
    })
    ElMessage.success('托管预约已提交')
    dialogVisible.value = false
    await Promise.all([loadOrders(), loadMatrix(), loadOptions()])
  } finally {
    saving.value = false
  }
}

async function cancelBoarding(row) {
  await ElMessageBox.confirm(`确定取消托管预约「${row.boardingNo}」吗？`, '取消托管', { type: 'warning' })
  await cancelMyBoardingOrder(row.id)
  ElMessage.success('托管预约已取消')
  await Promise.all([loadOrders(), loadMatrix(), loadOptions()])
}

async function payBoarding(row) {
  await payMyBoardingOrder(row.id, { paymentMethod: 'MOCK' })
  ElMessage.success('支付成功')
  await Promise.all([loadOrders(), loadMatrix(), loadOptions()])
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

onMounted(loadEverything)
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h1 class="page-title">宠物托管</h1>
        <p class="muted">为自己的宠物预约托管位置</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog()">新增托管</el-button>
    </div>

    <el-tabs v-model="activeTab" class="boarding-tabs">
      <el-tab-pane label="托管房态" name="calendar">
        <section class="filter-panel matrix-filter">
          <div class="date-tools">
            <el-button @click="resetMatrixToToday">今天</el-button>
            <el-button :icon="ArrowLeft" @click="moveRange(-matrixQuery.days)" />
            <el-button :icon="ArrowRight" @click="moveRange(matrixQuery.days)" />
            <strong>{{ matrix.startDate }} - {{ matrix.endDate }}</strong>
            <el-radio-group v-model="matrixQuery.days" @change="loadMatrix">
              <el-radio-button :value="7">7天</el-radio-button>
              <el-radio-button :value="14">14天</el-radio-button>
            </el-radio-group>
            <el-input v-model="matrixQuery.keyword" :prefix-icon="Search" clearable placeholder="搜索房号 / 宠物" @change="loadMatrix" />
            <el-select v-model="matrixQuery.areaId" clearable placeholder="全部区域" @change="loadMatrix">
              <el-option v-for="area in areas" :key="area.id" :label="area.name" :value="area.id" />
            </el-select>
          </div>
          <div class="status-filters">
            <button
              v-for="item in matrixStatusOptions"
              :key="item.value"
              :class="{ active: matrixQuery.status === item.value }"
              type="button"
              @click="matrixQuery.status = item.value; loadMatrix()"
            >
              {{ item.label }}
            </button>
          </div>
        </section>

        <section v-loading="matrixLoading" class="matrix-card">
          <div class="matrix-table">
            <div class="matrix-header room-head">区域 / 房间</div>
            <div v-for="date in matrix.dates" :key="date" class="matrix-header day-head">
              <strong>{{ dateLabel(date) }}</strong>
              <span>{{ date === formatDate(new Date()) ? '今天' : date.slice(5) }}</span>
            </div>
            <template v-for="row in matrix.rows" :key="row.locationId">
              <div class="room-cell">
                <strong>{{ row.locationCode }}</strong>
                <span>{{ row.areaName }} · {{ row.locationType }} · {{ row.petSize }} · {{ row.capacity }}只</span>
              </div>
              <button
                v-for="cell in row.cells"
                :key="`${row.locationId}-${cell.date}`"
                :class="matrixCellClasses(row, cell)"
                type="button"
                :draggable="Boolean(cell.order && cell.order.status === 'RESERVED')"
                @mousedown="startMatrixSelection(row, cell, $event)"
                @mouseenter="moveMatrixSelection(row, cell)"
                @mouseup="finishMatrixSelection(row)"
                @dragstart="startOrderDrag(cell.order, $event)"
                @dragend="clearOrderDrag"
                @dragover.prevent
                @drop.prevent="dropOrderOnCell(row, cell)"
                @click="openCreateDialog(row, cell)"
              >
                <div v-if="cell.order && isOrderStartCell(cell)" class="booking-bar" :style="bookingBarStyle(cell)">
                  <strong>{{ bookingBarTitle(cell.order) }}</strong>
                  <span>{{ bookingBarMeta(cell.order) }}</span>
                </div>
                <template v-else-if="!cell.order">
                  <span>{{ cellDisplayText(cell) }}</span>
                  <small>{{ cellSubText(cell) }}</small>
                </template>
              </button>
            </template>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="我的预约" name="orders">
        <el-card shadow="never" class="filter-panel">
          <el-form :inline="true" :model="query">
            <el-form-item label="状态">
              <el-select v-model="query.status" clearable placeholder="全部状态" class="status-select">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="入住日期">
              <el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="离店日期">
              <el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="query.page = 1; loadOrders()">查询</el-button>
              <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-panel">
          <el-table v-loading="loading" :data="records" border>
            <el-table-column prop="boardingNo" label="托管编号" min-width="180" />
            <el-table-column prop="petName" label="宠物" min-width="120" />
            <el-table-column label="托管位置" min-width="180">
              <template #default="{ row }">{{ row.areaName }} · {{ row.locationCode }}</template>
            </el-table-column>
            <el-table-column prop="plannedCheckInTime" label="入住时间" min-width="170" />
            <el-table-column prop="plannedCheckOutTime" label="离店时间" min-width="170" />
            <el-table-column prop="totalAmount" label="金额" width="110" />
            <el-table-column prop="paymentStatus" label="支付状态" width="110">
              <template #default="{ row }">
                <el-tag :type="paymentStatusTypes[row.paymentStatus || 'UNPAID']">
                  {{ paymentStatusText[row.paymentStatus || 'UNPAID'] || row.paymentStatus }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }"><el-tag :type="statusTypes[row.status]">{{ statusName(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'RESERVED'" link type="warning" @click="cancelBoarding(row)">取消</el-button>
                <el-button v-if="row.status === 'COMPLETED' && (row.paymentStatus || 'UNPAID') !== 'PAID'" link type="primary" @click="payBoarding(row)">支付</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-bar">
            <el-pagination background layout="total, sizes, prev, pager, next" :total="total" :current-page="query.page" :page-size="query.pageSize" :page-sizes="[10, 20, 50]" @current-change="handlePageChange" @size-change="handlePageSizeChange" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" title="新增托管预约" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px" class="dialog-form">
        <el-form-item label="宠物" prop="petId">
          <el-select v-model="form.petId" class="form-control" placeholder="请选择宠物">
            <el-option v-for="pet in pets" :key="pet.id" :label="pet.name" :value="pet.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="托管位置" prop="locationId">
          <el-select v-model="form.locationId" class="form-control" placeholder="请选择可用位置">
            <el-option
              v-for="location in locations"
              :key="location.id"
              :label="`${location.code} / ${location.name} / ${location.areaName || '-'}`"
              :value="location.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="计划入住" prop="plannedCheckInTime">
          <el-date-picker v-model="form.plannedCheckInTime" type="datetime" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" class="form-control" placeholder="选择入住时间" />
        </el-form-item>
        <el-form-item label="计划退房" prop="plannedCheckOutTime">
          <el-date-picker v-model="form.plannedCheckOutTime" type="datetime" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" class="form-control" placeholder="选择退房时间" />
        </el-form-item>
        <el-form-item label="预估费用">
          <el-input-number :model-value="estimatedAmount" :min="0" :precision="2" disabled />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="补充托管说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBoarding">保存预约</el-button>
      </template>
    </el-dialog>
  </AppLayout>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header p {
  margin: 6px 0 0;
}

.boarding-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.boarding-tabs :deep(.el-tabs__item) {
  height: 38px;
  color: #64748b;
  font-weight: 700;
}

.boarding-tabs :deep(.el-tabs__item.is-active) {
  color: #111827;
}

.filter-panel {
  margin-bottom: 16px;
  border-radius: 8px;
}

.table-panel {
  border-radius: 8px;
}

.matrix-filter {
  padding: 16px;
  border: 1px solid #edf0f5;
  background: #ffffff;
}

.date-tools {
  display: grid;
  grid-template-columns: auto auto auto minmax(180px, 1fr) auto minmax(220px, 1fr) 180px;
  align-items: center;
  gap: 10px;
}

.date-tools strong {
  color: #111827;
  font-size: 14px;
}

.status-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.status-filters button {
  height: 30px;
  padding: 0 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #ffffff;
  color: #64748b;
  font-weight: 700;
  cursor: pointer;
}

.status-filters button.active {
  border-color: #f5ad00;
  background: #fff7df;
  color: #9a5b00;
}

.matrix-card {
  overflow: auto;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #ffffff;
}

.matrix-table {
  --days: v-bind('matrix.days');
  display: grid;
  grid-template-columns: 220px repeat(var(--days, 7), minmax(148px, 1fr));
  min-width: 1256px;
}

.matrix-header,
.room-cell,
.matrix-cell {
  min-height: 76px;
  border-right: 1px solid #edf0f5;
  border-bottom: 1px solid #edf0f5;
}

.matrix-header {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 12px 16px;
  background: #fbfcfe;
}

.day-head strong {
  font-size: 14px;
}

.day-head span,
.room-cell span,
.matrix-cell small {
  color: #8a96aa;
  font-size: 12px;
}

.room-cell {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 12px 16px;
  background: #ffffff;
}

.room-cell strong {
  margin-bottom: 5px;
  color: #111827;
  font-size: 16px;
}

.matrix-cell {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  padding: 10px 12px;
  border-top: 0;
  border-left: 0;
  background: #ffffff;
  color: #334155;
  text-align: left;
  user-select: none;
  cursor: pointer;
  transition: background-color 180ms ease, box-shadow 180ms ease;
}

.matrix-cell:hover {
  box-shadow: inset 0 0 0 2px rgba(245, 173, 0, 0.24);
}

.matrix-cell[draggable='true'] {
  cursor: grab;
}

.matrix-cell[draggable='true']:active {
  cursor: grabbing;
}

.matrix-cell--available span {
  color: #12805c;
  font-weight: 800;
}

.matrix-cell.is-selecting {
  background: #fff7df;
  box-shadow: inset 0 0 0 2px rgba(245, 173, 0, 0.38);
}

.matrix-cell.is-drop-target:hover {
  background: #fffaf0;
  box-shadow: inset 0 0 0 2px rgba(245, 173, 0, 0.36);
}

.matrix-cell--reserved,
.matrix-cell--checked_in {
  background: #ffffff;
}

.matrix-cell--needs_cleaning {
  background: #fff0ef;
  color: #b42318;
}

.matrix-cell--unavailable {
  background: #f4f6f8;
  color: #94a3b8;
  cursor: default;
}

.matrix-cell.is-booking-continuation {
  pointer-events: auto;
}

.booking-bar {
  position: absolute;
  z-index: 2;
  inset: 10px auto 10px 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  min-width: 0;
  padding: 10px 14px;
  border: 1px solid #f4c04d;
  border-left: 3px solid #f5ad00;
  border-radius: 8px;
  background: #fff6df;
  box-shadow: 0 8px 18px rgba(154, 91, 0, 0.12);
  color: #9a5b00;
  text-align: left;
}

.booking-bar strong,
.booking-bar span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.booking-bar strong {
  color: #9a5b00;
  font-size: 14px;
  line-height: 18px;
  font-weight: 800;
}

.booking-bar span {
  color: #6b4b16;
  font-size: 12px;
  line-height: 16px;
  font-weight: 700;
}

.matrix-cell--checked_in .booking-bar {
  background: #fff9eb;
  border-color: #efbd50;
  border-left-color: #f0a500;
}

.matrix-cell[draggable='true']:hover .booking-bar {
  box-shadow: 0 10px 22px rgba(154, 91, 0, 0.16);
}

.status-select {
  width: 140px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.form-control {
  width: 100%;
}

.fee-note {
  margin-left: 8px;
  color: var(--pc-muted);
}

@media (max-width: 900px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .date-tools {
    grid-template-columns: repeat(3, auto);
  }

  .date-tools strong,
  .date-tools :deep(.el-radio-group),
  .date-tools :deep(.el-input),
  .date-tools :deep(.el-select) {
    grid-column: 1 / -1;
    width: 100%;
  }

}
</style>
