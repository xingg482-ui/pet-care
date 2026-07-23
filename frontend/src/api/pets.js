import http from './http'

export function fetchPets(params) {
  return http.get('/pets', { params })
}

export function fetchCustomerPets(customerId, onlyEnabled = false) {
  return http.get(`/customers/${customerId}/pets`, { params: { onlyEnabled } })
}

export function createPet(data) {
  return http.post('/pets', data)
}

export function updatePet(id, data) {
  return http.put(`/pets/${id}`, data)
}

export function updatePetStatus(id, status) {
  return http.put(`/pets/${id}/status`, null, { params: { status } })
}
