import http from './http'

export function chatWithAi(data) {
  return http.post('/ai/chat', data, { timeout: 45000 })
}

export function fetchAiFaqs() {
  return http.get('/ai/faqs')
}

export function fetchAiBusinessSummary() {
  return http.get('/ai/business-summary')
}

export function fetchAiSessions() {
  return http.get('/ai/sessions')
}

export function fetchAiSession(id) {
  return http.get(`/ai/sessions/${id}`)
}
