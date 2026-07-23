import http from './http'

export function fetchHealthRecords(petId) {
  return http.get(`/pets/${petId}/health`)
}

export function createVaccineRecord(petId, data) {
  return http.post(`/pets/${petId}/health/vaccines`, data)
}

export function updateVaccineRecord(petId, id, data) {
  return http.put(`/pets/${petId}/health/vaccines/${id}`, data)
}

export function deleteVaccineRecord(petId, id) {
  return http.delete(`/pets/${petId}/health/vaccines/${id}`)
}

export function createDewormingRecord(petId, data) {
  return http.post(`/pets/${petId}/health/deworming-records`, data)
}

export function updateDewormingRecord(petId, id, data) {
  return http.put(`/pets/${petId}/health/deworming-records/${id}`, data)
}

export function deleteDewormingRecord(petId, id) {
  return http.delete(`/pets/${petId}/health/deworming-records/${id}`)
}

export function createWeightRecord(petId, data) {
  return http.post(`/pets/${petId}/health/weights`, data)
}

export function updateWeightRecord(petId, id, data) {
  return http.put(`/pets/${petId}/health/weights/${id}`, data)
}

export function deleteWeightRecord(petId, id) {
  return http.delete(`/pets/${petId}/health/weights/${id}`)
}
