import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

request.interceptors.response.use(
  res => {
    const body = res.data
    if (body && body.code === 401) {
      localStorage.clear()
      window.location.href = '/login'
      return Promise.reject(new Error('未登录'))
    }
    if (body && body.code === 403) {
      ElMessage.warning('权限不足')
      return Promise.reject(new Error('权限不足'))
    }
    return body
  },
  err => {
    if (err.response) {
      const status = err.response.status
      if (status === 401) {
        localStorage.clear()
        window.location.href = '/login'
      } else if (status === 403) {
        ElMessage.warning('权限不足')
      } else if (status >= 500) {
        ElMessage.error('服务器异常，请稍后重试')
      } else {
        const msg = err.response.data?.msg || '请求失败'
        ElMessage.error(msg)
      }
    } else if (err.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请重试')
    } else {
      ElMessage.error('网络异常，请检查连接')
    }
    return Promise.reject(err)
  }
)

export default request
