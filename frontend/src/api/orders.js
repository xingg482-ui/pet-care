import http from './http'

export function fetchOrders(params) {
  return http.get('/orders', { params })
}

export function fetchMyOrders(params) {
  return http.get('/my/orders', { params })
}

export function fetchOrder(id) {
  return http.get(`/orders/${id}`)
}

export function fetchMyOrder(id) {
  return http.get(`/my/orders/${id}`)
}

export function createOrder(data) {
  return http.post('/orders', data)
}

export function createMyOrder(data) {
  return http.post('/my/orders', data)
}

export function cancelMyOrder(id) {
  return http.put(`/my/orders/${id}/cancel`)
}

export function payMyOrder(id, data) {
  return http.post(`/my/orders/${id}/pay`, data)
}

export function confirmOrderPayment(id, data) {
  return http.post(`/orders/${id}/payment-confirm`, data)
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
