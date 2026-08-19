<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Delete as DeleteIcon, EditPen, FirstAidKit, Plus, SwitchButton, Upload } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import AvatarCropperDialog from '../components/AvatarCropperDialog.vue'
import { fetchPetAvatarLibrary } from '../api/petAvatarLibrary'
import { createMyPet, fetchMyPets, removeMyPetAvatar, updateMyPet, updateMyPetStatus, uploadMyPetAvatar } from '../api/pets'
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
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const avatarInputRef = ref()
const editingId = ref(null)
const records = ref([])
const healthDialogVisible = ref(false)
const healthLoading = ref(false)
const healthSaving = ref(false)
const activeHealthTab = ref('vaccines')
const currentPet = ref(null)
const healthRecords = ref({ vaccines: [], dewormingRecords: [], weights: [] })
const healthByPetId = ref({})
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

const form = reactive({
  customerId: 0,
  name: '',
  species: '',
  breed: '',
  gender: 'UNKNOWN',
  birthday: '',
  weight: null,
  sterilized: false,
  color: '',
  microchipNo: '',
  allergies: '',
  medicalHistory: '',
  dietPreference: '',
  behaviorNotes: '',
  exerciseLevel: '',
  careNotes: '',
  remark: '',
})

const rules = {
  name: [{ required: true, message: '请输入宠物名称', trigger: 'blur' }],
  species: [{ required: true, message: '请输入宠物种类', trigger: 'blur' }],
}

const genderOptions = [
  { label: '未知', value: 'UNKNOWN' },
  { label: '公', value: 'MALE' },
  { label: '母', value: 'FEMALE' },
]

const exerciseLevelOptions = [
  { label: '低', value: 'LOW' },
  { label: '中', value: 'MEDIUM' },
  { label: '高', value: 'HIGH' },
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

function genderLabel(gender) {
  return gender === 'MALE' ? '公' : gender === 'FEMALE' ? '母' : '未知'
}

function exerciseLevelLabel(value) {
  return exerciseLevelOptions.find((item) => item.value === value)?.label || '未填写'
}

function displayValue(value, suffix = '') {
  if (value === null || value === undefined || value === '') {
    return '未填写'
  }
  return `${value}${suffix}`
}

function briefText(value, maxLength = 26) {
  if (!value) return '未填写'
  const text = String(value)
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text
}

function latestRecord(records, dateField) {
  return [...(records || [])].sort((left, right) => String(right[dateField] || '').localeCompare(String(left[dateField] || '')))[0]
}

function petHealth(row) {
  return healthByPetId.value[row.id] || { vaccines: [], dewormingRecords: [], weights: [] }
}

function latestVaccine(row) {
  return latestRecord(petHealth(row).vaccines, 'vaccinationDate')
}

function latestDeworming(row) {
  return latestRecord(petHealth(row).dewormingRecords, 'dewormingDate')
}

function latestWeight(row) {
  return latestRecord(petHealth(row).weights, 'recordDate')
}

function latestWeightText(row) {
  const record = latestWeight(row)
  if (record?.weight) {
    return `${record.weight} kg`
  }
  return displayValue(row.weight, ' kg')
}

function recentWeightDate(row) {
  return latestWeight(row)?.recordDate || '未填写'
}

async function loadPetHealthSummaries(pets) {
  const entries = await Promise.all(
    pets.map(async (pet) => {
      const summary = await fetchHealthRecords(pet.id).catch(() => ({ vaccines: [], dewormingRecords: [], weights: [] }))
      return [pet.id, summary]
    })
  )
  healthByPetId.value = Object.fromEntries(entries)
}

function normalizeAssetUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  return url.startsWith('/') ? url : `/${url}`
}

function firstAvailableAvatar(urls) {
  return urls.map((url) => normalizeAssetUrl(url)).find((url) => url && !brokenAvatarUrls.value.has(url)) || ''
}

function petAvatar(row) {
  const species = `${row.species || ''}`.toLowerCase()
  if (species.includes('猫') || species.includes('cat')) return '喵'
  if (species.includes('狗') || species.includes('犬') || species.includes('dog')) return '汪'
  return (row.name || '宠').slice(0, 1)
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

function petAvatarSrc(row) {
  if (restoredDefaultPetIds.value.has(row.id)) {
    return resolveRuntimePetAvatar(row)
  }
  return firstAvailableAvatar([row.avatarUrl, resolveRuntimePetAvatar(row)])
}

function formDefaultAvatarSrc() {
  return resolveRuntimePetAvatar(form)
}

function formAvatarSrc() {
  return firstAvailableAvatar([
    avatarPreviewUrl.value,
    !removeAvatarAfterSave.value ? currentAvatarUrl.value : '',
    formDefaultAvatarSrc(),
  ])
}

function formAvatarSourceLabel() {
  if (avatarPreviewUrl.value) return '待保存头像'
  if (currentAvatarUrl.value && !removeAvatarAfterSave.value) return '上传头像'
  if (formDefaultAvatarSrc()) return '品种默认'
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

function markBrokenAvatar(url) {
  const normalizedUrl = normalizeAssetUrl(url)
  if (!normalizedUrl) return
  const nextBrokenUrls = new Set(brokenAvatarUrls.value)
  nextBrokenUrls.add(normalizedUrl)
  brokenAvatarUrls.value = nextBrokenUrls
}

function handleAvatarLoadError(url) {
  markBrokenAvatar(url)
  if (url === avatarPreviewUrl.value) {
    revokeAvatarPreview()
    avatarFile.value = null
  }
}

async function applyAvatarFile(file) {
  try {
    if (!validateAvatarFile(file)) return
    avatarCropperFile.value = file
    avatarCropperVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '头像处理失败，请重新选择')
  }
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

async function loadAvatarLibrary() {
  avatarLibraryRecords.value = await fetchPetAvatarLibrary({ status: 'ENABLED' }).catch(() => [])
}

async function loadPets() {
  loading.value = true
  try {
    const result = await fetchMyPets()
    records.value = result.records
    await loadPetHealthSummaries(records.value)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    customerId: 0,
    name: '',
    species: '',
    breed: '',
    gender: 'UNKNOWN',
    birthday: '',
    weight: null,
    sterilized: false,
    color: '',
    microchipNo: '',
    allergies: '',
    medicalHistory: '',
    dietPreference: '',
    behaviorNotes: '',
    exerciseLevel: '',
    careNotes: '',
    remark: '',
  })
  resetAvatarState()
  formRef.value?.clearValidate()
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
    color: row.color || '',
    microchipNo: row.microchipNo || '',
    allergies: row.allergies || '',
    medicalHistory: row.medicalHistory || '',
    dietPreference: row.dietPreference || '',
    behaviorNotes: row.behaviorNotes || '',
    exerciseLevel: row.exerciseLevel || '',
    careNotes: row.careNotes || '',
    remark: row.remark,
  })
  resetAvatarState(row)
  dialogVisible.value = true
}

async function savePet() {
  await formRef.value.validate()
  saving.value = true
  try {
    let savedPet
    if (editingId.value) {
      savedPet = await updateMyPet(editingId.value, form)
      ElMessage.success('宠物资料已更新')
    } else {
      savedPet = await createMyPet(form)
      ElMessage.success('宠物已新增')
    }
    if (savedPet?.id && removeAvatarAfterSave.value && !avatarFile.value) {
      savedPet = await removeMyPetAvatar(savedPet.id)
      const nextRestoredIds = new Set(restoredDefaultPetIds.value)
      nextRestoredIds.add(savedPet.id)
      restoredDefaultPetIds.value = nextRestoredIds
    }
    if (savedPet?.id && avatarFile.value) {
      savedPet = await uploadMyPetAvatar(savedPet.id, avatarFile.value)
      const nextRestoredIds = new Set(restoredDefaultPetIds.value)
      nextRestoredIds.delete(savedPet.id)
      restoredDefaultPetIds.value = nextRestoredIds
      ElMessage.success('宠物头像已保存')
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
  await updateMyPetStatus(row.id, nextStatus)
  ElMessage.success(`宠物已${actionText}`)
  await loadPets()
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
    healthByPetId.value = {
      ...healthByPetId.value,
      [currentPet.value.id]: healthRecords.value,
    }
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
    await loadHealthRecords()
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
  await loadHealthRecords()
}

onMounted(async () => {
  await loadAvatarLibrary()
  await loadPets()
})
</script>

<template>
  <AppLayout>
    <section class="pets-panel">
      <div class="page-header">
        <div>
          <h1 class="page-title">我的宠物</h1>
          <p class="muted">管理自己的宠物资料和头像</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增宠物</el-button>
      </div>

      <div v-loading="loading" class="pet-grid-wrap">
        <el-empty v-if="!records.length && !loading" description="暂无宠物档案" />
        <div v-else class="pet-grid">
          <article
            v-for="row in records"
            :key="row.id"
            class="pet-card"
            :class="{ 'is-disabled': row.status !== 'ENABLED' }"
          >
            <div class="pet-avatar" :class="{ 'has-image': petAvatarSrc(row), 'is-cat': `${row.species || ''}`.includes('猫'), 'is-dog': `${row.species || ''}`.includes('狗') || `${row.species || ''}`.includes('犬') }">
              <img
                v-if="petAvatarSrc(row)"
                :src="petAvatarSrc(row)"
                :alt="`${row.breed || row.species || '宠物'}头像`"
                @error="handleAvatarLoadError(petAvatarSrc(row))"
              />
              <span v-else>{{ petAvatar(row) }}</span>
            </div>
            <section class="pet-card-main">
              <div class="pet-card-top">
                <div class="pet-title-wrap">
                  <div class="pet-title-line">
                    <h2 class="pet-name">{{ row.name }}</h2>
                    <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="light" class="status-tag">
                      {{ row.status === 'ENABLED' ? '启用' : '停用' }}
                    </el-tag>
                  </div>
                </div>
              </div>

              <div class="pet-info-list">
                <div class="pet-info-item">
                  <span>种类</span>
                  <strong>{{ displayValue(row.species) }}</strong>
                </div>
                <div class="pet-info-item">
                  <span>生日</span>
                  <strong>{{ displayValue(row.birthday) }}</strong>
                </div>
                <div class="pet-info-item">
                  <span>品种</span>
                  <strong>{{ displayValue(row.breed) }}</strong>
                </div>
                <div class="pet-info-item">
                  <span>是否绝育</span>
                  <strong>{{ row.sterilized ? '已绝育' : '未绝育' }}</strong>
                </div>
                <div class="pet-info-item">
                  <span>性别</span>
                  <strong>{{ genderLabel(row.gender) }}</strong>
                </div>
                <div class="pet-info-item">
                  <span>芯片号</span>
                  <strong>{{ displayValue(row.microchipNo) }}</strong>
                </div>
                <div class="pet-info-item">
                  <span>毛色</span>
                  <strong>{{ displayValue(row.color) }}</strong>
                </div>
                <div class="pet-info-item">
                  <span>运动量</span>
                  <strong>{{ exerciseLevelLabel(row.exerciseLevel) }}</strong>
                </div>
              </div>
            </section>

            <section class="pet-health-section">
              <div class="health-section-head">
                <el-icon><FirstAidKit /></el-icon>
                <strong>健康信息</strong>
              </div>
              <div class="health-card-grid">
                <div class="health-mini-card vaccine-card">
                  <div class="health-mini-title">
                    <el-icon><CircleCheck /></el-icon>
                    <strong>疫苗</strong>
                  </div>
                  <p>
                    <span>最近记录</span>
                    <strong>{{ latestVaccine(row)?.vaccineName || '暂无记录' }}</strong>
                  </p>
                  <p>
                    <span>下次提醒</span>
                    <strong>{{ latestVaccine(row)?.nextVaccinationDate || '暂无记录' }}</strong>
                  </p>
                </div>
                <div class="health-mini-card deworming-card">
                  <div class="health-mini-title">
                    <el-icon><FirstAidKit /></el-icon>
                    <strong>驱虫</strong>
                  </div>
                  <p>
                    <span>最近记录</span>
                    <strong>{{ latestDeworming(row)?.medicineName || '暂无记录' }}</strong>
                  </p>
                  <p>
                    <span>下次提醒</span>
                    <strong>{{ latestDeworming(row)?.nextDewormingDate || '暂无记录' }}</strong>
                  </p>
                </div>
                <div class="health-mini-card weight-card">
                  <div class="health-mini-title">
                    <el-icon><FirstAidKit /></el-icon>
                    <strong>体重</strong>
                  </div>
                  <p>
                    <span>当前体重</span>
                    <strong>{{ latestWeightText(row) }}</strong>
                  </p>
                  <p>
                    <span>最近更新</span>
                    <strong>{{ recentWeightDate(row) }}</strong>
                  </p>
                </div>
              </div>
            </section>

            <aside class="pet-card-side">
              <div class="pet-card-actions">
                <el-button :icon="EditPen" @click="openEditDialog(row)">编辑档案</el-button>
                <el-button type="warning" plain :icon="FirstAidKit" @click="openHealthDialog(row)">编辑健康记录</el-button>
                <el-button
                  :icon="row.status === 'ENABLED' ? SwitchButton : CircleCheck"
                  :type="row.status === 'ENABLED' ? 'danger' : 'warning'"
                  plain
                  @click="toggleStatus(row)"
                >
                  {{ row.status === 'ENABLED' ? '停用' : '启用' }}
                </el-button>
              </div>
            </aside>

            <div class="pet-ai-summary">
              <div>
                <span>健康重点</span>
                <strong>{{ briefText(row.allergies || row.medicalHistory, 34) }}</strong>
              </div>
              <div>
                <span>饮食与照护</span>
                <strong>{{ briefText(row.dietPreference || row.careNotes, 34) }}</strong>
              </div>
              <div>
                <span>性格习惯</span>
                <strong>{{ briefText(row.behaviorNotes, 34) }}</strong>
              </div>
            </div>

            <div v-if="row.remark" class="pet-remark">
              备注：{{ row.remark }}
            </div>
          </article>
        </div>
      </div>
      <div class="pets-footnote">
        定期记录宠物的健康数据，给它们更好的照顾
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑宠物' : '新增宠物'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <div class="form-section-title">基础资料</div>
        <el-form-item label="宠物名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="种类" prop="species">
          <el-input v-model="form.species" maxlength="50" placeholder="如猫、狗" />
        </el-form-item>
        <el-form-item label="品种">
          <el-input v-model="form.breed" maxlength="50" />
        </el-form-item>
        <el-form-item label="毛色特征">
          <el-input v-model="form.color" maxlength="50" placeholder="如黄色、黑白花、银渐层" />
        </el-form-item>
        <el-form-item label="芯片号">
          <el-input v-model="form.microchipNo" maxlength="80" placeholder="可选，便于识别宠物身份" />
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
                {{ avatarFile ? '当前头像待保存。' : '' }}选择图片后进入裁剪界面，可缩放图片、拖拽视角；支持拖拽上传，jpg/png/webp，最大 2MB
              </span>
              <input ref="avatarInputRef" class="avatar-input" type="file" accept="image/png,image/jpeg,image/webp" @change="handleAvatarChange" />
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

        <div class="form-section-title">健康管理</div>
        <el-form-item label="过敏史">
          <el-input v-model="form.allergies" type="textarea" maxlength="500" show-word-limit :rows="2" placeholder="如鸡肉过敏、皮肤敏感、药物过敏；没有可填无" />
        </el-form-item>
        <el-form-item label="既往问题">
          <el-input v-model="form.medicalHistory" type="textarea" maxlength="500" show-word-limit :rows="2" placeholder="如肠胃敏感、髌骨问题、耳道炎史、慢性病等" />
        </el-form-item>
        <el-form-item label="饮食习惯">
          <el-input v-model="form.dietPreference" type="textarea" maxlength="500" show-word-limit :rows="2" placeholder="主粮品牌、喂食频率、忌口、零食偏好等" />
        </el-form-item>

        <div class="form-section-title">行为与照护</div>
        <el-form-item label="运动量">
          <el-radio-group v-model="form.exerciseLevel">
            <el-radio-button v-for="item in exerciseLevelOptions" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="性格习惯">
          <el-input v-model="form.behaviorNotes" type="textarea" maxlength="500" show-word-limit :rows="2" placeholder="如怕生、护食、怕吹风、喜欢和同类玩等" />
        </el-form-item>
        <el-form-item label="照护偏好">
          <el-input v-model="form.careNotes" type="textarea" maxlength="500" show-word-limit :rows="2" placeholder="洗护、托管、遛放、安抚方式等注意事项" />
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

    <el-dialog v-model="healthDialogVisible" :title="`${currentPet?.name || ''}的健康记录`" width="900px">
      <div v-loading="healthLoading">
        <div class="health-overview">
          <div>
            <span>最近疫苗</span>
            <strong>{{ latestRecord(healthRecords.vaccines, 'vaccinationDate')?.vaccineName || '暂无记录' }}</strong>
          </div>
          <div>
            <span>最近驱虫</span>
            <strong>{{ latestRecord(healthRecords.dewormingRecords, 'dewormingDate')?.medicineName || '暂无记录' }}</strong>
          </div>
          <div>
            <span>最近体重</span>
            <strong>{{ latestRecord(healthRecords.weights, 'recordDate')?.weight ? `${latestRecord(healthRecords.weights, 'recordDate').weight} kg` : '暂无记录' }}</strong>
          </div>
        </div>
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

    <AvatarCropperDialog
      v-model="avatarCropperVisible"
      :file="avatarCropperFile"
      @confirm="handleAvatarCropConfirm"
      @cancel="handleAvatarCropCancel"
    />
  </AppLayout>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
}

.page-header p {
  margin: 6px 0 0;
}

.pets-panel {
  padding: 28px 32px 22px;
  border: 1px solid #f0e8dc;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(82, 63, 35, 0.08);
}

.pet-grid-wrap {
  min-height: 200px;
}

.pet-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

.pet-card {
  display: grid;
  grid-template-columns: 168px minmax(300px, 1fr) minmax(380px, 1.3fr) 220px;
  gap: 26px;
  align-items: center;
  min-height: 270px;
  padding: 28px 28px 22px;
  border: 1px solid #eee3d4;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(58, 42, 22, 0.04);
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 180ms ease;
}

.pet-card:hover {
  border-color: #f2c879;
  box-shadow: 0 14px 32px rgba(58, 42, 22, 0.07);
  transform: translateY(-2px);
}

.pet-card.is-disabled {
  border-color: #d8dce3;
  background: linear-gradient(180deg, #ffffff 0%, #fbfbfc 100%);
}

.pet-avatar {
  align-self: center;
  justify-self: center;
  width: 136px;
  height: 136px;
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
  box-shadow: 0 0 0 8px #fff5e8, 0 12px 24px rgba(166, 98, 19, 0.12);
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

.pet-avatar img,
.avatar-preview img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.pet-card-main {
  min-width: 0;
  align-self: stretch;
  padding: 8px 0 4px;
}

.pet-card-side {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  align-self: stretch;
  padding-left: 24px;
  border-left: 1px dashed #e8d9c4;
}

.pet-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 22px;
}

.pet-title-wrap {
  min-width: 0;
}

.pet-title-line {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 14px;
}

.pet-name {
  margin: 0;
  overflow: hidden;
  color: #111827;
  font-size: 26px;
  line-height: 34px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  flex: 0 0 auto;
  height: 28px;
  padding: 0 12px;
  border-radius: 8px;
  font-size: 14px;
}

.pet-info-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 34px;
}

.pet-info-item {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6b7280;
  font-size: 14px;
}

.pet-info-item span {
  flex: 0 0 64px;
}

.pet-info-item strong {
  min-width: 0;
  overflow: hidden;
  color: #1f2937;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pet-health-section {
  min-width: 0;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 10px 0 10px 26px;
  border-left: 1px dashed #e8d9c4;
}

.health-section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  color: #111827;
  font-size: 17px;
}

.health-section-head .el-icon {
  color: #39a355;
  font-size: 18px;
}

.health-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.health-mini-card {
  min-width: 0;
  padding: 16px 16px 15px;
  border: 1px solid #dcebdc;
  border-radius: 8px;
  background: linear-gradient(180deg, #fbfff8 0%, #ffffff 100%);
}

.health-mini-card p {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr);
  gap: 8px;
  margin: 14px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.health-mini-card p strong {
  overflow: hidden;
  color: #111827;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.health-mini-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #2f8d45;
  font-size: 15px;
}

.health-mini-title .el-icon {
  font-size: 18px;
}

.deworming-card {
  border-color: #f2dcae;
  background: linear-gradient(180deg, #fffaf0 0%, #ffffff 100%);
}

.deworming-card .health-mini-title {
  color: #b46a05;
}

.weight-card {
  border-color: #cfe0f7;
  background: linear-gradient(180deg, #f4f9ff 0%, #ffffff 100%);
}

.weight-card .health-mini-title {
  color: #2f74c0;
}

.pet-ai-summary {
  grid-column: 1 / 4;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 0;
}

.pet-ai-summary div {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid #f0e6d8;
  border-radius: 8px;
  background: linear-gradient(180deg, #fffdf8 0%, #ffffff 100%);
}

.pet-ai-summary span,
.pet-ai-summary strong {
  display: block;
}

.pet-ai-summary span {
  display: inline-block;
  margin-right: 14px;
  color: #8a6b45;
  font-size: 13px;
}

.pet-ai-summary strong {
  display: inline;
  min-width: 0;
  color: #1f2937;
  font-size: 13px;
  line-height: 18px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pet-remark {
  grid-column: 1 / -1;
  margin: -2px 0 0;
  overflow: hidden;
  color: #667085;
  font-size: 13px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pet-card-actions {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.pet-card-actions .el-button {
  width: 100%;
  min-height: 44px;
  margin-left: 0;
}

.pets-footnote {
  margin-top: 18px;
  color: #8a6b45;
  font-size: 14px;
  text-align: center;
}

.form-suffix {
  margin-left: 8px;
  color: #6b7280;
}

.form-section-title {
  margin: 8px 0 16px;
  padding-left: 10px;
  border-left: 3px solid #f6a700;
  color: #111827;
  font-size: 15px;
  font-weight: 750;
}

.health-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.health-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.health-overview div {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid #f0e6d8;
  border-radius: 8px;
  background: #fffaf3;
}

.health-overview span,
.health-overview strong {
  display: block;
}

.health-overview span {
  margin-bottom: 4px;
  color: #8a6b45;
  font-size: 12px;
}

.health-overview strong {
  overflow: hidden;
  color: #111827;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
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

@media (max-width: 720px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
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
}

@media (max-width: 1280px) {
  .pet-card {
    grid-template-columns: 148px minmax(0, 1fr);
  }

  .pet-health-section,
  .pet-card-side {
    grid-column: 1 / -1;
    padding-left: 0;
    padding-top: 16px;
    border-left: 0;
    border-top: 1px dashed #eadfce;
  }

  .pet-card-actions {
    flex-direction: row;
  }

  .pet-card-actions .el-button {
    flex: 1 1 180px;
  }

  .pet-ai-summary {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .pets-panel {
    padding: 20px 16px;
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

  .pet-title-line {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .pet-info-list {
    grid-template-columns: 1fr;
  }

  .health-card-grid,
  .pet-ai-summary {
    grid-template-columns: 1fr;
  }

  .pet-health-section,
  .pet-card-side {
    grid-column: 1;
  }

  .health-overview {
    grid-template-columns: 1fr;
  }

  .pet-card-actions .el-button {
    width: 100%;
  }
}
</style>
