<script setup>
import { useRoute, useRouter } from 'vue-router'
import { Collection, Document, House, Money, SwitchButton, Tickets, User } from '@element-plus/icons-vue'
import { logout } from '../api/auth'

const route = useRoute()
const router = useRouter()

async function handleLogout() {
  await logout().catch(() => {})
  localStorage.removeItem('petCareToken')
  localStorage.removeItem('petCareUsername')
  router.push('/login')
}
</script>

<template>
  <el-container class="app-shell">
    <el-aside width="224px" class="sidebar">
      <div class="brand">
        <span class="brand-mark">宠</span>
        <span>宠物订单系统</span>
      </div>
      <el-menu router :default-active="route.path" class="nav-menu">
        <el-menu-item index="/dashboard">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/customers">
          <el-icon><User /></el-icon>
          <span>客户管理</span>
        </el-menu-item>
        <el-menu-item index="/pets">
          <el-icon><Collection /></el-icon>
          <span>宠物管理</span>
        </el-menu-item>
        <el-menu-item index="/service-items">
          <el-icon><Tickets /></el-icon>
          <span>服务项目</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/orders/create">
          <el-icon><Document /></el-icon>
          <span>新建订单</span>
        </el-menu-item>
        <el-menu-item index="/finance">
          <el-icon><Money /></el-icon>
          <span>财务管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <span>单门店管理后台</span>
        <el-button :icon="SwitchButton" text @click="handleLogout">退出</el-button>
      </el-header>
      <el-main class="content">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--pc-bg);
}

.sidebar {
  background: #ffffff;
  border-right: 1px solid var(--pc-border);
  box-shadow: 1px 0 0 rgba(31, 41, 51, 0.02);
}

.brand {
  height: 57px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0;
  color: var(--pc-text);
  border-bottom: 1px solid var(--pc-border);
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  color: var(--pc-primary-hover);
  background: var(--pc-primary-soft);
  border: 1px solid #cfe0e4;
  font-size: 14px;
}

.nav-menu {
  border-right: 0;
  padding: 8px;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #526070;
  --el-menu-hover-bg-color: #f4f7f9;
  --el-menu-active-color: var(--pc-primary-hover);
}

.nav-menu :deep(.el-menu-item) {
  height: 36px;
  margin-bottom: 4px;
  padding: 0 10px !important;
  border-radius: 8px;
  color: #526070;
  font-size: 14px;
  transition: background-color 180ms ease, color 180ms ease, box-shadow 180ms ease;
}

.nav-menu :deep(.el-menu-item:hover) {
  background: #f4f7f9;
  color: var(--pc-text);
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: var(--pc-primary-soft);
  color: var(--pc-primary-hover);
  box-shadow: inset 0 0 0 1px #cfe0e4;
}

.nav-menu :deep(.el-icon) {
  color: inherit;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid var(--pc-border);
  height: 57px;
  padding: 0 24px;
  font-weight: 600;
  color: #3d4854;
}

.content {
  background: var(--pc-bg);
  padding: 16px 24px 24px;
}
</style>
