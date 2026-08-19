import http from './http'

export function fetchPets(params) {
  return http.get('/pets', { params })
}

export function fetchMyPets() {
  return http.get('/my/pets')
}

export function fetchCustomerPets(customerId, onlyEnabled = false) {
  return http.get(`/customers/${customerId}/pets`, { params: { onlyEnabled } })
}

export function createPet(data) {
  return http.post('/pets', data)
}

export function createMyPet(data) {
  return http.post('/my/pets', data)
}

export function updatePet(id, data) {
  return http.put(`/pets/${id}`, data)
}

export function updateMyPet(id, data) {
  return http.put(`/my/pets/${id}`, data)
}

export function updateMyPetStatus(id, status) {
  return http.put(`/my/pets/${id}/status`, null, { params: { status } })
}

export function updatePetStatus(id, status) {
  return http.put(`/pets/${id}/status`, null, { params: { status } })
}

export function deletePet(id) {
  return http.delete(`/pets/${id}`)
}

export function uploadPetAvatar(id, file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post(`/pets/${id}/avatar`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function uploadMyPetAvatar(id, file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post(`/my/pets/${id}/avatar`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function removePetAvatar(id) {
  return http.delete(`/pets/${id}/avatar`)
}

export function removeMyPetAvatar(id) {
  return http.delete(`/my/pets/${id}/avatar`)
}
