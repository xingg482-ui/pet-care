<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { fetchCustomers } from '../api/customers'
import { createPet, fetchPets, updatePet, updatePetStatus } from '../api/pets'
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
  label: `${item.name} / ${item.phone}`,
  value: item.id,
})))

async function loadCustomers() {
  const result = await fetchCustomers({ status: 'ENABLED', page: 1, pageSize: 100 })
  customers.value = result.records
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
  dialogVisible.value = true
}

async function savePet() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form, customerId: Number(form.customerId) }
    if (editingId.value) {
      await updatePet(editingId.value, payload)
      ElMessage.success('宠物已更新')
    } else {
      await createPet(payload)
      ElMessage.success('宠物已新增')
    }
    dialogVisible.value = false
    loadPets()
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
  await loadCustomers()
  await loadPets()
})
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <h1 class="page-title">宠物管理</h1>
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

    <el-card shadow="never" class="table-panel">
      <el-table v-loading="loading" :data="records" border>
        <el-table-column prop="name" label="宠物名称" min-width="120" />
        <el-table-column prop="species" label="种类" width="100" />
        <el-table-column prop="breed" label="品种" min-width="120" />
        <el-table-column prop="gender" label="性别" width="90">
          <template #default="{ row }">
            {{ row.gender === 'MALE' ? '公' : row.gender === 'FEMALE' ? '母' : '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="所属客户" min-width="130" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="primary" @click="openHealthDialog(row)">健康记录</el-button>
            <el-button link :type="row.status === 'ENABLED' ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 'ENABLED' ? '停用' : '启用' }}
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

.filter-panel,
.table-panel {
  border-radius: 8px;
}

.filter-panel {
  margin-bottom: 16px;
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

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.health-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
</style>
