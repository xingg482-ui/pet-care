<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { fetchCustomers } from '../api/customers'
import { fetchCustomerPets } from '../api/pets'
import {
  cancelBoardingOrder,
  changeBoardingOrderLocation,
  checkInBoardingOrder,
  confirmBoardingOrderPickedUp,
  completeBoardingCareTask,
  createBoardingCareTasks,
  createBoardingArea,
  createBoardingLocation,
  createBoardingOrder,
  fetchBoardingAreas,
  fetchBoardingInHousePets,
  fetchBoardingLocations,
  fetchBoardingRoomStatus,
  updateBoardingCareTaskRemark,
  updateBoardingOrderCheckOutTime,
  updateBoardingOrderSchedule,
  updateBoardingArea,
  updateBoardingAreaStatus,
  updateBoardingLocation,
  updateBoardingLocationCleanStatus,
  updateBoardingLocationStatus,
} from '../api/boarding'

const loading = ref(false)
const route = useRoute()
const matrixLoading = ref(false)
const areaLoading = ref(false)
const careLoading = ref(false)
const saving = ref(false)
const tabNames = ['matrix', 'care', 'locations', 'areas']
const activeTab = ref(tabNames.includes(route.query.tab) ? route.query.tab : 'matrix')
const locationDialogVisible = ref(false)
const areaDialogVisible = ref(false)
const orderDialogVisible = ref(false)
const orderDetailVisible = ref(false)
const careTaskDialogVisible = ref(false)
const locationFormRef = ref()
const areaFormRef = ref()
const orderFormRef = ref()
const editingLocationId = ref(null)
const editingAreaId = ref(null)
const areas = ref([])
const records = ref([])
const allLocations = ref([])
const total = ref(0)
const customers = ref([])
const pets = ref([])
const careRows = ref([])
const careTaskPetOptions = ref([])
const selectedOrder = ref(null)
const draggedOrder = ref(null)
const suppressMatrixClick = ref(false)
const detailSaving = ref(false)
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

const today = new Date()
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
  areaId: '',
  code: '',
  locationType: '',
  petSpecies: '',
  petSize: '',
  status: '',
  cleanStatus: '',
  page: 1,
  pageSize: 10,
})

const careQuery = reactive({
  date: formatDate(today),
  areaId: '',
  keyword: '',
})

const locationForm = reactive({
  areaId: '',
  code: '',
  name: '',
  locationType: '房间',
  petSpecies: '狗',
  petSize: '通用',
  capacity: 1,
  pricePerDay: 0,
  costPerDay: 0,
  remark: '',
})

const areaForm = reactive({
  name: '',
  sortOrder: 0,
  remark: '',
})

const orderForm = reactive({
  customerId: '',
  petId: '',
  locationId: '',
  plannedCheckInTime: '',
  plannedCheckOutTime: '',
  totalAmount: 0,
  remark: '',
})

const detailForm = reactive({
  plannedCheckOutTime: '',
  locationId: '',
})

const careTaskForm = reactive({
  boardingOrderId: '',
  taskType: 'FEEDING',
  taskName: '进食',
  taskDate: formatDate(today),
  startTime: '09:00',
  intervalHours: 4,
  repeatCount: 1,
  remark: '',
})

const locationRules = {
  areaId: [{ required: true, message: '请选择所属区域', trigger: 'change' }],
  code: [{ required: true, message: '请输入位置编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入位置名称', trigger: 'blur' }],
  locationType: [{ required: true, message: '请选择位置类型', trigger: 'change' }],
  petSpecies: [{ required: true, message: '请选择适用宠物', trigger: 'change' }],
  petSize: [{ required: true, message: '请选择适用体型', trigger: 'change' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'change' }],
}

const areaRules = {
  name: [{ required: true, message: '请输入区域名称', trigger: 'blur' }],
}

const orderRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  locationId: [{ required: true, message: '请选择托管位置', trigger: 'change' }],
  plannedCheckInTime: [{ required: true, message: '请选择入住时间', trigger: 'change' }],
  plannedCheckOutTime: [{ required: true, message: '请选择退房时间', trigger: 'change' }],
}

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
  { label: '维护中', value: 'MAINTENANCE' },
]

const areaStatusOptions = statusOptions.slice(0, 2)
const cleanStatusOptions = [
  { label: '已清洁', value: 'CLEAN' },
  { label: '待清洁', value: 'DIRTY' },
  { label: '清洁中', value: 'CLEANING' },
]

const matrixStatusOptions = [
  { label: '全部位置', value: 'ALL' },
  { label: '期间有空档', value: 'AVAILABLE' },
  { label: '期间有占用', value: 'OCCUPIED' },
]

const typeOptions = ['房间', '笼位', '套间']
const speciesOptions = ['狗', '猫', '通用']
const sizeOptions = ['小型', '中型', '大型', '通用']
const careTaskTypeOptions = [
  { label: '进食', value: 'FEEDING', defaultTime: '09:00', intervalHours: 8 },
  { label: '喂水', value: 'WATER', defaultTime: '10:00', intervalHours: 4 },
  { label: '洗护', value: 'GROOMING', defaultTime: '15:00', intervalHours: 24 },
  { label: '户外活动', value: 'OUTDOOR', defaultTime: '11:00', intervalHours: 6 },
  { label: '健康观察', value: 'OBSERVE', defaultTime: '20:00', intervalHours: 12 },
]

const areaMap = computed(() => areas.value.reduce((map, area) => {
  map[area.id] = area.name
  return map
}, {}))

const enabledAreas = computed(() => areas.value.filter((area) => area.status === 'ENABLED'))
const enabledCleanLocations = computed(() => allLocations.value.filter((item) => item.status === 'ENABLED' && item.cleanStatus === 'CLEAN'))
const customerOptions = computed(() => customers.value.map((item) => ({
  label: `${item.name} / ${item.phone || '-'}`,
  value: item.id,
})))
const careSummary = computed(() => {
  const totalPets = careRows.value.length
  const totalTasks = careRows.value.reduce((sum, row) => sum + Number(row.totalTaskCount || 0), 0)
  const pendingTasks = careRows.value.reduce((sum, row) => sum + Number(row.pendingTaskCount || 0), 0)
  const completedTasks = careRows.value.reduce((sum, row) => sum + Number(row.completedTaskCount || 0), 0)
  return { totalPets, totalTasks, pendingTasks, completedTasks }
})

watch(() => orderForm.customerId, async (customerId) => {
  orderForm.petId = ''
  pets.value = []
  if (customerId) {
    const result = await fetchCustomerPets(customerId, true)
    pets.value = result.records
  }
})

watch(
  () => [orderForm.locationId, orderForm.plannedCheckInTime, orderForm.plannedCheckOutTime],
  () => {
    orderForm.totalAmount = estimateOrderAmount()
  },
)

watch(
  () => route.query.tab,
  (tab) => {
    if (tabNames.includes(tab)) {
      activeTab.value = tab
    }
  },
)

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

function optionLabel(options, value) {
  return options.find((item) => item.value === value)?.label || value || '-'
}

function orderStatusLabel(value) {
  const map = {
    RESERVED: '已预约',
    CHECKED_IN: '已入住',
    CANCELLED: '已取消',
    COMPLETED: '已接回',
  }
  return map[value] || value || '-'
}

function isPastDateTime(value) {
  if (!value) {
    return false
  }
  const date = new Date(String(value).replace(' ', 'T'))
  return Number.isFinite(date.getTime()) && date.getTime() <= Date.now()
}

function canConfirmSelectedOrderPickedUp() {
  return selectedOrder.value?.status === 'CHECKED_IN' && isPastDateTime(selectedOrder.value.plannedCheckOutTime)
}

function taskStatusLabel(value) {
  return value === 'DONE' ? '已完成' : '待处理'
}

function money(value) {
  return `￥${Number(value || 0).toFixed(2)}`
}

function chargeDays(startValue, endValue) {
  if (!startValue || !endValue) {
    return 0
  }
  const start = new Date(startValue)
  const end = new Date(endValue)
  const minutes = Math.max(1, (end.getTime() - start.getTime()) / 60000)
  if (Number.isNaN(minutes) || minutes <= 0) {
    return 0
  }
  return Math.max(1, Math.ceil(minutes / 1440))
}

function estimateOrderAmount() {
  const location = allLocations.value.find((item) => item.id === orderForm.locationId)
  const days = chargeDays(orderForm.plannedCheckInTime, orderForm.plannedCheckOutTime)
  return Number(((Number(location?.pricePerDay || 0)) * days).toFixed(2))
}

function dateOnly(value) {
  return String(value || '').slice(0, 10)
}

function timeOnly(value) {
  return String(value || '').slice(11, 16)
}

function addDays(value, days) {
  const date = new Date(`${value}T00:00:00`)
  date.setDate(date.getDate() + days)
  return formatDate(date)
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

function cellText(cell) {
  if (cell.status === 'AVAILABLE') {
    return ''
  }
  if (cell.status === 'UNAVAILABLE') {
    return '不可用'
  }
  if (cell.status === 'NEEDS_CLEANING') {
    return '待清洁'
  }
  return cell.order?.petName || '已占用'
}

function cellSubText(cell) {
  if (cell.order) {
    return `${cell.order.customerName} / ${orderStatusLabel(cell.order.status)}`
  }
  return ''
}

function cellTimeText(cell) {
  if (!cell.order) {
    return ''
  }
  const isStart = cell.date === dateOnly(cell.order.plannedCheckInTime)
  const isEnd = cell.date === dateOnly(cell.order.plannedCheckOutTime)
  if (isStart && isEnd) {
    return `${timeOnly(cell.order.plannedCheckInTime)} 入住 / ${timeOnly(cell.order.plannedCheckOutTime)} 退房`
  }
  if (isStart) {
    return `${timeOnly(cell.order.plannedCheckInTime)} 入住`
  }
  if (isEnd) {
    return `${timeOnly(cell.order.plannedCheckOutTime)} 退房`
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
  return draggedOrder.value?.id === order?.id ? '预约已更改' : order?.petName || '已预约'
}

function bookingBarMeta(order) {
  if (!order) {
    return ''
  }
  return `${order.customerName} · 入住 ${timeOnly(order.plannedCheckInTime)} · 退房 ${timeOnly(order.plannedCheckOutTime)}`
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

async function loadBaseData() {
  const [areaResult, customerResult] = await Promise.all([
    fetchBoardingAreas(),
    fetchCustomers({ status: 'ENABLED', page: 1, pageSize: 100 }),
  ])
  areas.value = areaResult
  customers.value = customerResult.records
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

async function loadAreas() {
  areaLoading.value = true
  try {
    areas.value = await fetchBoardingAreas()
  } finally {
    areaLoading.value = false
  }
}

async function loadLocations() {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.areaId) {
      delete params.areaId
    }
    const result = await fetchBoardingLocations(params)
    records.value = result.records
    total.value = result.total
    query.page = result.page
    query.pageSize = result.pageSize
  } finally {
    loading.value = false
  }
}

async function loadAllLocations() {
  const result = await fetchBoardingLocations({ page: 1, pageSize: 100 })
  allLocations.value = result.records
}

async function loadCareBoard() {
  careLoading.value = true
  try {
    const params = { ...careQuery }
    if (!params.areaId) {
      delete params.areaId
    }
    if (!params.keyword) {
      delete params.keyword
    }
    careRows.value = await fetchBoardingInHousePets(params)
    careTaskPetOptions.value = careRows.value
  } finally {
    careLoading.value = false
  }
}

async function loadCareTaskPetOptions() {
  const params = { date: careQuery.date || formatDate(new Date()) }
  careTaskPetOptions.value = await fetchBoardingInHousePets(params)
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
  Object.assign(query, {
    areaId: '',
    code: '',
    locationType: '',
    petSpecies: '',
    petSize: '',
    status: '',
    cleanStatus: '',
    page: 1,
    pageSize: query.pageSize,
  })
  loadLocations()
}

function resetLocationForm() {
  Object.assign(locationForm, {
    areaId: enabledAreas.value[0]?.id || '',
    code: '',
    name: '',
    locationType: '房间',
    petSpecies: '狗',
    petSize: '通用',
    capacity: 1,
    pricePerDay: 0,
    costPerDay: 0,
    remark: '',
  })
}

function resetAreaForm() {
  Object.assign(areaForm, {
    name: '',
    sortOrder: areas.value.length * 10 + 10,
    remark: '',
  })
}

function resetOrderForm() {
  Object.assign(orderForm, {
    customerId: '',
    petId: '',
    locationId: enabledCleanLocations.value[0]?.id || '',
    plannedCheckInTime: '',
    plannedCheckOutTime: '',
    totalAmount: 0,
    remark: '',
  })
  pets.value = []
}

function openCreateLocationDialog() {
  editingLocationId.value = null
  resetLocationForm()
  locationDialogVisible.value = true
}

function openEditLocationDialog(row) {
  editingLocationId.value = row.id
  Object.assign(locationForm, {
    areaId: row.areaId,
    code: row.code,
    name: row.name,
    locationType: row.locationType,
    petSpecies: row.petSpecies,
    petSize: row.petSize,
    capacity: row.capacity,
    pricePerDay: row.pricePerDay || 0,
    costPerDay: row.costPerDay || 0,
    remark: row.remark,
  })
  locationDialogVisible.value = true
}

function openCreateAreaDialog() {
  editingAreaId.value = null
  resetAreaForm()
  areaDialogVisible.value = true
}

function openEditAreaDialog(row) {
  editingAreaId.value = row.id
  Object.assign(areaForm, {
    name: row.name,
    sortOrder: row.sortOrder,
    remark: row.remark,
  })
  areaDialogVisible.value = true
}

function openCreateOrderDialog(row, cell) {
  if (suppressMatrixClick.value) {
    suppressMatrixClick.value = false
    return
  }
  if (cell && cell.status !== 'AVAILABLE') {
    if (cell.status === 'NEEDS_CLEANING') {
      cleanLocation(row)
      return
    }
    showOrderDetail(cell.order, cell.status)
    return
  }
  resetOrderForm()
  if (row) {
    orderForm.locationId = row.locationId
  }
  if (cell) {
    orderForm.plannedCheckInTime = `${cell.date} 10:00:00`
    const next = new Date(`${cell.date}T00:00:00`)
    next.setDate(next.getDate() + 1)
    orderForm.plannedCheckOutTime = `${formatDate(next)} 18:00:00`
  }
  orderDialogVisible.value = true
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
  resetOrderForm()
  orderForm.locationId = row.locationId
  orderForm.plannedCheckInTime = `${dates[0]} 10:00:00`
  orderForm.plannedCheckOutTime = `${addDays(dates[dates.length - 1], 1)} 18:00:00`
  orderDialogVisible.value = true
}

function clearMatrixSelection() {
  matrixSelection.active = false
  matrixSelection.rowId = null
  matrixSelection.startDate = ''
  matrixSelection.endDate = ''
}

function startOrderDrag(order, event) {
  if (!order || !['RESERVED', 'CHECKED_IN'].includes(order.status)) {
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
    await updateBoardingOrderSchedule(order.id, {
      locationId: Number(row.locationId),
      plannedCheckInTime: formatDateTime(targetStart),
      plannedCheckOutTime: formatDateTime(targetEnd),
    })
    ElMessage.success('托管预约时间已调整')
    await loadLocations()
    await loadAllLocations()
    loadMatrix()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '目标时段不可用')
  }
}

function showOrderDetail(order, status) {
  if (!order) {
    ElMessage.info(cellText({ status }))
    return
  }
  selectedOrder.value = order
  detailForm.plannedCheckOutTime = order.plannedCheckOutTime
  detailForm.locationId = order.locationId
  orderDetailVisible.value = true
}

async function cleanLocation(row) {
  if (!row) {
    return
  }
  await ElMessageBox.confirm(`确认 ${row.locationCode} 已清洁完成，并恢复为空闲可预约？`, '清洁完成', {
    type: 'warning',
    confirmButtonText: '确认完成',
    cancelButtonText: '取消',
  })
  await updateBoardingLocationCleanStatus(row.locationId, 'CLEAN')
  ElMessage.success('清洁状态已恢复为空闲')
  await loadLocations()
  await loadAllLocations()
  loadMatrix()
}

async function refreshAfterOrderAction(message) {
  ElMessage.success(message)
  orderDetailVisible.value = false
  selectedOrder.value = null
  await loadLocations()
  await loadAllLocations()
  loadMatrix()
}

async function checkInSelectedOrder() {
  if (!selectedOrder.value) {
    return
  }
  await ElMessageBox.confirm(`确认办理「${selectedOrder.value.petName}」入住？`, '办理入住', {
    type: 'warning',
    confirmButtonText: '办理入住',
    cancelButtonText: '取消',
  })
  detailSaving.value = true
  try {
    await checkInBoardingOrder(selectedOrder.value.id)
    await refreshAfterOrderAction('已办理入住')
  } finally {
    detailSaving.value = false
  }
}

async function confirmPickedUpSelectedOrder() {
  if (!selectedOrder.value) {
    return
  }
  await ElMessageBox.confirm(`确认「${selectedOrder.value.petName}」已被客户接回？确认后该托管单会从客户端未完成托管中清除，位置进入待清洁。`, '确认已接回', {
    type: 'warning',
    confirmButtonText: '确认已接回',
    cancelButtonText: '取消',
  })
  detailSaving.value = true
  try {
    await confirmBoardingOrderPickedUp(selectedOrder.value.id)
    await refreshAfterOrderAction('已确认接回，客户端未完成托管已清除')
  } finally {
    detailSaving.value = false
  }
}

async function cancelSelectedOrder() {
  if (!selectedOrder.value) {
    return
  }
  await ElMessageBox.confirm(`确认取消「${selectedOrder.value.petName}」的托管预约？`, '取消预约', {
    type: 'warning',
    confirmButtonText: '确认取消',
    cancelButtonText: '返回',
  })
  detailSaving.value = true
  try {
    await cancelBoardingOrder(selectedOrder.value.id)
    await refreshAfterOrderAction('托管预约已取消')
  } finally {
    detailSaving.value = false
  }
}

async function saveDetailCheckOutTime() {
  if (!selectedOrder.value || !detailForm.plannedCheckOutTime) {
    ElMessage.warning('请选择预计退房时间')
    return
  }
  detailSaving.value = true
  try {
    await updateBoardingOrderCheckOutTime(selectedOrder.value.id, {
      plannedCheckOutTime: formatDateTime(detailForm.plannedCheckOutTime),
    })
    await refreshAfterOrderAction('预计退房时间已更新')
  } finally {
    detailSaving.value = false
  }
}

async function saveDetailLocation() {
  if (!selectedOrder.value || !detailForm.locationId) {
    ElMessage.warning('请选择托管位置')
    return
  }
  detailSaving.value = true
  try {
    await changeBoardingOrderLocation(selectedOrder.value.id, {
      locationId: Number(detailForm.locationId),
    })
    await refreshAfterOrderAction('托管位置已调整')
  } finally {
    detailSaving.value = false
  }
}

async function saveLocation() {
  await locationFormRef.value.validate()
  saving.value = true
  try {
    if (editingLocationId.value) {
      await updateBoardingLocation(editingLocationId.value, locationForm)
      ElMessage.success('托管位置已更新')
    } else {
      await createBoardingLocation(locationForm)
      ElMessage.success('托管位置已新增')
    }
    locationDialogVisible.value = false
    await loadLocations()
    await loadAllLocations()
    loadMatrix()
  } finally {
    saving.value = false
  }
}

async function saveArea() {
  await areaFormRef.value.validate()
  saving.value = true
  try {
    if (editingAreaId.value) {
      await updateBoardingArea(editingAreaId.value, areaForm)
      ElMessage.success('托管区域已更新')
    } else {
      await createBoardingArea(areaForm)
      ElMessage.success('托管区域已新增')
    }
    areaDialogVisible.value = false
    await loadAreas()
    loadLocations()
    loadAllLocations()
    loadMatrix()
  } finally {
    saving.value = false
  }
}

async function saveOrder() {
  await orderFormRef.value.validate()
  saving.value = true
  try {
    await createBoardingOrder({
      customerId: Number(orderForm.customerId),
      petId: Number(orderForm.petId),
      locationId: Number(orderForm.locationId),
      plannedCheckInTime: formatDateTime(orderForm.plannedCheckInTime),
      plannedCheckOutTime: formatDateTime(orderForm.plannedCheckOutTime),
      totalAmount: Number(orderForm.totalAmount || 0),
      remark: orderForm.remark,
    })
    ElMessage.success('托管预约已创建')
    orderDialogVisible.value = false
    await loadMatrix()
  } finally {
    saving.value = false
  }
}

async function changeLocationStatus(row, status) {
  await updateBoardingLocationStatus(row.id, status)
    ElMessage.success('位置状态已更新')
    await loadLocations()
    await loadAllLocations()
  loadMatrix()
}

async function changeCleanStatus(row, cleanStatus) {
  await updateBoardingLocationCleanStatus(row.id, cleanStatus)
  ElMessage.success('清洁状态已更新')
  await loadLocations()
  await loadAllLocations()
  loadMatrix()
}

async function changeAreaStatus(row, status) {
  await updateBoardingAreaStatus(row.id, status)
  ElMessage.success('区域状态已更新')
  await loadAreas()
  loadLocations()
  loadAllLocations()
  loadMatrix()
}

async function completeCareTask(task) {
  await completeBoardingCareTask(task.id)
  ElMessage.success('照护任务已完成')
  loadCareBoard()
}

async function editCareTaskRemark(task) {
  const { value } = await ElMessageBox.prompt('记录异常、喂食情况或观察备注', '照护备注', {
    inputValue: task.remark || '',
    inputType: 'textarea',
    inputPlaceholder: '如：食欲正常、轻微紧张、已额外补水',
    confirmButtonText: '保存',
    cancelButtonText: '取消',
  })
  await updateBoardingCareTaskRemark(task.id, { remark: value })
  ElMessage.success('照护备注已保存')
  loadCareBoard()
}

async function openCareTaskDialog(row) {
  if (!row && !careTaskPetOptions.value.length) {
    await loadCareTaskPetOptions()
  }
  const option = careTaskTypeOptions[0]
  Object.assign(careTaskForm, {
    boardingOrderId: row?.boardingOrderId || careTaskPetOptions.value[0]?.boardingOrderId || '',
    taskType: option.value,
    taskName: option.label,
    taskDate: careQuery.date || formatDate(new Date()),
    startTime: option.defaultTime,
    intervalHours: null,
    repeatCount: 1,
    remark: '',
  })
  careTaskDialogVisible.value = true
}

function changeCareTaskType(value) {
  const option = careTaskTypeOptions.find((item) => item.value === value)
  if (!option) {
    return
  }
  careTaskForm.taskName = option.label
  careTaskForm.startTime = option.defaultTime
  careTaskForm.intervalHours = option.intervalHours
}

function changeCareTaskRepeatCount(value) {
  if (Number(value) <= 1) {
    careTaskForm.intervalHours = null
    return
  }
  if (!careTaskForm.intervalHours) {
    const option = careTaskTypeOptions.find((item) => item.value === careTaskForm.taskType)
    careTaskForm.intervalHours = option?.intervalHours || 4
  }
}

async function saveCareTasks() {
  if (!careTaskForm.boardingOrderId) {
    ElMessage.warning('请选择在店宠物')
    return
  }
  if (Number(careTaskForm.repeatCount) > 1 && !careTaskForm.intervalHours) {
    ElMessage.warning('多次执行任务需要填写间隔小时')
    return
  }
  await createBoardingCareTasks({
    boardingOrderId: Number(careTaskForm.boardingOrderId),
    taskType: careTaskForm.taskType,
    taskName: careTaskForm.taskName,
    taskDate: careTaskForm.taskDate,
    startTime: careTaskForm.startTime,
    intervalHours: Number(careTaskForm.repeatCount) > 1 ? Number(careTaskForm.intervalHours) : null,
    repeatCount: Number(careTaskForm.repeatCount),
    remark: careTaskForm.remark,
  })
  ElMessage.success('照护任务已创建')
  careTaskDialogVisible.value = false
  loadCareBoard()
}

function handlePageChange(page) {
  query.page = page
  loadLocations()
}

function handleSizeChange(pageSize) {
  query.pageSize = pageSize
  query.page = 1
  loadLocations()
}

function refreshCurrentTab() {
  if (activeTab.value === 'matrix') {
    loadMatrix()
  } else if (activeTab.value === 'care') {
    loadCareBoard()
  } else {
    loadLocations()
  }
}

onMounted(async () => {
  if (tabNames.includes(route.query.tab)) {
    activeTab.value = route.query.tab
  }
  await loadBaseData()
  await loadLocations()
  await loadAllLocations()
  await loadMatrix()
  await loadCareBoard()
})
</script>

<template>
  <AppLayout>
    <div class="boarding-page">
      <div class="page-head">
        <div>
          <h1>宠物托管</h1>
        </div>
        <div class="head-actions">
          <el-button :icon="Refresh" @click="refreshCurrentTab">刷新</el-button>
          <el-button type="primary" class="primary-action" :icon="Plus" @click="openCreateOrderDialog()">新建托管预约</el-button>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="boarding-tabs">
        <el-tab-pane label="托管房态" name="matrix">
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
              <el-input v-model="matrixQuery.keyword" :prefix-icon="Search" clearable placeholder="搜索房号 / 宠物 / 主人 / 电话" @change="loadMatrix" />
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

          <section class="summary-strip">
            <div>
              <span>今日待入住</span>
              <strong>{{ matrix.summary.todayPendingCheckIn }}</strong>
            </div>
            <div>
              <span>今日待退房</span>
              <strong>{{ matrix.summary.todayPendingCheckOut }}</strong>
            </div>
            <div>
              <span>今日占用</span>
              <strong>{{ matrix.summary.todayOccupiedCapacity }}</strong>
            </div>
            <div>
              <span>期间可预约格</span>
              <strong>{{ matrix.summary.availableCapacity }}</strong>
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
                  :draggable="Boolean(cell.order)"
                  @mousedown="startMatrixSelection(row, cell, $event)"
                  @mouseenter="moveMatrixSelection(row, cell)"
                  @mouseup="finishMatrixSelection(row)"
                  @dragstart="startOrderDrag(cell.order, $event)"
                  @dragend="clearOrderDrag"
                  @dragover.prevent
                  @drop.prevent="dropOrderOnCell(row, cell)"
                  @click="openCreateOrderDialog(row, cell)"
                >
                  <div v-if="cell.order && isOrderStartCell(cell)" class="booking-bar" :style="bookingBarStyle(cell)">
                    <strong>{{ bookingBarTitle(cell.order) }}</strong>
                    <span>{{ bookingBarMeta(cell.order) }}</span>
                  </div>
                  <template v-else-if="!cell.order">
                    <span>{{ cellText(cell) }}</span>
                    <small>{{ cellSubText(cell) }}</small>
                  </template>
                </button>
              </template>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="照护看板" name="care">
          <section class="filter-panel matrix-filter">
            <div class="date-tools">
              <el-date-picker v-model="careQuery.date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" @change="loadCareBoard" />
              <el-input v-model="careQuery.keyword" :prefix-icon="Search" clearable placeholder="搜索宠物 / 主人 / 电话 / 房号" @change="loadCareBoard" />
              <el-select v-model="careQuery.areaId" clearable placeholder="全部区域" @change="loadCareBoard">
                <el-option v-for="area in areas" :key="area.id" :label="area.name" :value="area.id" />
              </el-select>
              <el-button :icon="Refresh" @click="loadCareBoard">刷新</el-button>
              <el-button type="primary" :icon="Plus" @click="openCareTaskDialog()">新建照护任务</el-button>
            </div>
          </section>

          <section class="summary-strip care-summary">
            <div>
              <span>在店宠物</span>
              <strong>{{ careSummary.totalPets }}</strong>
            </div>
            <div>
              <span>今日任务</span>
              <strong>{{ careSummary.totalTasks }}</strong>
            </div>
            <div>
              <span>已完成</span>
              <strong>{{ careSummary.completedTasks }}</strong>
            </div>
            <div>
              <span>未完成</span>
              <strong>{{ careSummary.pendingTasks }}</strong>
            </div>
          </section>

          <section v-loading="careLoading" class="care-board">
            <el-empty v-if="!careRows.length" description="暂无在店宠物" />
            <article v-for="row in careRows" v-else :key="row.boardingOrderId" class="care-card">
              <div class="care-pet">
                <div>
                  <h2>{{ row.petName }}</h2>
                  <p>
                    {{ row.customerName }} {{ row.customerPhone || '-' }} · {{ row.areaName }} / {{ row.locationCode }} ·
                    {{ row.actualCheckInTime ? `已入住 ${row.stayDays} 天` : '今日预约待入住' }}
                  </p>
                </div>
                <div :class="['care-progress', { urgent: row.pendingTaskCount > 0 }]">
                  <strong>{{ row.completedTaskCount }}/{{ row.totalTaskCount }}</strong>
                  <span>{{ row.pendingTaskCount > 0 ? `未完成 ${row.pendingTaskCount}` : '已全部完成' }}</span>
                </div>
              </div>
              <div class="task-grid">
                <div v-for="task in row.tasks" :key="task.id" :class="['task-item', { done: task.status === 'DONE' }]">
                  <div class="task-main">
                    <span>{{ task.taskTime }}</span>
                    <strong>{{ task.taskName }}</strong>
                    <small>{{ taskStatusLabel(task.status) }}</small>
                    <p v-if="task.remark">{{ task.remark }}</p>
                  </div>
                  <div class="task-actions">
                    <el-button v-if="task.status !== 'DONE'" size="small" type="primary" @click="completeCareTask(task)">完成</el-button>
                    <el-button size="small" @click="editCareTaskRemark(task)">备注</el-button>
                  </div>
                </div>
              </div>
            </article>
          </section>
        </el-tab-pane>

        <el-tab-pane label="位置管理" name="locations">
          <section class="filter-panel">
            <el-form :model="query" label-width="72px" class="filter-form">
              <el-form-item label="所属区域">
                <el-select v-model="query.areaId" clearable placeholder="全部区域">
                  <el-option v-for="area in areas" :key="area.id" :label="area.name" :value="area.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="位置编号">
                <el-input v-model="query.code" clearable placeholder="搜索编号" />
              </el-form-item>
              <el-form-item label="位置类型">
                <el-select v-model="query.locationType" clearable placeholder="全部类型">
                  <el-option v-for="item in typeOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item label="适用宠物">
                <el-select v-model="query.petSpecies" clearable placeholder="全部宠物">
                  <el-option v-for="item in speciesOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="query.status" clearable placeholder="全部状态">
                  <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="清洁状态">
                <el-select v-model="query.cleanStatus" clearable placeholder="全部清洁状态">
                  <el-option v-for="item in cleanStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item class="filter-actions">
                <el-button type="primary" :icon="Search" @click="query.page = 1; loadLocations()">查询</el-button>
                <el-button @click="resetQuery">重置</el-button>
              </el-form-item>
            </el-form>
          </section>
          <section class="table-card">
            <div class="table-toolbar">
              <div>
                <h2>位置列表</h2>
                <span>{{ total }} 个位置</span>
              </div>
              <el-button :icon="Plus" @click="openCreateLocationDialog">新增位置</el-button>
            </div>
            <el-table v-loading="loading" :data="records" class="boarding-table">
              <el-table-column prop="code" label="位置编号" width="92" />
              <el-table-column prop="name" label="位置名称" min-width="128" />
              <el-table-column label="所属区域" width="112">
                <template #default="{ row }">{{ areaMap[row.areaId] || '-' }}</template>
              </el-table-column>
              <el-table-column prop="locationType" label="类型" width="76" />
              <el-table-column label="适用宠物" width="104">
                <template #default="{ row }">{{ row.petSpecies }} / {{ row.petSize }}</template>
              </el-table-column>
              <el-table-column prop="capacity" label="容量" width="64" />
              <el-table-column label="日价" width="96">
                <template #default="{ row }">{{ money(row.pricePerDay) }}</template>
              </el-table-column>
              <el-table-column label="日成本" width="96">
                <template #default="{ row }">{{ money(row.costPerDay) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="92">
                <template #default="{ row }">
                  <span :class="['status-pill', `status-pill--${String(row.status).toLowerCase()}`]">{{ optionLabel(statusOptions, row.status) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="清洁状态" width="100">
                <template #default="{ row }">
                  <span :class="['status-pill', `status-pill--${String(row.cleanStatus).toLowerCase()}`]">{{ optionLabel(cleanStatusOptions, row.cleanStatus) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="updatedAt" label="更新时间" width="152" />
              <el-table-column label="操作" width="168">
                <template #default="{ row }">
                  <div class="row-actions">
                    <el-button link type="primary" :icon="Edit" @click="openEditLocationDialog(row)">编辑</el-button>
                    <el-dropdown trigger="click" @command="(status) => changeLocationStatus(row, status)">
                      <el-button link>状态</el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item v-for="item in statusOptions" :key="item.value" :command="item.value">{{ item.label }}</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                    <el-dropdown trigger="click" @command="(status) => changeCleanStatus(row, status)">
                      <el-button link>清洁</el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item v-for="item in cleanStatusOptions" :key="item.value" :command="item.value">{{ item.label }}</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-wrap">
              <el-pagination
                v-model:current-page="query.page"
                v-model:page-size="query.pageSize"
                :total="total"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                @current-change="handlePageChange"
                @size-change="handleSizeChange"
              />
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="区域管理" name="areas">
          <section class="table-card">
            <div class="table-toolbar">
              <div>
                <h2>区域列表</h2>
                <span>{{ areas.length }} 个区域</span>
              </div>
              <el-button type="primary" :icon="Plus" @click="openCreateAreaDialog">新增区域</el-button>
            </div>
            <el-table v-loading="areaLoading" :data="areas" class="boarding-table">
              <el-table-column prop="name" label="区域名称" min-width="140" />
              <el-table-column prop="sortOrder" label="排序" width="88" />
              <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
              <el-table-column label="状态" width="112">
                <template #default="{ row }">
                  <span :class="['status-pill', `status-pill--${String(row.status).toLowerCase()}`]">{{ optionLabel(areaStatusOptions, row.status) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <div class="row-actions">
                    <el-button link type="primary" :icon="Edit" @click="openEditAreaDialog(row)">编辑</el-button>
                    <el-dropdown trigger="click" @command="(status) => changeAreaStatus(row, status)">
                      <el-button link>状态</el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item v-for="item in areaStatusOptions" :key="item.value" :command="item.value">{{ item.label }}</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="orderDialogVisible" title="新建托管预约" width="620px">
      <el-form ref="orderFormRef" :model="orderForm" :rules="orderRules" label-width="108px" class="dialog-form">
        <el-form-item label="客户" prop="customerId">
          <el-select v-model="orderForm.customerId" filterable placeholder="请选择客户">
            <el-option v-for="item in customerOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="宠物" prop="petId">
          <el-select v-model="orderForm.petId" filterable placeholder="请先选择客户">
            <el-option v-for="item in pets" :key="item.id" :label="`${item.name} / ${item.species}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="托管位置" prop="locationId">
          <el-select v-model="orderForm.locationId" filterable placeholder="请选择托管位置">
            <el-option v-for="item in enabledCleanLocations" :key="item.id" :label="`${item.code} / ${item.name} / ${areaMap[item.areaId] || '-'}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划入住" prop="plannedCheckInTime">
          <el-date-picker v-model="orderForm.plannedCheckInTime" type="datetime" format="YYYY-MM-DD HH:mm:ss" placeholder="选择入住时间" />
        </el-form-item>
        <el-form-item label="计划退房" prop="plannedCheckOutTime">
          <el-date-picker v-model="orderForm.plannedCheckOutTime" type="datetime" format="YYYY-MM-DD HH:mm:ss" placeholder="选择退房时间" />
        </el-form-item>
        <el-form-item label="预估费用">
          <el-input-number v-model="orderForm.totalAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.remark" type="textarea" :rows="3" placeholder="补充托管说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveOrder">保存预约</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="orderDetailVisible" title="托管预约详情" width="660px" class="boarding-detail-dialog">
      <div v-if="selectedOrder" class="detail-panel">
        <div class="detail-grid">
          <div class="detail-item">
            <span>宠物</span>
            <strong>{{ selectedOrder.petName }}</strong>
          </div>
          <div class="detail-item">
            <span>客户</span>
            <strong>{{ selectedOrder.customerName }} {{ selectedOrder.customerPhone }}</strong>
          </div>
          <div class="detail-item">
            <span>当前位置</span>
            <strong>{{ selectedOrder.locationCode }}</strong>
          </div>
          <div class="detail-item">
            <span>状态</span>
            <strong>{{ orderStatusLabel(selectedOrder.status) }}</strong>
          </div>
          <div class="detail-item">
            <span>计划入住</span>
            <strong>{{ selectedOrder.plannedCheckInTime }}</strong>
          </div>
          <div class="detail-item">
            <span>计划退房</span>
            <strong>{{ selectedOrder.plannedCheckOutTime }}</strong>
          </div>
          <div class="detail-item">
            <span>实际入住</span>
            <strong>{{ selectedOrder.actualCheckInTime || '-' }}</strong>
          </div>
          <div class="detail-item">
            <span>实际退房</span>
            <strong>{{ selectedOrder.actualCheckOutTime || '-' }}</strong>
          </div>
          <div class="detail-item">
            <span>计费天数</span>
            <strong>{{ selectedOrder.chargeDays || 0 }} 天</strong>
          </div>
          <div class="detail-item">
            <span>价格快照</span>
            <strong>{{ money(selectedOrder.unitPrice) }} / 天</strong>
          </div>
          <div class="detail-item">
            <span>费用合计</span>
            <strong>{{ money(selectedOrder.totalAmount) }}</strong>
          </div>
          <div class="detail-item">
            <span>预计利润</span>
            <strong>{{ money(selectedOrder.totalProfit) }}</strong>
          </div>
        </div>

        <el-form :model="detailForm" label-width="108px" class="dialog-form detail-form">
          <el-form-item label="预计退房">
            <el-date-picker
              v-model="detailForm.plannedCheckOutTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm:ss"
              placeholder="调整预计退房时间"
            />
          </el-form-item>
          <el-form-item label="托管位置">
            <el-select v-model="detailForm.locationId" filterable placeholder="选择新的托管位置">
              <el-option v-for="item in enabledCleanLocations" :key="item.id" :label="`${item.code} / ${item.name} / ${areaMap[item.areaId] || '-'}`" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="detail-footer">
          <el-button @click="orderDetailVisible = false">关闭</el-button>
          <el-button v-if="selectedOrder?.status === 'RESERVED'" :loading="detailSaving" @click="cancelSelectedOrder">取消预约</el-button>
          <el-button :loading="detailSaving" @click="saveDetailLocation">保存换房</el-button>
          <el-button :loading="detailSaving" @click="saveDetailCheckOutTime">保存退房时间</el-button>
          <el-button v-if="selectedOrder?.status === 'RESERVED'" type="primary" :loading="detailSaving" @click="checkInSelectedOrder">办理入住</el-button>
          <el-button v-if="canConfirmSelectedOrderPickedUp()" type="primary" :loading="detailSaving" @click="confirmPickedUpSelectedOrder">确认已接回</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="careTaskDialogVisible" title="新建照护任务" width="560px">
      <el-form :model="careTaskForm" label-width="108px" class="dialog-form">
        <el-form-item label="在店宠物">
          <el-select v-model="careTaskForm.boardingOrderId" filterable placeholder="请选择在店宠物">
            <el-option
              v-for="row in careTaskPetOptions"
              :key="row.boardingOrderId"
              :label="`${row.petName} / ${row.customerName} / ${row.locationCode}${row.actualCheckInTime ? ' / 已入住' : ' / 已预约'}`"
              :value="row.boardingOrderId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任务类型">
          <el-select v-model="careTaskForm.taskType" @change="changeCareTaskType">
            <el-option v-for="item in careTaskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务名称">
          <el-input v-model="careTaskForm.taskName" placeholder="如 进食 / 喂水 / 洗护 / 户外活动" />
        </el-form-item>
        <el-form-item label="执行日期">
          <el-date-picker v-model="careTaskForm.taskDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-picker v-model="careTaskForm.startTime" format="HH:mm" value-format="HH:mm" />
        </el-form-item>
        <el-form-item label="执行次数">
          <el-input-number v-model="careTaskForm.repeatCount" :min="1" :max="12" @change="changeCareTaskRepeatCount" />
        </el-form-item>
        <el-form-item v-if="Number(careTaskForm.repeatCount) > 1" label="间隔小时">
          <el-input-number v-model="careTaskForm.intervalHours" :min="1" :max="24" />
        </el-form-item>
        <el-form-item label="照护说明">
          <el-input v-model="careTaskForm.remark" type="textarea" :rows="3" placeholder="补充喂食量、外出要求、注意事项" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="careTaskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCareTasks">创建任务</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="locationDialogVisible" :title="editingLocationId ? '编辑托管位置' : '新增托管位置'" width="560px">
      <el-form ref="locationFormRef" :model="locationForm" :rules="locationRules" label-width="96px" class="dialog-form">
        <el-form-item label="所属区域" prop="areaId">
          <el-select v-model="locationForm.areaId" placeholder="请选择区域">
            <el-option v-for="area in enabledAreas" :key="area.id" :label="area.name" :value="area.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="位置编号" prop="code"><el-input v-model="locationForm.code" placeholder="如 A01" /></el-form-item>
        <el-form-item label="位置名称" prop="name"><el-input v-model="locationForm.name" placeholder="如 A01 大型房" /></el-form-item>
        <el-form-item label="位置类型" prop="locationType">
          <el-select v-model="locationForm.locationType"><el-option v-for="item in typeOptions" :key="item" :label="item" :value="item" /></el-select>
        </el-form-item>
        <el-form-item label="适用宠物" prop="petSpecies">
          <el-select v-model="locationForm.petSpecies"><el-option v-for="item in speciesOptions" :key="item" :label="item" :value="item" /></el-select>
        </el-form-item>
        <el-form-item label="适用体型" prop="petSize">
          <el-select v-model="locationForm.petSize"><el-option v-for="item in sizeOptions" :key="item" :label="item" :value="item" /></el-select>
        </el-form-item>
        <el-form-item label="容量" prop="capacity"><el-input-number v-model="locationForm.capacity" :min="1" :max="20" /></el-form-item>
        <el-form-item label="每日价格">
          <el-input-number v-model="locationForm.pricePerDay" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="每日成本">
          <el-input-number v-model="locationForm.costPerDay" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="locationForm.remark" type="textarea" :rows="3" placeholder="补充位置说明" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="locationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLocation">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="areaDialogVisible" :title="editingAreaId ? '编辑托管区域' : '新增托管区域'" width="520px">
      <el-form ref="areaFormRef" :model="areaForm" :rules="areaRules" label-width="88px" class="dialog-form">
        <el-form-item label="区域名称" prop="name"><el-input v-model="areaForm.name" placeholder="如 一楼东区" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="areaForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="areaForm.remark" type="textarea" :rows="3" placeholder="补充区域说明" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="areaDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveArea">保存</el-button>
      </template>
    </el-dialog>
  </AppLayout>
</template>

<style scoped>
.boarding-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  color: #111827;
}

.page-head,
.head-actions,
.date-tools,
.status-filters,
.summary-strip,
.table-toolbar,
.row-actions {
  display: flex;
  align-items: center;
}

.page-head {
  justify-content: space-between;
  gap: 16px;
}

.page-head h1 {
  margin: 0;
  font-size: 24px;
  line-height: 32px;
  font-weight: 800;
}

.head-actions {
  gap: 8px;
}

.filter-panel,
.table-card,
.matrix-card,
.summary-strip,
.care-board {
  border: 1px solid #e6eaf0;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(17, 24, 39, 0.05);
}

.filter-panel {
  padding: 16px 16px 4px;
}

.matrix-filter {
  padding-bottom: 16px;
}

.date-tools {
  flex-wrap: wrap;
  gap: 10px;
}

.date-tools strong {
  min-width: 190px;
  font-size: 16px;
}

.date-tools .el-input {
  width: 330px;
}

.date-tools .el-select {
  width: 160px;
}

.status-filters {
  gap: 8px;
  margin-top: 12px;
}

.status-filters button {
  height: 34px;
  padding: 0 14px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: #fbfcfe;
  color: #475569;
  font-weight: 700;
  transition: background-color 180ms ease, border-color 180ms ease, color 180ms ease;
}

.status-filters button.active {
  color: #9a5b00;
  border-color: #f4c04d;
  background: #fff7df;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
}

.summary-strip div {
  padding: 14px 18px;
  border-right: 1px solid #edf0f5;
}

.summary-strip div:last-child {
  border-right: 0;
}

.summary-strip span {
  display: block;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.summary-strip strong {
  display: block;
  margin-top: 4px;
  color: #111827;
  font-size: 24px;
  line-height: 28px;
}

.care-summary div:last-child strong {
  color: #b45309;
}

.care-board {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
}

.care-card {
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #ffffff;
  overflow: hidden;
}

.care-pet {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-bottom: 1px solid #edf0f5;
  background: #fffdf6;
}

.care-pet h2 {
  margin: 0;
  color: #111827;
  font-size: 18px;
  line-height: 26px;
  font-weight: 800;
}

.care-pet p {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 13px;
}

.care-progress {
  min-width: 116px;
  text-align: right;
}

.care-progress strong,
.care-progress span {
  display: block;
}

.care-progress strong {
  color: #12805c;
  font-size: 20px;
  line-height: 24px;
}

.care-progress span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.care-progress.urgent strong {
  color: #b45309;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
}

.task-item {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 132px;
  padding: 12px;
  border-right: 1px solid #edf0f5;
  background: #ffffff;
}

.task-item:last-child {
  border-right: 0;
}

.task-item.done {
  background: #f8faf6;
}

.task-main span,
.task-main small {
  display: block;
  color: #8a96aa;
  font-size: 12px;
  font-weight: 700;
}

.task-main strong {
  display: block;
  margin: 5px 0 3px;
  color: #111827;
  font-size: 15px;
  line-height: 22px;
  font-weight: 800;
}

.task-main p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.task-item.done .task-main strong,
.task-item.done .task-main small {
  color: #12805c;
}

.task-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.matrix-card {
  overflow: auto;
}

.matrix-table {
  display: grid;
  grid-template-columns: 220px repeat(var(--days, 7), minmax(148px, 1fr));
  min-width: 1256px;
}

.matrix-table {
  --days: v-bind('matrix.days');
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

.matrix-cell--available {
  background: #ffffff;
}

.matrix-cell--available span {
  display: none;
}

.matrix-cell.is-selecting {
  background: #fff7df;
  box-shadow: inset 0 0 0 2px rgba(245, 173, 0, 0.38);
}

.matrix-cell.is-drop-target:hover {
  background: #fffaf0;
  box-shadow: inset 0 0 0 2px rgba(245, 173, 0, 0.36);
}

.matrix-cell--reserved {
  background: #ffffff;
}

.matrix-cell--reserved span {
  color: #9a5b00;
  font-weight: 800;
}

.matrix-cell--checked_in {
  background: #ffffff;
}

.matrix-cell--checked_in span {
  color: #8a5200;
  font-weight: 800;
}

.matrix-cell em {
  color: #b45309;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
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
  transition: background-color 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
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

.matrix-cell--needs_cleaning {
  background: #fff0ef;
  color: #b42318;
}

.matrix-cell--unavailable {
  background: #f4f6f8;
  color: #94a3b8;
}

.filter-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(220px, 1fr));
  column-gap: 16px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.filter-form :deep(.el-select),
.filter-form :deep(.el-input) {
  width: 100%;
}

.filter-actions :deep(.el-form-item__content) {
  justify-content: flex-end;
}

.boarding-tabs {
  --el-color-primary: #f0a500;
}

.boarding-tabs :deep(.el-tabs__header) {
  margin: 0 0 12px;
}

.boarding-tabs :deep(.el-tabs__item) {
  height: 38px;
  font-weight: 700;
  color: #64748b;
}

.boarding-tabs :deep(.el-tabs__item.is-active) {
  color: #111827;
}

.table-card {
  overflow: hidden;
}

.table-toolbar {
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border-bottom: 1px solid #edf0f5;
}

.table-toolbar h2 {
  margin: 0;
  font-size: 16px;
  line-height: 24px;
  font-weight: 800;
}

.table-toolbar span {
  display: block;
  margin-top: 2px;
  color: #8a96aa;
  font-size: 13px;
}

.boarding-table {
  --el-table-header-bg-color: #fbfcfe;
  --el-table-border-color: #edf0f5;
  --el-table-row-hover-bg-color: #fff8eb;
  font-size: 14px;
}

.boarding-table :deep(th.el-table__cell) {
  font-weight: 700;
  color: #475569;
}

.boarding-table :deep(.el-table__cell) {
  padding: 10px 0;
}

.row-actions {
  gap: 10px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 54px;
  height: 24px;
  padding: 0 8px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #f5f7fa;
  color: #64748b;
  font-size: 12px;
  line-height: 22px;
  font-weight: 700;
  white-space: nowrap;
}

.status-pill--enabled,
.status-pill--clean {
  color: #12805c;
  border-color: #b8ead8;
  background: #eaf8f1;
}

.status-pill--maintenance,
.status-pill--cleaning {
  color: #9a5b00;
  border-color: #f7d58d;
  background: #fff7df;
}

.status-pill--dirty {
  color: #b42318;
  border-color: #f2b8b5;
  background: #fff0ef;
}

.status-pill--disabled {
  color: #64748b;
  border-color: #dbe3ee;
  background: #f5f7fa;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px 16px;
  border-top: 1px solid #edf0f5;
}

.dialog-form :deep(.el-select),
.dialog-form :deep(.el-input),
.dialog-form :deep(.el-textarea) {
  width: 100%;
}

.detail-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
  padding: 14px;
  border: 1px solid #f2dfaa;
  border-radius: 8px;
  background: #fffdf6;
}

.detail-item {
  min-width: 0;
}

.detail-item span {
  display: block;
  margin-bottom: 4px;
  color: #8a96aa;
  font-size: 12px;
  font-weight: 700;
}

.detail-item strong {
  display: block;
  overflow: hidden;
  color: #111827;
  font-size: 14px;
  line-height: 20px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-form {
  padding: 2px 0 0;
}

.detail-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.detail-footer :deep(.el-button + .el-button) {
  margin-left: 0;
}

:deep(.el-button) {
  border-radius: 8px;
  transition: background-color 180ms ease, border-color 180ms ease, color 180ms ease, box-shadow 180ms ease, transform 180ms ease;
}

:deep(.el-button--primary) {
  --el-button-bg-color: #f5ad00;
  --el-button-border-color: #f5ad00;
  --el-button-hover-bg-color: #f8bd29;
  --el-button-hover-border-color: #f8bd29;
  --el-button-active-bg-color: #dc9700;
  --el-button-active-border-color: #dc9700;
  color: #ffffff !important;
  border-color: #f5ad00 !important;
  background: linear-gradient(180deg, #ffc53d 0%, #f5a900 100%) !important;
  box-shadow: 0 10px 22px rgba(245, 173, 0, 0.18);
}

.primary-action {
  min-width: 136px;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dfe5ee inset;
  transition: box-shadow 180ms ease, background-color 180ms ease;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover),
:deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused),
:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #f0a500 inset, 0 0 0 3px rgba(240, 165, 0, 0.14);
}

@media (max-width: 1180px) {
  .filter-form {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }

  .task-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .task-item:nth-child(2n) {
    border-right: 0;
  }
}

@media (max-width: 760px) {
  .page-head,
  .table-toolbar,
  .care-pet {
    align-items: flex-start;
    flex-direction: column;
  }

  .filter-form,
  .summary-strip,
  .detail-grid,
  .task-grid {
    grid-template-columns: 1fr;
  }

  .task-item {
    border-right: 0;
    border-bottom: 1px solid #edf0f5;
  }

  .head-actions {
    width: 100%;
  }
}
</style>
