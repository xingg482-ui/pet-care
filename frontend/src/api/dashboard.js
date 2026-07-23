import http from './http'

export function fetchDashboardSummary() {
  return http.get('/dashboard/summary')
}

export function fetchDashboardRevenueTrend() {
  return http.get('/dashboard/revenue-trend')
}

export function fetchDashboardServiceRevenue() {
  return http.get('/dashboard/service-revenue')
}

export function fetchDashboardTodayActiveOrders() {
  return http.get('/dashboard/today-active-orders')
}
