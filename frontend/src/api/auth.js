import http from './http'

export function login(data) {
  return http.post('/auth/login', data)
}

export function register(data) {
  return http.post('/auth/register', data)
}

export function checkUsernameAvailable(username) {
  return http.get('/auth/username-available', { params: { username } })
}

export function getCurrentAccount() {
  return http.get('/auth/me')
}

export function fetchProfile() {
  return http.get('/profile')
}

export function updateProfile(data) {
  return http.put('/profile', data)
}

export function restoreDefaultAvatar() {
  return http.delete('/profile/avatar')
}

export function changePassword(data) {
  return http.put('/auth/password', data)
}

export function logout() {
  return http.post('/auth/logout')
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/files/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
