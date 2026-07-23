import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('petCareToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code !== 0) {
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(new Error(body.message || '操作失败'))
    }
    return body.data
  },
  (error) => {
    const message = error.response?.data?.message || (error.code === 'ERR_NETWORK' ? '后端服务不可用，请确认 8080 已启动' : error.message)
    ElMessage.error(message || '网络请求失败')
    return Promise.reject(error)
  },
)

export default http
