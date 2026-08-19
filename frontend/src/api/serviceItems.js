import http from './http'

export function fetchServiceItems(params) {
  return http.get('/service-items', { params })
}

export function fetchEnabledServiceItems() {
  return http.get('/service-items/enabled')
}

export function createServiceItem(data) {
  return http.post('/service-items', data)
}

export function updateServiceItem(id, data) {
  return http.put(`/service-items/${id}`, data)
}

export function updateServiceItemStatus(id, status) {
  return http.put(`/service-items/${id}/status`, null, { params: { status } })
}

export function deleteServiceItem(id) {
  return http.delete(`/service-items/${id}`)
}
