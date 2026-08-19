<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { createCustomer, deleteCustomer, fetchCustomers, updateCustomer, updateCustomerStatus } from '../api/customers'

const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const editingId = ref(null)
const records = ref([])
const total = ref(0)

const query = reactive({
  name: '',
  phone: '',
  status: '',
  page: 1,
  pageSize: 10,
})

const form = reactive({
  name: '',
  phone: '',
  email: '',
  address: '',
  remark: '',
})

const rules = {
  name: [{ required: true, message: '请输入客户姓名', trigger: 'blur' }],
}

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
]

async function loadCustomers() {
  loading.value = true
  try {
    const result = await fetchCustomers(query)
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
  query.phone = ''
  query.status = ''
  query.page = 1
  loadCustomers()
}

function openCreateDialog() {
  editingId.value = null
  Object.assign(form, { name: '', phone: '', email: '', address: '', remark: '' })
  dialogVisible.value = true
}

function openEditDialog(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    phone: row.phone,
    email: row.email,
    address: row.address,
    remark: row.remark,
  })
  dialogVisible.value = true
}

async function saveCustomer() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await updateCustomer(editingId.value, form)
      ElMessage.success('客户已更新')
    } else {
      await createCustomer(form)
      ElMessage.success('客户已新增')
    }
    dialogVisible.value = false
    loadCustomers()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const actionText = nextStatus === 'ENABLED' ? '启用' : '停用'
  await ElMessageBox.confirm(`确定${actionText}客户「${row.name}」吗？`, '确认操作', { type: 'warning' })
  await updateCustomerStatus(row.id, nextStatus)
  ElMessage.success(`客户已${actionText}`)
  loadCustomers()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除停用客户「${row.name}」吗？删除后不可恢复。`, '删除客户', { type: 'warning' })
  await deleteCustomer(row.id)
  ElMessage.success('客户已删除')
  loadCustomers()
}

function handlePageChange(page) {
  query.page = page
  loadCustomers()
}

function handlePageSizeChange(pageSize) {
  query.pageSize = pageSize
  query.page = 1
  loadCustomers()
}

onMounted(loadCustomers)

function displayValue(value) {
  return value === null || value === undefined || value === '' ? '-' : value
}
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <h1 class="page-title">客户管理</h1>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增客户</el-button>
    </div>

    <el-card shadow="never" class="filter-panel">
      <el-form :inline="true" :model="query">
        <el-form-item label="客户姓名">
          <el-input v-model="query.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable class="status-select">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="query.page = 1; loadCustomers()">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-panel">
      <el-table v-loading="loading" :data="records" border>
        <el-table-column prop="name" label="客户姓名" min-width="120" />
        <el-table-column label="手机号" min-width="140">
          <template #default="{ row }">{{ displayValue(row.phone) }}</template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ displayValue(row.email) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ displayValue(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link :type="row.status === 'ENABLED' ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
            <el-button v-if="row.status === 'DISABLED'" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑客户' : '新增客户'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="客户姓名" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="30" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" maxlength="100" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" maxlength="200" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCustomer">保存</el-button>
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

.filter-panel {
  margin-bottom: 16px;
  border-radius: 8px;
}

.table-panel {
  border-radius: 8px;
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
