import http from './http'

export function fetchPetAvatarLibrary(params) {
  return http.get('/pet-avatar-library', { params })
}

export function matchPetAvatar(params) {
  return http.get('/pet-avatar-library/match', { params })
}

export function createPetAvatarLibraryItem(data) {
  return http.post('/pet-avatar-library', data)
}

export function updatePetAvatarLibraryItem(id, data) {
  return http.put(`/pet-avatar-library/${id}`, data)
}

export function updatePetAvatarLibraryStatus(id, status) {
  return http.put(`/pet-avatar-library/${id}/status`, null, { params: { status } })
}

export function uploadPetAvatarLibraryImage(id, file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post(`/pet-avatar-library/${id}/avatar`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
