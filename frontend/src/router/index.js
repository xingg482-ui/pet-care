import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import LoginView from '../views/LoginView.vue'
import CustomersView from '../views/CustomersView.vue'
import PetsView from '../views/PetsView.vue'
import ServiceItemsView from '../views/ServiceItemsView.vue'
import OrdersView from '../views/OrdersView.vue'
import OrderCreateView from '../views/OrderCreateView.vue'
import OrderDetailView from '../views/OrderDetailView.vue'
import FinanceView from '../views/FinanceView.vue'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/dashboard', component: DashboardView },
  { path: '/customers', component: CustomersView },
  { path: '/pets', component: PetsView },
  { path: '/service-items', component: ServiceItemsView },
  { path: '/orders', component: OrdersView },
  { path: '/orders/create', component: OrderCreateView },
  { path: '/orders/:id', component: OrderDetailView },
  { path: '/finance', component: FinanceView },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const token = localStorage.getItem('petCareToken')
  if (!to.meta.public && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
})

export default router
