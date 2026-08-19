import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import LoginView from '../views/LoginView.vue'
import CustomersView from '../views/CustomersView.vue'
import PetsView from '../views/PetsView.vue'
import ServiceItemsView from '../views/ServiceItemsView.vue'
import OrdersView from '../views/OrdersView.vue'
import OrderDetailView from '../views/OrderDetailView.vue'
import FinanceView from '../views/FinanceView.vue'
import BoardingView from '../views/BoardingView.vue'
import PetAvatarLibraryView from '../views/PetAvatarLibraryView.vue'
import AccountsView from '../views/AccountsView.vue'
import ProfileView from '../views/ProfileView.vue'
import SupportTicketsView from '../views/SupportTicketsView.vue'
import MyHomeView from '../views/MyHomeView.vue'
import MyPetsView from '../views/MyPetsView.vue'
import MyOrdersView from '../views/MyOrdersView.vue'
import MyBoardingView from '../views/MyBoardingView.vue'
import MySupportTicketsView from '../views/MySupportTicketsView.vue'
import AiConsultView from '../views/AiConsultView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'

const SUPER_ADMIN = 'SUPER_ADMIN'
const ADMIN = 'ADMIN'
const CUSTOMER = 'CUSTOMER'
const STAFF_ROLES = [SUPER_ADMIN, ADMIN]

function defaultPathForRole(role) {
  return role === CUSTOMER ? '/my-home' : '/dashboard'
}

const routes = [
  { path: '/', redirect: () => defaultPathForRole(localStorage.getItem('petCareRole') || SUPER_ADMIN) },
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/dashboard', component: DashboardView, meta: { roles: STAFF_ROLES } },
  { path: '/customers', component: CustomersView, meta: { roles: STAFF_ROLES } },
  { path: '/pets', component: PetsView, meta: { roles: STAFF_ROLES } },
  { path: '/service-items', component: ServiceItemsView, meta: { roles: STAFF_ROLES } },
  { path: '/boarding', component: BoardingView, meta: { roles: STAFF_ROLES } },
  { path: '/orders', component: OrdersView, meta: { roles: STAFF_ROLES } },
  { path: '/orders/create', redirect: '/orders', meta: { roles: STAFF_ROLES } },
  { path: '/orders/:id', component: OrderDetailView, meta: { roles: STAFF_ROLES } },
  { path: '/finance', component: FinanceView, meta: { roles: STAFF_ROLES } },
  { path: '/ai-consult', component: AiConsultView, meta: { roles: STAFF_ROLES } },
  { path: '/pet-avatars', component: PetAvatarLibraryView, meta: { roles: STAFF_ROLES } },
  { path: '/accounts', component: AccountsView, meta: { roles: [SUPER_ADMIN] } },
  { path: '/support-tickets', component: SupportTicketsView, meta: { roles: STAFF_ROLES } },
  { path: '/profile', component: ProfileView, meta: { roles: [SUPER_ADMIN, ADMIN, CUSTOMER] } },
  { path: '/my-home', component: MyHomeView, meta: { roles: [CUSTOMER] } },
  { path: '/my-pets', component: MyPetsView, meta: { roles: [CUSTOMER] } },
  { path: '/my-orders', component: MyOrdersView, meta: { roles: [CUSTOMER] } },
  { path: '/my-orders/:id', component: OrderDetailView, meta: { roles: [CUSTOMER] } },
  { path: '/my-boarding', component: MyBoardingView, meta: { roles: [CUSTOMER] } },
  { path: '/my-support', component: MySupportTicketsView, meta: { roles: [CUSTOMER] } },
  { path: '/my-ai-consult', component: AiConsultView, meta: { roles: [CUSTOMER] } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const token = localStorage.getItem('petCareToken')
  const role = localStorage.getItem('petCareRole') || SUPER_ADMIN
  if (!to.meta.public && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return defaultPathForRole(role)
  }
  if (token && to.meta.roles && !to.meta.roles.includes(role)) {
    return defaultPathForRole(role)
  }
})

export default router
