import axios from 'axios'

/**
 * 带登录态（Authorization）的文件下载。
 * window.open 无法携带请求头，导出接口受登录拦截器保护，因此统一使用
 * fetch 携带 token 下载 Blob，再触发浏览器保存。
 */
export async function downloadWithAuth(url) {
  const token = localStorage.getItem('token')
  const res = await axios.get(url, {
    baseURL: '/api',
    responseType: 'blob',
    headers: token ? { Authorization: token } : {}
  })
  const blob = new Blob([res.data])
  const disposition = res.headers['content-disposition'] || ''
  const match = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  let filename = 'export.xlsx'
  if (match) {
    try { filename = decodeURIComponent(match[1]) } catch { filename = match[1] }
  }
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
}

export default downloadWithAuth
