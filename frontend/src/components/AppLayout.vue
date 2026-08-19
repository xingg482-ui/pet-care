<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  ChatDotRound,
  Collection,
  Document,
  Expand,
  Fold,
  House,
  Money,
  Picture,
  Shop,
  SwitchButton,
  Tickets,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { logout } from '../api/auth'
import AiFloatingAssistant from './AiFloatingAssistant.vue'

const SUPER_ADMIN = 'SUPER_ADMIN'
const ADMIN = 'ADMIN'
const CUSTOMER = 'CUSTOMER'
const STAFF_ROLES = [SUPER_ADMIN, ADMIN]

const route = useRoute()
const router = useRouter()
const role = computed(() => localStorage.getItem('petCareRole') || SUPER_ADMIN)
const activeMenu = computed(() => {
  if (route.path.startsWith('/orders')) {
    return '/orders'
  }
  return route.path
})
const displayName = ref(localStorage.getItem('petCareDisplayName') || localStorage.getItem('petCareUsername') || 'admin')
const avatarUrl = ref(localStorage.getItem('petCareAvatarUrl') || '')
const isSidebarCollapsed = ref(localStorage.getItem('petCareSidebarCollapsed') === 'true')
const roleName = computed(() => {
  const names = {
    [SUPER_ADMIN]: '高级管理员',
    [ADMIN]: '管理员',
    [CUSTOMER]: '客户',
  }
  return names[role.value] || '高级管理员'
})
const sidebarToggleIcon = computed(() => (isSidebarCollapsed.value ? Expand : Fold))

const menuItems = [
  { path: '/dashboard', label: '首页', icon: House, roles: STAFF_ROLES },
  { path: '/orders', label: '订单管理', icon: Document, roles: STAFF_ROLES },
  { path: '/customers', label: '客户管理', icon: User, roles: STAFF_ROLES },
  { path: '/pets', label: '宠物管理', icon: Collection, roles: STAFF_ROLES },
  { path: '/boarding', label: '宠物托管', icon: Shop, roles: STAFF_ROLES },
  { path: '/finance', label: '财务管理', icon: Money, roles: STAFF_ROLES },
  { path: '/ai-consult', label: 'AI 咨询', icon: ChatDotRound, roles: STAFF_ROLES },
  { path: '/accounts', label: '账号管理', icon: UserFilled, roles: [SUPER_ADMIN] },
  { path: '/support-tickets', label: '客服消息', icon: Bell, roles: STAFF_ROLES },
  { path: '/service-items', label: '服务项目', icon: Tickets, roles: STAFF_ROLES },
  { path: '/pet-avatars', label: '形象库', icon: Picture, roles: STAFF_ROLES },
  { path: '/my-home', label: '首页', icon: House, roles: [CUSTOMER] },
  { path: '/my-pets', label: '我的宠物', icon: Collection, roles: [CUSTOMER] },
  { path: '/my-orders', label: '订单管理', icon: Tickets, roles: [CUSTOMER] },
  { path: '/my-boarding', label: '宠物托管', icon: Shop, roles: [CUSTOMER] },
  { path: '/my-support', label: '联系客服', icon: Bell, roles: [CUSTOMER] },
  { path: '/my-ai-consult', label: 'AI 咨询', icon: ChatDotRound, roles: [CUSTOMER] },
  { path: '/profile', label: '我的资料', icon: User, roles: [SUPER_ADMIN, ADMIN, CUSTOMER] },
]
const visibleMenuItems = computed(() => menuItems.filter((item) => item.roles.includes(role.value)))

function syncUserInfo() {
  displayName.value = localStorage.getItem('petCareDisplayName') || localStorage.getItem('petCareUsername') || 'admin'
  avatarUrl.value = localStorage.getItem('petCareAvatarUrl') || ''
}

function goProfile() {
  router.push('/profile')
}

function toggleSidebar() {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
  localStorage.setItem('petCareSidebarCollapsed', String(isSidebarCollapsed.value))
}

async function handleLogout() {
  await logout().catch(() => {})
  localStorage.removeItem('petCareToken')
  localStorage.removeItem('petCareAccountId')
  localStorage.removeItem('petCareUsername')
  localStorage.removeItem('petCareDisplayName')
  localStorage.removeItem('petCareRole')
  localStorage.removeItem('petCareAvatarUrl')
  localStorage.removeItem('petCareCustomerId')
  router.push('/login')
}

onMounted(() => {
  window.addEventListener('pet-care-user-updated', syncUserInfo)
})

onUnmounted(() => {
  window.removeEventListener('pet-care-user-updated', syncUserInfo)
})
</script>

<template>
  <el-container class="app-shell">
    <el-aside :width="isSidebarCollapsed ? '76px' : '248px'" class="sidebar" :class="{ 'is-collapsed': isSidebarCollapsed }">
      <div class="brand">
        <span class="brand-mark" aria-hidden="true">
          <span class="paw-print">
            <span class="toe toe-1"></span>
            <span class="toe toe-2"></span>
            <span class="toe toe-3"></span>
            <span class="toe toe-4"></span>
            <span class="pad"></span>
          </span>
        </span>
        <span class="brand-text">宠物订单系统</span>
      </div>
      <el-menu router :default-active="activeMenu" :collapse="isSidebarCollapsed" class="nav-menu">
        <el-menu-item v-for="item in visibleMenuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
      <button class="collapse-entry" type="button" @click="toggleSidebar">
        <el-icon><component :is="sidebarToggleIcon" /></el-icon>
        <span>{{ isSidebarCollapsed ? '展开菜单' : '收起菜单' }}</span>
      </button>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div class="topbar-actions">
          <el-tag class="role-tag" type="warning">{{ roleName }}</el-tag>
          <button class="user-menu" type="button" @click="goProfile">
            <span class="avatar">
              <img v-if="avatarUrl" :src="avatarUrl" alt="头像" />
              <el-icon v-else><UserFilled /></el-icon>
            </span>
            <span>{{ displayName }}</span>
          </button>
          <el-button :icon="SwitchButton" text @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="content">
        <slot />
      </el-main>
    </el-container>
    <AiFloatingAssistant />
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f8fafc;
}

.sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #ffffff;
  box-shadow: none;
  overflow-x: hidden;
  transition: width 180ms ease;
}

.brand {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 28px;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0;
  color: #111827;
  border-bottom: 1px solid #edf0f5;
  transition: padding 180ms ease;
}

.brand-mark {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(180deg, #fffaf0 0%, #fff5e6 100%);
  border: 1px solid #fff0d6;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.85), 0 8px 18px rgba(245, 158, 11, 0.08);
  font-size: 24px;
}

.brand-text {
  white-space: nowrap;
  transition: opacity 140ms ease;
}

.paw-print {
  position: relative;
  width: 29px;
  height: 28px;
  display: block;
}

.paw-print span {
  position: absolute;
  display: block;
  background: #ec8f00;
  box-shadow: inset 0 -1px 1px rgba(170, 86, 0, 0.14);
}

.toe {
  width: 7px;
  height: 10px;
  border-radius: 50%;
}

.toe-1 {
  left: 0;
  top: 10px;
  transform: rotate(-15deg);
}

.toe-2 {
  left: 7px;
  top: 2px;
  transform: rotate(-6deg);
}

.toe-3 {
  right: 7px;
  top: 2px;
  transform: rotate(6deg);
}

.toe-4 {
  right: 0;
  top: 10px;
  transform: rotate(15deg);
}

.pad {
  left: 7px;
  bottom: 1px;
  width: 15px;
  height: 15px;
  border-radius: 58% 58% 64% 64%;
}

.nav-menu {
  flex: 1;
  overflow-y: auto;
  border-right: 1px solid #edf0f5;
  padding: 28px 12px;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #64748b;
  --el-menu-hover-bg-color: #fff9ec;
  --el-menu-active-color: #111827;
}

.nav-menu :deep(.el-menu-item) {
  position: relative;
  height: 46px;
  margin-bottom: 12px;
  padding: 0 26px !important;
  border-radius: 8px;
  color: #667085;
  font-size: 15px;
  font-weight: 600;
  transition: background-color 180ms ease, color 180ms ease, box-shadow 180ms ease, transform 150ms ease;
}

.nav-menu :deep(.el-menu-item:hover) {
  background: #fff8e8;
  color: #111827;
  transform: translateX(1px);
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #fff4d8 0%, #fffaf0 100%);
  color: #111827;
  box-shadow: none;
}

.nav-menu :deep(.el-menu-item.is-active::before) {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 46px;
  border-radius: 0 999px 999px 0;
  background: #f6a700;
}

.nav-menu :deep(.el-icon) {
  color: inherit;
  margin-right: 14px;
  font-size: 20px;
}

.collapse-entry {
  flex: 0 0 auto;
  height: 58px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 32px;
  border: 0;
  border-top: 1px solid #edf0f5;
  border-right: 1px solid #edf0f5;
  background: #ffffff;
  color: #667085;
  font-size: 14px;
  cursor: pointer;
  transition: color 180ms ease, background-color 180ms ease;
}

.collapse-entry .el-icon {
  flex: 0 0 auto;
  font-size: 18px;
}

.collapse-entry:hover {
  color: #111827;
  background: #fffaf2;
}

.sidebar.is-collapsed .brand {
  justify-content: center;
  padding: 0;
}

.sidebar.is-collapsed .brand-text,
.sidebar.is-collapsed .collapse-entry span {
  opacity: 0;
  width: 0;
  overflow: hidden;
}

.sidebar.is-collapsed .nav-menu {
  width: 100%;
  padding: 28px 8px;
}

.sidebar.is-collapsed .nav-menu :deep(.el-menu-item) {
  justify-content: center;
  width: 44px;
  height: 44px;
  margin: 0 auto 12px;
  padding: 0 !important;
}

.sidebar.is-collapsed .nav-menu :deep(.el-icon) {
  margin-right: 0;
}

.sidebar.is-collapsed .collapse-entry {
  justify-content: center;
  gap: 0;
  padding: 0;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 76px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #edf0f5;
  color: #111827;
  font-size: 17px;
  font-weight: 700;
  backdrop-filter: blur(16px);
}

.topbar-actions,
.user-menu {
  display: flex;
  align-items: center;
}

.topbar-actions {
  gap: 16px;
}

.role-tag {
  flex: 0 0 auto;
}

.user-menu {
  height: 40px;
  gap: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #ffffff;
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 150ms ease;
}

.user-menu {
  border-color: transparent;
  padding: 0 4px 0 0;
}

.user-menu:hover {
  border-color: #f6c453;
  box-shadow: 0 8px 18px rgba(245, 158, 11, 0.12);
  transform: translateY(-1px);
}

.avatar {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(180deg, #facc15 0%, #f59e0b 100%);
  color: #ffffff;
  font-size: 20px;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.content {
  background: #fbfcfe;
  padding: 28px;
}
</style>
