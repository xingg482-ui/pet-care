<script setup>
import { useRoute, useRouter } from 'vue-router'
import { Collection, Document, House, SwitchButton, Tickets, User } from '@element-plus/icons-vue'
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
    <el-aside width="220px" class="sidebar">
      <div class="brand">宠物订单系统</div>
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
}

.sidebar {
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
}

.brand {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-size: 18px;
  font-weight: 700;
  border-bottom: 1px solid #e5e7eb;
}

.nav-menu {
  border-right: 0;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  font-weight: 600;
}

.content {
  background: #f5f7fb;
}
</style>
