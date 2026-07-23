import http from './http'

export function fetchFinanceSummary() {
  return http.get('/finance/summary')
}

export function fetchFinanceServiceItems(params) {
  return http.get('/finance/service-items', { params })
}
