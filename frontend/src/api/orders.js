import http from './http'

export function fetchOrders(params) {
  return http.get('/orders', { params })
}

export function fetchOrder(id) {
  return http.get(`/orders/${id}`)
}

export function createOrder(data) {
  return http.post('/orders', data)
}

export function updateOrderStatus(id, data) {
  return http.put(`/orders/${id}/status`, data)
}

export function updateAppointmentTime(id, data) {
  return http.put(`/orders/${id}/appointment-time`, data)
}

export function fetchOrderStatusLogs(id) {
  return http.get(`/orders/${id}/status-logs`)
}
