import http from './http'

export function fetchCustomers(params) {
  return http.get('/customers', { params })
}

export function createCustomer(data) {
  return http.post('/customers', data)
}

export function updateCustomer(id, data) {
  return http.put(`/customers/${id}`, data)
}

export function updateCustomerStatus(id, status) {
  return http.put(`/customers/${id}/status`, null, { params: { status } })
}

export function deleteCustomer(id) {
  return http.delete(`/customers/${id}`)
}
