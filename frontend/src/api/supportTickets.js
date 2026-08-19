import http from './http'

export function createSupportTicket(data) {
  return http.post('/support/tickets', data)
}

export function queryPublicSupportTicket(params) {
  return http.get('/support/tickets/public-query', { params })
}

export function fetchLatestPublicConversation(contactInfo) {
  return http.get('/support/tickets/latest-public', { params: { contactInfo } })
}

export function fetchSupportTickets(params) {
  return http.get('/support/tickets', { params })
}

export function createMySupportTicket(data) {
  return http.post('/support/tickets/my', data)
}

export function fetchMySupportTickets(params) {
  return http.get('/support/tickets/my', { params })
}

export function fetchMySupportTicket(id) {
  return http.get(`/support/tickets/my/${id}`)
}

export function replyMySupportTicket(id, content) {
  return http.post(`/support/tickets/my/${id}/replies`, { content })
}

export function fetchSupportTicket(id) {
  return http.get(`/support/tickets/${id}`)
}

export function replySupportTicket(id, content) {
  return http.post(`/support/tickets/${id}/replies`, { content })
}

export function sendCustomerSupportMessage(id, data) {
  return http.post(`/support/tickets/${id}/customer-messages`, data)
}

export function updateSupportTicketStatus(id, status) {
  return http.put(`/support/tickets/${id}/status`, { status })
}
