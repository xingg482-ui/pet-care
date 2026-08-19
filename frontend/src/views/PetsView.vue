<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Delete as DeleteIcon, EditPen, FirstAidKit, Plus, Refresh, Search, SwitchButton, Upload } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import AvatarCropperDialog from '../components/AvatarCropperDialog.vue'
import { fetchCustomers } from '../api/customers'
import { fetchPetAvatarLibrary } from '../api/petAvatarLibrary'
import { createPet, deletePet, fetchPets, removePetAvatar, updatePet, updatePetStatus, uploadPetAvatar } from '../api/pets'
import { resolvePetAvatar } from '../data/petAvatarLibrary'
import { validateAvatarFile } from '../utils/avatarImage'
import {
  createDewormingRecord,
  createVaccineRecord,
  createWeightRecord,
  deleteDewormingRecord,
  deleteVaccineRecord,
  deleteWeightRecord,
  fetchHealthRecords,
  updateDewormingRecord,
  updateVaccineRecord,
  updateWeightRecord,
} from '../api/healthRecords'

const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const avatarInputRef = ref()
const editingId = ref(null)
const records = ref([])
const customers = ref([])
const total = ref(0)
const healthDialogVisible = ref(false)
const healthLoading = ref(false)
const healthSaving = ref(false)
const activeHealthTab = ref('vaccines')
const currentPet = ref(null)
const healthRecords = ref({ vaccines: [], dewormingRecords: [], weights: [] })
const healthFormRef = ref()
const healthEditingId = ref(null)
const healthFormType = ref('vaccines')
const healthFormVisible = ref(false)
const avatarFile = ref(null)
const avatarPreviewUrl = ref('')
const currentAvatarUrl = ref('')
const removeAvatarAfterSave = ref(false)
const avatarLibraryRecords = ref([])
const avatarDragging = ref(false)
const brokenAvatarUrls = ref(new Set())
const avatarCropperVisible = ref(false)
const avatarCropperFile = ref(null)
const restoredDefaultPetIds = ref(new Set())

const query = reactive({
  name: '',
  customerId: '',
  species: '',
  status: '',
  page: 1,
  pageSize: 10,
})

const form = reactive({
  customerId: '',
  name: '',
  species: '',
  breed: '',
  gender: 'UNKNOWN',
  birthday: '',
  weight: null,
  sterilized: false,
  remark: '',
})

const rules = {
  customerId: [{ required: true, message: '请选择所属客户', trigger: 'change' }],
  name: [{ required: true, message: '请输入宠物名称', trigger: 'blur' }],
  species: [{ required: true, message: '请输入宠物种类', trigger: 'blur' }],
}

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
]

const genderOptions = [
  { label: '公', value: 'MALE' },
  { label: '母', value: 'FEMALE' },
  { label: '未知', value: 'UNKNOWN' },
]

const dewormingTypeOptions = ['体内', '体外']

const healthForm = reactive({
  vaccineName: '',
  vaccinationDate: '',
  institution: '',
  nextVaccinationDate: '',
  dewormingType: '体内',
  medicineName: '',
  dewormingDate: '',
  nextDewormingDate: '',
  recordDate: '',
  weight: null,
  remark: '',
})

const healthRules = {
  vaccineName: [{ required: true, message: '请输入疫苗名称', trigger: 'blur' }],
  vaccinationDate: [{ required: true, message: '请选择接种日期', trigger: 'change' }],
  dewormingType: [{ required: true, message: '请选择驱虫类型', trigger: 'change' }],
  medicineName: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  dewormingDate: [{ required: true, message: '请选择驱虫日期', trigger: 'change' }],
  recordDate: [{ required: true, message: '请选择记录日期', trigger: 'change' }],
  weight: [{ required: true, message: '请输入体重', trigger: 'change' }],
}

const customerOptions = computed(() => customers.value.map((item) => ({
  label: `${item.name} / ${item.phone || '-'}`,
  value: item.id,
})))

function genderLabel(gender) {
  return gender === 'MALE' ? '公' : gender === 'FEMALE' ? '母' : '未知'
}

function petAvatar(row) {
  const species = `${row.species || ''}`.toLowerCase()
  if (species.includes('猫') || species.includes('cat')) {
    return '喵'
  }
  if (species.includes('狗') || species.includes('犬') || species.includes('dog')) {
    return '汪'
  }
  return (row.name || '宠').slice(0, 1)
}

function petAvatarSrc(row) {
  if (restoredDefaultPetIds.value.has(row.id)) {
    return resolveRuntimePetAvatar(row)
  }
  return firstAvailableAvatar([row.avatarUrl, resolveRuntimePetAvatar(row)])
}

function normalizeAssetUrl(url) {
  if (!url) {
    return ''
  }
  if (/^https?:\/\//.test(url)) {
    return url
  }
  return url.startsWith('/') ? url : `/${url}`
}

function formDefaultAvatarSrc() {
  return resolveRuntimePetAvatar(form)
}

function resolveRuntimePetAvatar(pet) {
  const breed = `${pet?.breed || ''}`.toLowerCase()
  const species = `${pet?.species || ''}`.toLowerCase()
  const petText = `${breed} ${species}`
  const libraryAvatar = avatarLibraryRecords.value.find((item) => {
    const keywords = `${item.breed || ''},${item.keywords || ''}`.split(/[,，]/)
    return keywords.some((keyword) => keyword.trim() && petText.includes(keyword.trim().toLowerCase()))
  })
  return normalizeAssetUrl(libraryAvatar?.avatarUrl) || resolvePetAvatar(pet)
}

function formAvatarSrc() {
  return firstAvailableAvatar([
    avatarPreviewUrl.value,
    !removeAvatarAfterSave.value ? currentAvatarUrl.value : '',
    formDefaultAvatarSrc(),
  ])
}

function formAvatarSourceLabel() {
  if (avatarPreviewUrl.value) {
    return '待保存头像'
  }
  if (currentAvatarUrl.value && !removeAvatarAfterSave.value) {
    return '上传头像'
  }
  if (formDefaultAvatarSrc()) {
    return '品种默认'
  }
  return '通用默认'
}

function revokeAvatarPreview() {
  if (avatarPreviewUrl.value) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
  }
  avatarPreviewUrl.value = ''
}

function resetAvatarState(row = null) {
  revokeAvatarPreview()
  avatarFile.value = null
  currentAvatarUrl.value = row?.avatarUrl || ''
  removeAvatarAfterSave.value = false
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
}

function openAvatarPicker() {
  avatarInputRef.value?.click()
}

function firstAvailableAvatar(urls) {
  return urls.map((url) => normalizeAssetUrl(url)).find((url) => url && !brokenAvatarUrls.value.has(url)) || ''
}

function markBrokenAvatar(url) {
  const normalizedUrl = normalizeAssetUrl(url)
  if (!normalizedUrl) {
    return
  }
  const nextBrokenUrls = new Set(brokenAvatarUrls.value)
  nextBrokenUrls.add(normalizedUrl)
  brokenAvatarUrls.value = nextBrokenUrls
}

async function applyAvatarFile(file) {
  try {
    if (!validateAvatarFile(file)) {
      return
    }
    avatarCropperFile.value = file
    avatarCropperVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '头像处理失败，请重新选择')
  }
}

function handleAvatarCropConfirm(result) {
  revokeAvatarPreview()
  avatarFile.value = result.file
  avatarPreviewUrl.value = result.previewUrl
    removeAvatarAfterSave.value = false
    avatarCropperFile.value = null
    if (editingId.value) {
      const nextRestoredIds = new Set(restoredDefaultPetIds.value)
      nextRestoredIds.delete(editingId.value)
      restoredDefaultPetIds.value = nextRestoredIds
    }
    ElMessage.success('头像效果已确认，保存宠物后生效')
}

function handleAvatarCropCancel() {
  avatarCropperFile.value = null
}

async function handleAvatarChange(event) {
  await applyAvatarFile(event.target.files?.[0])
  event.target.value = ''
}

async function handleAvatarDrop(event) {
  avatarDragging.value = false
  await applyAvatarFile(event.dataTransfer.files?.[0])
}

function handleAvatarDragLeave(event) {
  if (!event.currentTarget.contains(event.relatedTarget)) {
    avatarDragging.value = false
  }
}

function handleAvatarLoadError(url) {
  markBrokenAvatar(url)
  if (url === avatarPreviewUrl.value) {
    revokeAvatarPreview()
    avatarFile.value = null
  }
}

function removeSelectedAvatar() {
  revokeAvatarPreview()
  avatarFile.value = null
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
  if (currentAvatarUrl.value) {
    removeAvatarAfterSave.value = true
  }
}

function displayValue(value, suffix = '') {
  if (value === null || value === undefined || value === '') {
    return '未填写'
  }
  return `${value}${suffix}`
}

async function loadCustomers() {
  const result = await fetchCustomers({ status: 'ENABLED', page: 1, pageSize: 100 })
  customers.value = result.records
}

async function loadAvatarLibrary() {
  avatarLibraryRecords.value = await fetchPetAvatarLibrary({ status: 'ENABLED' }).catch(() => [])
}

async function loadPets() {
  loading.value = true
  try {
    const params = { ...query, customerId: query.customerId || undefined }
    const result = await fetchPets(params)
    records.value = result.records
    total.value = result.total
    query.page = result.page
    query.pageSize = result.pageSize
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.name = ''
  query.customerId = ''
  query.species = ''
  query.status = ''
  query.page = 1
  loadPets()
}

function resetForm() {
  Object.assign(form, {
    customerId: '',
    name: '',
    species: '',
    breed: '',
    gender: 'UNKNOWN',
    birthday: '',
    weight: null,
    sterilized: false,
    remark: '',
  })
  resetAvatarState()
}

function openCreateDialog() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  editingId.value = row.id
  Object.assign(form, {
    customerId: row.customerId,
    name: row.name,
    species: row.species,
    breed: row.breed,
    gender: row.gender || 'UNKNOWN',
    birthday: row.birthday,
    weight: row.weight,
    sterilized: Boolean(row.sterilized),
    remark: row.remark,
  })
  resetAvatarState(row)
  dialogVisible.value = true
}

async function savePet() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form, customerId: Number(form.customerId) }
    let savedPet
    if (editingId.value) {
      savedPet = await updatePet(editingId.value, payload)
      ElMessage.success('宠物已更新')
    } else {
      savedPet = await createPet(payload)
      ElMessage.success('宠物已新增')
    }
    const shouldRestoreDefaultAvatar = Boolean(savedPet?.id && removeAvatarAfterSave.value && !avatarFile.value)
    if (shouldRestoreDefaultAvatar) {
      savedPet = await removePetAvatar(savedPet.id)
      const nextRestoredIds = new Set(restoredDefaultPetIds.value)
      nextRestoredIds.add(savedPet.id)
      restoredDefaultPetIds.value = nextRestoredIds
    }
    if (savedPet?.id && avatarFile.value) {
      savedPet = await uploadPetAvatar(savedPet.id, avatarFile.value)
      ElMessage.success('宠物头像已保存')
    }
    if (savedPet?.id && avatarFile.value && savedPet.avatarUrl) {
      const nextRestoredIds = new Set(restoredDefaultPetIds.value)
      nextRestoredIds.delete(savedPet.id)
      restoredDefaultPetIds.value = nextRestoredIds
    }
    const recordIndex = records.value.findIndex((item) => item.id === savedPet?.id)
    if (recordIndex >= 0) {
      records.value.splice(recordIndex, 1, savedPet)
    }
    resetAvatarState()
    dialogVisible.value = false
    await loadPets()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const actionText = nextStatus === 'ENABLED' ? '启用' : '停用'
  await ElMessageBox.confirm(`确定${actionText}宠物「${row.name}」吗？`, '确认操作', { type: 'warning' })
  await updatePetStatus(row.id, nextStatus)
  ElMessage.success(`宠物已${actionText}`)
  loadPets()
}

async function handleDeletePet(row) {
  await ElMessageBox.confirm(`确定删除已停用宠物「${row.name}」吗？删除后不可恢复。`, '删除宠物', {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
  })
  await deletePet(row.id)
  ElMessage.success('宠物已删除')
  loadPets()
}

async function openHealthDialog(row) {
  currentPet.value = row
  healthDialogVisible.value = true
  activeHealthTab.value = 'vaccines'
  await loadHealthRecords()
}

async function loadHealthRecords() {
  if (!currentPet.value) {
    return
  }
  healthLoading.value = true
  try {
    healthRecords.value = await fetchHealthRecords(currentPet.value.id)
  } finally {
    healthLoading.value = false
  }
}

function resetHealthForm(type) {
  healthFormType.value = type
  healthEditingId.value = null
  Object.assign(healthForm, {
    vaccineName: '',
    vaccinationDate: '',
    institution: '',
    nextVaccinationDate: '',
    dewormingType: '体内',
    medicineName: '',
    dewormingDate: '',
    nextDewormingDate: '',
    recordDate: '',
    weight: null,
    remark: '',
  })
}

function openHealthForm(type, row = null) {
  resetHealthForm(type)
  if (row) {
    healthEditingId.value = row.id
    Object.assign(healthForm, row)
  }
  healthFormVisible.value = true
}

async function saveHealthRecord() {
  await healthFormRef.value.validate()
  healthSaving.value = true
  try {
    const petId = currentPet.value.id
    if (healthFormType.value === 'vaccines') {
      const payload = {
        vaccineName: healthForm.vaccineName,
        vaccinationDate: healthForm.vaccinationDate,
        institution: healthForm.institution,
        nextVaccinationDate: healthForm.nextVaccinationDate,
        remark: healthForm.remark,
      }
      healthEditingId.value ? await updateVaccineRecord(petId, healthEditingId.value, payload) : await createVaccineRecord(petId, payload)
    } else if (healthFormType.value === 'deworming') {
      const payload = {
        dewormingType: healthForm.dewormingType,
        medicineName: healthForm.medicineName,
        dewormingDate: healthForm.dewormingDate,
        nextDewormingDate: healthForm.nextDewormingDate,
        remark: healthForm.remark,
      }
      healthEditingId.value ? await updateDewormingRecord(petId, healthEditingId.value, payload) : await createDewormingRecord(petId, payload)
    } else {
      const payload = {
        recordDate: healthForm.recordDate,
        weight: Number(healthForm.weight),
        remark: healthForm.remark,
      }
      healthEditingId.value ? await updateWeightRecord(petId, healthEditingId.value, payload) : await createWeightRecord(petId, payload)
    }
    ElMessage.success('健康记录已保存')
    healthFormVisible.value = false
    loadHealthRecords()
  } finally {
    healthSaving.value = false
  }
}

async function deleteHealthRecord(type, row) {
  await ElMessageBox.confirm('确定删除这条健康记录吗？', '确认操作', { type: 'warning' })
  const petId = currentPet.value.id
  if (type === 'vaccines') {
    await deleteVaccineRecord(petId, row.id)
  } else if (type === 'deworming') {
    await deleteDewormingRecord(petId, row.id)
  } else {
    await deleteWeightRecord(petId, row.id)
  }
  ElMessage.success('健康记录已删除')
  loadHealthRecords()
}

function handlePageChange(page) {
  query.page = page
  loadPets()
}

function handlePageSizeChange(pageSize) {
  query.pageSize = pageSize
  query.page = 1
  loadPets()
}

onMounted(async () => {
  await Promise.all([loadCustomers(), loadAvatarLibrary()])
  await loadPets()
})
</script>

<template>
  <AppLayout>
    <div class="page-header pets-page-header">
      <div>
        <h1 class="page-title">宠物管理</h1>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增宠物</el-button>
    </div>

    <el-card shadow="never" class="filter-panel">
      <el-form :inline="true" :model="query">
        <el-form-item label="宠物名称">
          <el-input v-model="query.name" placeholder="请输入名称" clearable />
        </el-form-item>
        <el-form-item label="所属客户">
          <el-select v-model="query.customerId" placeholder="全部客户" clearable filterable class="customer-select">
            <el-option v-for="item in customerOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="种类">
          <el-input v-model="query.species" placeholder="如猫、狗" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable class="status-select">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="query.page = 1; loadPets()">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="pets-panel">
      <div v-loading="loading" class="pet-grid-wrap">
        <el-empty v-if="!records.length && !loading" description="暂无宠物档案" />
        <div v-else class="pet-grid">
          <article
            v-for="row in records"
            :key="row.id"
            class="pet-card"
            :class="{ 'is-disabled': row.status !== 'ENABLED' }"
          >
            <div class="pet-status-rail" aria-hidden="true"></div>
            <div class="pet-avatar" :class="{ 'has-image': petAvatarSrc(row), 'is-cat': `${row.species || ''}`.includes('猫'), 'is-dog': `${row.species || ''}`.includes('狗') || `${row.species || ''}`.includes('犬') }">
              <img
                v-if="petAvatarSrc(row)"
                :src="petAvatarSrc(row)"
                :alt="`${row.breed || row.species || '宠物'}头像`"
                @error="handleAvatarLoadError(petAvatarSrc(row))"
              />
              <span v-else>{{ petAvatar(row) }}</span>
            </div>
            <div class="pet-card-main">
              <div class="pet-card-top">
                <div class="pet-title-wrap">
                  <h2 class="pet-name">{{ row.name }}</h2>
                  <div class="pet-meta-line">
                    <span class="pet-pill">种类 <strong>{{ displayValue(row.species) }}</strong></span>
                    <span class="pet-pill">品种 <strong>{{ displayValue(row.breed) }}</strong></span>
                    <span class="pet-pill">性别 <strong>{{ genderLabel(row.gender) }}</strong></span>
                  </div>
                </div>
                <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="light" class="status-tag">
                  {{ row.status === 'ENABLED' ? '启用' : '停用' }}
                </el-tag>
              </div>

              <div class="pet-info-list">
                <div class="pet-info-item">
                  <span>所属客户</span>
                  <strong>{{ displayValue(row.customerName) }}</strong>
                </div>
                <div class="pet-info-item">
                  <span>生日</span>
                  <strong>{{ displayValue(row.birthday) }}</strong>
                </div>
                <div class="pet-info-item">
                  <span>体重</span>
                  <strong>{{ displayValue(row.weight, ' kg') }}</strong>
                </div>
                <div class="pet-info-item">
                  <span>是否绝育</span>
                  <strong>{{ row.sterilized ? '已绝育' : '未绝育' }}</strong>
                </div>
              </div>

              <div v-if="row.remark" class="pet-remark">
                备注：{{ row.remark }}
              </div>

              <div class="pet-card-actions">
                <el-button :icon="EditPen" @click="openEditDialog(row)">编辑</el-button>
                <el-button :icon="FirstAidKit" @click="openHealthDialog(row)">健康记录</el-button>
                <el-button
                  :icon="row.status === 'ENABLED' ? SwitchButton : CircleCheck"
                  :type="row.status === 'ENABLED' ? 'danger' : 'warning'"
                  plain
                  @click="toggleStatus(row)"
                >
                  {{ row.status === 'ENABLED' ? '停用' : '启用' }}
                </el-button>
                <el-button
                  v-if="row.status !== 'ENABLED'"
                  :icon="DeleteIcon"
                  type="danger"
                  plain
                  @click="handleDeletePet(row)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </article>
        </div>
      </div>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑宠物' : '新增宠物'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属客户" prop="customerId">
          <el-select v-model="form.customerId" placeholder="请选择客户" filterable class="full-width">
            <el-option v-for="item in customerOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="宠物名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="种类" prop="species">
          <el-input v-model="form.species" maxlength="50" placeholder="如猫、狗" />
        </el-form-item>
        <el-form-item label="品种">
          <el-input v-model="form.breed" maxlength="50" />
        </el-form-item>
        <el-form-item label="宠物头像">
          <div
            class="avatar-editor"
            :class="{ 'is-dragover': avatarDragging }"
            @dragenter.prevent="avatarDragging = true"
            @dragover.prevent="avatarDragging = true"
            @dragleave.prevent="handleAvatarDragLeave"
            @drop.prevent="handleAvatarDrop"
          >
            <div class="avatar-preview" :class="{ 'has-image': formAvatarSrc() }">
              <img v-if="formAvatarSrc()" :src="formAvatarSrc()" alt="宠物头像预览" @error="handleAvatarLoadError(formAvatarSrc())" />
              <span v-else>宠</span>
            </div>
            <div class="avatar-actions">
              <el-tag effect="light" type="info">{{ formAvatarSourceLabel() }}</el-tag>
              <div class="avatar-buttons">
                <el-button :icon="Upload" @click="openAvatarPicker">{{ avatarFile ? '重新上传' : '上传头像' }}</el-button>
                <el-button :icon="DeleteIcon" plain @click="removeSelectedAvatar">恢复默认</el-button>
              </div>
              <span class="avatar-hint">
                {{ avatarFile ? '当前头像待保存。' : '' }}选择图片后进入裁剪界面，可拖动圆形区域调整效果；支持拖拽，jpg/png/webp，最大 2MB
              </span>
              <input
                ref="avatarInputRef"
                class="avatar-input"
                type="file"
                accept="image/png,image/jpeg,image/webp"
                @change="handleAvatarChange"
              />
            </div>
          </div>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio-button v-for="item in genderOptions" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" placeholder="请选择生日" />
        </el-form-item>
        <el-form-item label="体重">
          <el-input-number v-model="form.weight" :min="0.1" :precision="2" :step="0.1" />
          <span class="form-suffix">kg</span>
        </el-form-item>
        <el-form-item label="是否绝育">
          <el-switch v-model="form.sterilized" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePet">保存</el-button>
      </template>
    </el-dialog>

    <AvatarCropperDialog
      v-model="avatarCropperVisible"
      :file="avatarCropperFile"
      @confirm="handleAvatarCropConfirm"
      @cancel="handleAvatarCropCancel"
    />

    <el-dialog v-model="healthDialogVisible" :title="`${currentPet?.name || ''}的健康记录`" width="900px">
      <div v-loading="healthLoading">
        <el-tabs v-model="activeHealthTab">
          <el-tab-pane label="疫苗记录" name="vaccines">
            <div class="health-toolbar">
              <el-button type="primary" size="small" @click="openHealthForm('vaccines')">新增疫苗记录</el-button>
            </div>
            <el-table :data="healthRecords.vaccines" border>
              <el-table-column prop="vaccineName" label="疫苗名称" />
              <el-table-column prop="vaccinationDate" label="接种日期" width="120" />
              <el-table-column prop="institution" label="接种机构" />
              <el-table-column prop="nextVaccinationDate" label="下次接种" width="120" />
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openHealthForm('vaccines', row)">编辑</el-button>
                  <el-button link type="danger" @click="deleteHealthRecord('vaccines', row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="驱虫记录" name="deworming">
            <div class="health-toolbar">
              <el-button type="primary" size="small" @click="openHealthForm('deworming')">新增驱虫记录</el-button>
            </div>
            <el-table :data="healthRecords.dewormingRecords" border>
              <el-table-column prop="dewormingType" label="类型" width="90" />
              <el-table-column prop="medicineName" label="药品名称" />
              <el-table-column prop="dewormingDate" label="驱虫日期" width="120" />
              <el-table-column prop="nextDewormingDate" label="下次驱虫" width="120" />
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openHealthForm('deworming', row)">编辑</el-button>
                  <el-button link type="danger" @click="deleteHealthRecord('deworming', row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="体重记录" name="weights">
            <div class="health-toolbar">
              <el-button type="primary" size="small" @click="openHealthForm('weights')">新增体重记录</el-button>
            </div>
            <el-table :data="healthRecords.weights" border>
              <el-table-column prop="recordDate" label="记录日期" width="120" />
              <el-table-column prop="weight" label="体重 kg" width="120" />
              <el-table-column prop="remark" label="备注" />
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openHealthForm('weights', row)">编辑</el-button>
                  <el-button link type="danger" @click="deleteHealthRecord('weights', row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <el-dialog v-model="healthFormVisible" title="健康记录" width="560px">
      <el-form ref="healthFormRef" :model="healthForm" :rules="healthRules" label-width="110px">
        <template v-if="healthFormType === 'vaccines'">
          <el-form-item label="疫苗名称" prop="vaccineName">
            <el-input v-model="healthForm.vaccineName" maxlength="100" />
          </el-form-item>
          <el-form-item label="接种日期" prop="vaccinationDate">
            <el-date-picker v-model="healthForm.vaccinationDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="接种机构">
            <el-input v-model="healthForm.institution" maxlength="100" />
          </el-form-item>
          <el-form-item label="下次接种">
            <el-date-picker v-model="healthForm.nextVaccinationDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
        </template>
        <template v-else-if="healthFormType === 'deworming'">
          <el-form-item label="驱虫类型" prop="dewormingType">
            <el-radio-group v-model="healthForm.dewormingType">
              <el-radio-button v-for="item in dewormingTypeOptions" :key="item" :label="item" />
            </el-radio-group>
          </el-form-item>
          <el-form-item label="药品名称" prop="medicineName">
            <el-input v-model="healthForm.medicineName" maxlength="100" />
          </el-form-item>
          <el-form-item label="驱虫日期" prop="dewormingDate">
            <el-date-picker v-model="healthForm.dewormingDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="下次驱虫">
            <el-date-picker v-model="healthForm.nextDewormingDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="记录日期" prop="recordDate">
            <el-date-picker v-model="healthForm.recordDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="体重" prop="weight">
            <el-input-number v-model="healthForm.weight" :min="0.1" :precision="2" :step="0.1" />
            <span class="form-suffix">kg</span>
          </el-form-item>
        </template>
        <el-form-item label="备注">
          <el-input v-model="healthForm.remark" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="healthFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="healthSaving" @click="saveHealthRecord">保存</el-button>
      </template>
    </el-dialog>
  </AppLayout>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.pets-page-header {
  align-items: flex-start;
}

.filter-panel,
.pets-panel {
  border-radius: 8px;
}

.filter-panel {
  margin-bottom: 16px;
}

.filter-panel :deep(.el-form) {
  gap: 10px 18px;
}

.filter-panel :deep(.el-form-item) {
  margin-bottom: 10px;
}

.customer-select {
  width: 220px;
}

.status-select {
  width: 140px;
}

.full-width {
  width: 100%;
}

.form-suffix {
  margin-left: 8px;
  color: #6b7280;
}

.avatar-editor {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 12px;
  border: 1px dashed #ead8bb;
  border-radius: 8px;
  background: #fffaf2;
  transition: border-color 180ms ease, background 180ms ease;
}

.avatar-editor.is-dragover {
  border-color: #f59e0b;
  background: #fff3dc;
}

.avatar-preview {
  width: 88px;
  height: 88px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid #f0e3d0;
  border-radius: 50%;
  background:
    radial-gradient(circle at 34% 28%, rgba(255, 255, 255, 0.9) 0 18%, transparent 19%),
    linear-gradient(145deg, #fff7e7 0%, #f4ddbc 100%);
  color: #a76213;
  font-size: 26px;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(166, 98, 19, 0.1);
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.avatar-actions {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.avatar-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.avatar-buttons .el-button {
  margin-left: 0;
}

.avatar-hint {
  color: #8a6b45;
  font-size: 12px;
  line-height: 18px;
}

.avatar-input {
  display: none;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.pets-panel :deep(.el-card__body) {
  padding: 0;
}

.pet-grid-wrap {
  min-height: 200px;
  padding: 20px;
}

.pet-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.pet-card {
  position: relative;
  display: grid;
  grid-template-columns: 148px minmax(0, 1fr);
  gap: 22px;
  min-height: 218px;
  padding: 26px 28px 22px;
  overflow: hidden;
  border: 1px solid #ebe5db;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(41, 31, 18, 0.06);
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 180ms ease;
}

.pet-card:hover {
  border-color: #f3c56b;
  box-shadow: 0 16px 34px rgba(41, 31, 18, 0.09);
  transform: translateY(-2px);
}

.pet-card.is-disabled {
  border-color: #d8dce3;
  background: linear-gradient(180deg, #ffffff 0%, #fbfbfc 100%);
}

.pet-status-rail {
  position: absolute;
  left: 0;
  top: 0;
  width: 4px;
  height: 100%;
  background: #49aa5b;
}

.pet-card.is-disabled .pet-status-rail {
  background: #9aa4b2;
}

.pet-avatar {
  align-self: center;
  width: 126px;
  height: 126px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background:
    radial-gradient(circle at 35% 28%, rgba(255, 255, 255, 0.95) 0 18%, transparent 19%),
    linear-gradient(145deg, #fff7e7 0%, #f4ddbc 100%);
  color: #a76213;
  font-size: 36px;
  font-weight: 800;
  box-shadow: inset 0 0 0 10px rgba(255, 255, 255, 0.55), 0 10px 24px rgba(166, 98, 19, 0.12);
}

.pet-avatar.is-cat {
  background:
    radial-gradient(circle at 34% 28%, rgba(255, 255, 255, 0.92) 0 18%, transparent 19%),
    linear-gradient(145deg, #f7f3eb 0%, #d9d1c5 100%);
  color: #5b6470;
}

.pet-avatar.is-dog {
  background:
    radial-gradient(circle at 34% 28%, rgba(255, 255, 255, 0.9) 0 18%, transparent 19%),
    linear-gradient(145deg, #fff2d7 0%, #eca43a 100%);
  color: #7a3d08;
}

.pet-avatar.has-image {
  overflow: hidden;
  background: #fff7e8;
  box-shadow: 0 10px 24px rgba(166, 98, 19, 0.12);
}

.pet-avatar img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.pet-card-main {
  min-width: 0;
}

.pet-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 14px;
  border-bottom: 1px dashed #eadfce;
}

.pet-title-wrap {
  min-width: 0;
}

.pet-name {
  margin: 0 0 12px;
  overflow: hidden;
  color: #111827;
  font-size: 24px;
  line-height: 30px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pet-meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.pet-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #7a6a59;
  font-size: 14px;
}

.pet-pill::first-letter {
  color: #8a6426;
}

.pet-pill strong {
  color: #1f2937;
  font-weight: 650;
}

.status-tag {
  flex: 0 0 auto;
  height: 30px;
  padding: 0 14px;
  border-radius: 8px;
  font-size: 14px;
}

.pet-info-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
  margin-top: 14px;
}

.pet-info-item {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #667085;
  font-size: 14px;
}

.pet-info-item span {
  flex: 0 0 auto;
}

.pet-info-item strong {
  min-width: 0;
  overflow: hidden;
  color: #1f2937;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pet-remark {
  margin-top: 12px;
  overflow: hidden;
  color: #667085;
  font-size: 13px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pet-card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  margin-top: 16px;
}

.pet-card-actions .el-button {
  min-width: 112px;
  margin-left: 0;
}

.pets-panel .pagination-bar {
  align-items: center;
  min-height: 68px;
  padding: 12px 20px 16px;
  margin-top: 0;
  border-top: 1px solid #eef1f4;
  background: #ffffff;
}

.health-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

@media (max-width: 1280px) {
  .pet-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .pets-page-header {
    flex-direction: column;
    gap: 12px;
  }

  .pet-grid-wrap {
    padding: 14px;
  }

  .pet-card {
    grid-template-columns: 1fr;
    gap: 16px;
    padding: 22px 20px;
  }

  .pet-avatar {
    width: 112px;
    height: 112px;
  }

  .pet-card-top,
  .pet-card-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .pet-info-list {
    grid-template-columns: 1fr;
  }

  .pet-card-actions .el-button {
    width: 100%;
  }

  .avatar-editor {
    align-items: flex-start;
    flex-direction: column;
  }

  .avatar-actions,
  .avatar-buttons {
    width: 100%;
  }

  .avatar-buttons .el-button {
    flex: 1 1 120px;
  }

  .pagination-bar {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
