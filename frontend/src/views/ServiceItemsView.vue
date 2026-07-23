<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import {
  createServiceItem,
  fetchServiceItems,
  updateServiceItem,
  updateServiceItemStatus,
} from '../api/serviceItems'

const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const editingId = ref(null)
const records = ref([])
const total = ref(0)

const query = reactive({
  name: '',
  category: '',
  status: '',
  page: 1,
  pageSize: 10,
})

const form = reactive({
  name: '',
  category: '',
  price: null,
  cost: 0,
  durationMinutes: 30,
  description: '',
})

const rules = {
  name: [{ required: true, message: '请输入服务名称', trigger: 'blur' }],
  category: [{ required: true, message: '请输入服务分类', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'change' }],
  cost: [{ required: true, message: '请输入成本', trigger: 'change' }],
  durationMinutes: [{ required: true, message: '请输入服务时长', trigger: 'change' }],
}

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
]

async function loadItems() {
  loading.value = true
  try {
    const result = await fetchServiceItems(query)
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
  query.category = ''
  query.status = ''
  query.page = 1
  loadItems()
}

function resetForm() {
  Object.assign(form, {
    name: '',
    category: '',
    price: null,
    cost: 0,
    durationMinutes: 30,
    description: '',
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
    name: row.name,
    category: row.category,
    price: row.price,
    cost: row.cost ?? 0,
    durationMinutes: row.durationMinutes,
    description: row.description,
  })
  dialogVisible.value = true
}

async function saveItem() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      ...form,
      price: Number(form.price),
      cost: Number(form.cost),
      durationMinutes: Number(form.durationMinutes),
    }
    if (editingId.value) {
      await updateServiceItem(editingId.value, payload)
      ElMessage.success('服务项目已更新')
    } else {
      await createServiceItem(payload)
      ElMessage.success('服务项目已新增')
    }
    dialogVisible.value = false
    loadItems()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const actionText = nextStatus === 'ENABLED' ? '启用' : '停用'
  await ElMessageBox.confirm(`确定${actionText}服务项目「${row.name}」吗？`, '确认操作', { type: 'warning' })
  await updateServiceItemStatus(row.id, nextStatus)
  ElMessage.success(`服务项目已${actionText}`)
  loadItems()
}

function handlePageChange(page) {
  query.page = page
  loadItems()
}

function handlePageSizeChange(pageSize) {
  query.pageSize = pageSize
  query.page = 1
  loadItems()
}

onMounted(loadItems)
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <h1 class="page-title">服务项目</h1>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增服务</el-button>
    </div>

    <el-card shadow="never" class="filter-panel">
      <el-form :inline="true" :model="query">
        <el-form-item label="服务名称">
          <el-input v-model="query.name" placeholder="请输入名称" clearable />
        </el-form-item>
        <el-form-item label="服务分类">
          <el-input v-model="query.category" placeholder="如洗护、问诊" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable class="status-select">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="query.page = 1; loadItems()">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-panel">
      <el-table v-loading="loading" :data="records" border>
        <el-table-column prop="name" label="服务名称" min-width="150" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="price" label="价格" width="110">
          <template #default="{ row }">￥{{ Number(row.price).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="cost" label="成本" width="110">
          <template #default="{ row }">￥{{ Number(row.cost || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="预估利润率" width="120">
          <template #default="{ row }">
            {{ Number(row.price) > 0 ? (((Number(row.price) - Number(row.cost || 0)) / Number(row.price)) * 100).toFixed(1) : '0.0' }}%
          </template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="服务时长" width="110">
          <template #default="{ row }">{{ row.durationMinutes }} 分钟</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑服务项目' : '新增服务项目'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="服务名称" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="服务分类" prop="category">
          <el-input v-model="form.category" maxlength="50" placeholder="如洗护、问诊、寄养" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="成本" prop="cost">
          <el-input-number v-model="form.cost" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="服务时长" prop="durationMinutes">
          <el-input-number v-model="form.durationMinutes" :min="1" :step="5" />
          <span class="form-suffix">分钟</span>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveItem">保存</el-button>
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

.status-select {
  width: 140px;
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
</style>
