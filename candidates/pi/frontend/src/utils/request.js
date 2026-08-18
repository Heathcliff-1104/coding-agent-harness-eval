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

/**
 * 通过 Blob 下载导出文件（GET + Authorization 头 + 浏览器对象URL下载）
 * @param {string} url 相对接口路径（如 /statistics/exportInbound）
 * @param {object} params 查询参数
 * @param {string} filename 下载文件名
 */
export async function downloadBlob(url, params = {}, filename = 'download.xlsx') {
  try {
    const token = localStorage.getItem('token')
    const res = await axios.get(url, {
      baseURL: '/api',
      params,
      responseType: 'blob',
      timeout: 120000,
      headers: token ? { Authorization: token } : {}
    })
    // 若后端返回的是 JSON 错误（非文件），尝试解析提示
    if (res.data && res.data.type === 'application/json') {
      const text = await res.data.text()
      try {
        const json = JSON.parse(text)
        ElMessage.error(json.msg || '导出失败')
      } catch {
        ElMessage.error('导出失败')
      }
      return
    }
    const blob = new Blob([res.data])
    const objectURL = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectURL
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(objectURL)
  } catch (e) {
    if (e.response) {
      const status = e.response.status
      if (status === 401) {
        localStorage.clear()
        window.location.href = '/login'
        ElMessage.error('请重新登录')
      } else if (status === 403) {
        ElMessage.error('无权限')
      } else if (status === 404) {
        ElMessage.error('接口不存在')
      } else {
        ElMessage.error('导出失败，请重试')
      }
    } else {
      ElMessage.error('导出失败，请重试')
    }
  }
}

export default request
