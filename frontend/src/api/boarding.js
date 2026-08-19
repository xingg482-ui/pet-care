import http from './http'

export function fetchBoardingAreas() {
  return http.get('/boarding/areas')
}

export function createBoardingArea(data) {
  return http.post('/boarding/areas', data)
}

export function updateBoardingArea(id, data) {
  return http.put(`/boarding/areas/${id}`, data)
}

export function updateBoardingAreaStatus(id, status) {
  return http.put(`/boarding/areas/${id}/status`, null, { params: { status } })
}

export function fetchBoardingLocations(params) {
  return http.get('/boarding/locations', { params })
}

export function createBoardingLocation(data) {
  return http.post('/boarding/locations', data)
}

export function updateBoardingLocation(id, data) {
  return http.put(`/boarding/locations/${id}`, data)
}

export function updateBoardingLocationStatus(id, status) {
  return http.put(`/boarding/locations/${id}/status`, null, { params: { status } })
}

export function updateBoardingLocationCleanStatus(id, cleanStatus) {
  return http.put(`/boarding/locations/${id}/clean-status`, null, { params: { cleanStatus } })
}

export function fetchBoardingOrders(params) {
  return http.get('/boarding/orders', { params })
}

export function fetchMyBoardingOrders(params) {
  return http.get('/my/boarding', { params })
}

export function createBoardingOrder(data) {
  return http.post('/boarding/orders', data)
}

export function createMyBoardingOrder(data) {
  return http.post('/my/boarding', data)
}

export function fetchBoardingRoomStatus(params) {
  return http.get('/boarding/room-status', { params })
}

export function checkInBoardingOrder(id) {
  return http.put(`/boarding/orders/${id}/check-in`)
}

export function checkOutBoardingOrder(id) {
  return http.put(`/boarding/orders/${id}/check-out`)
}

export function confirmBoardingOrderPickedUp(id) {
  return http.put(`/boarding/orders/${id}/picked-up`)
}

export function confirmBoardingOrderPayment(id, data) {
  return http.post(`/boarding/orders/${id}/payment-confirm`, data)
}

export function cancelBoardingOrder(id) {
  return http.put(`/boarding/orders/${id}/cancel`)
}

export function cancelMyBoardingOrder(id) {
  return http.put(`/my/boarding/${id}/cancel`)
}

export function updateMyBoardingOrderSchedule(id, data) {
  return http.put(`/my/boarding/${id}/schedule`, data)
}

export function payMyBoardingOrder(id, data) {
  return http.post(`/my/boarding/${id}/pay`, data)
}

export function updateBoardingOrderCheckOutTime(id, data) {
  return http.put(`/boarding/orders/${id}/planned-check-out`, data)
}

export function changeBoardingOrderLocation(id, data) {
  return http.put(`/boarding/orders/${id}/location`, data)
}

export function updateBoardingOrderSchedule(id, data) {
  return http.put(`/boarding/orders/${id}/schedule`, data)
}

export function fetchBoardingInHousePets(params) {
  return http.get('/boarding/in-house-pets', { params })
}

export function fetchMyBoardingCareUpdates(params) {
  return http.get('/my/boarding/care-updates', { params })
}

export function createBoardingCareTasks(data) {
  return http.post('/boarding/care-tasks', data)
}

export function completeBoardingCareTask(id) {
  return http.put(`/boarding/care-tasks/${id}/complete`)
}

export function updateBoardingCareTaskRemark(id, data) {
  return http.put(`/boarding/care-tasks/${id}/remark`, data)
}
