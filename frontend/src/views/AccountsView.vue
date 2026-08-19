<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, UserFilled } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { approveAccount, fetchAccounts, rejectAccount, updateAccountStatus } from '../api/accounts'

const loading = ref(false)
const records = ref([])
const total = ref(0)

const query = reactive({
  username: '',
  displayName: '',
  role: '',
  status: '',
  page: 1,
  pageSize: 10,
})

const roleOptions = [
  { label: '高级管理员', value: 'SUPER_ADMIN' },
  { label: '管理员', value: 'ADMIN' },
  { label: '客户', value: 'CUSTOMER' },
]

const statusOptions = [
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '正常', value: 'ACTIVE' },
  { label: '审核拒绝', value: 'REJECTED' },
  { label: '已停用', value: 'DISABLED' },
]

const statusTagTypes = {
  PENDING_REVIEW: 'warning',
  ACTIVE: 'success',
  REJECTED: 'danger',
  DISABLED: 'info',
}

function roleName(value) {
  return roleOptions.find((item) => item.value === value)?.label || value
}

function statusName(value) {
  return statusOptions.find((item) => item.value === value)?.label || value
}

function emailText(row) {
  if (row.email) {
    return row.email
  }
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(row.username || '') ? row.username : '-'
}

async function loadAccounts() {
  loading.value = true
  try {
    const result = await fetchAccounts(query)
    records.value = result.records
    total.value = result.total
    query.page = result.page
    query.pageSize = result.pageSize
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.username = ''
  query.displayName = ''
  query.role = ''
  query.status = ''
  query.page = 1
  loadAccounts()
}

async function handleApprove(row) {
  await ElMessageBox.confirm(`确定审核通过账号「${row.displayName}」吗？`, '审核通过', { type: 'warning' })
  await approveAccount(row.id)
  ElMessage.success('账号已审核通过')
  loadAccounts()
}

async function handleReject(row) {
  const { value } = await ElMessageBox.prompt(`请输入拒绝账号「${row.displayName}」的原因`, '审核拒绝', {
    confirmButtonText: '确认拒绝',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPattern: /\S+/,
    inputErrorMessage: '请输入拒绝原因',
  })
  await rejectAccount(row.id, value)
  ElMessage.success('账号已审核拒绝')
  loadAccounts()
}

async function handleToggleStatus(row) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const actionText = nextStatus === 'ACTIVE' ? '启用' : '停用'
  await ElMessageBox.confirm(`确定${actionText}账号「${row.displayName}」吗？`, '确认操作', { type: 'warning' })
  await updateAccountStatus(row.id, nextStatus)
  ElMessage.success(`账号已${actionText}`)
  loadAccounts()
}

function handlePageChange(page) {
  query.page = page
  loadAccounts()
}

function handlePageSizeChange(pageSize) {
  query.pageSize = pageSize
  query.page = 1
  loadAccounts()
}

onMounted(() => {
  loadAccounts()
})
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h1 class="page-title">账号管理</h1>
      </div>
    </div>

    <el-card shadow="never" class="filter-panel">
      <el-form :inline="true" :model="query">
        <el-form-item label="登录账号">
          <el-input v-model="query.username" placeholder="请输入账号" clearable />
        </el-form-item>
        <el-form-item label="用户名称">
          <el-input v-model="query.displayName" placeholder="请输入名称" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部角色" clearable class="filter-select">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable class="filter-select">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="query.page = 1; loadAccounts()">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-panel">
      <el-table v-loading="loading" :data="records" border>
        <el-table-column label="账号" min-width="220">
          <template #default="{ row }">
            <div class="account-cell">
              <span class="account-avatar">
                <el-icon><UserFilled /></el-icon>
              </span>
              <div>
                <strong>{{ row.displayName }}</strong>
                <span>{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">{{ roleName(row.role) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagTypes[row.status]">{{ statusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="140" show-overflow-tooltip />
        <el-table-column label="邮箱" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ emailText(row) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" min-width="170" />
        <el-table-column prop="reviewedAt" label="审核时间" min-width="170" />
        <el-table-column prop="rejectReason" label="拒绝原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING_REVIEW'" link type="primary" @click="handleApprove(row)">通过</el-button>
            <el-button v-if="row.status === 'PENDING_REVIEW'" link type="danger" @click="handleReject(row)">拒绝</el-button>
            <el-button
              v-if="row.status === 'ACTIVE' || row.status === 'DISABLED'"
              link
              :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
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

.filter-select {
  width: 150px;
}

.table-panel {
  border-radius: 8px;
}

.account-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.account-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  border-radius: 50%;
  color: #ffffff;
  background: linear-gradient(180deg, #facc15 0%, #f59e0b 100%);
  font-size: 24px;
}

.account-avatar :deep(.el-icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 24px;
}

.account-cell strong,
.account-cell > div span {
  display: block;
}

.account-cell > div span {
  margin-top: 3px;
  color: var(--pc-muted);
  font-size: 12px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
