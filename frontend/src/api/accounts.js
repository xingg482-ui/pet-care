import http from './http'

export function fetchAccounts(params) {
  return http.get('/accounts', { params })
}

export function approveAccount(id) {
  return http.put(`/accounts/${id}/approve`)
}

export function rejectAccount(id, reason) {
  return http.put(`/accounts/${id}/reject`, { reason })
}

export function updateAccountStatus(id, status, reason) {
  return http.put(`/accounts/${id}/status`, { status, reason })
}
