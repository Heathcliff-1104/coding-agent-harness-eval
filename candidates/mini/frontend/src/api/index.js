import request from '@/utils/request'

// ====================== 用户 ======================
export const userApi = {
  captcha: ()             => request({ url: '/user/captcha', method: 'get' }),
  login: (data)           => request({ url: '/user/login', method: 'post', data }),
  register: (data)        => request({ url: '/user/register', method: 'post', data }),
  info: ()                => request({ url: '/user/info', method: 'get' }),
  changePwd: (data)       => request({ url: '/user/changePwd', method: 'put', data }),
  list: ()                => request({ url: '/user/list', method: 'get' }),
  page: (params)          => request({ url: '/user/page', method: 'get', params }),
  update: (data)          => request({ url: '/user/update', method: 'put', data }),
  delete: (id)            => request({ url: '/user/delete/' + id, method: 'delete' }),
  resetPwd: (id)          => request({ url: '/user/resetPwd/' + id, method: 'post' }),
  adminCreate: (data)     => request({ url: '/user/admin/create', method: 'post', data }),
  importUsers: (data)     => request({ url: '/user/import', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' } }),
  dingtalkAuthUrl: (p)    => request({ url: '/user/dingtalk/auth-url', method: 'get', params: p }),
  dingtalkLogin: (data)   => request({ url: '/user/dingtalk/login', method: 'post', data }),
}

// ====================== 入库 ======================
export const inboundApi = {
  page: (params)          => request({ url: '/inbound/page', method: 'get', params }),
  get: (id)               => request({ url: '/inbound/get/' + id, method: 'get' }),
  saveDraft: (data)       => request({ url: '/inbound/saveDraft', method: 'post', data }),
  saveOrder: (data)       => request({ url: '/inbound/saveOrder', method: 'post', data }),
  confirm: (id, operUser) => request({ url: '/inbound/confirm/' + id, method: 'post', params: { operUser } }),
  updateStatus: (params)  => request({ url: '/inbound/updateStatus', method: 'post', params }),
  export: (id)            => `/api/inbound/export/${id}`,
}

// ====================== 出库 ======================
export const outboundApi = {
  page: (params)             => request({ url: '/outbound/page', method: 'get', params }),
  get: (id)                  => request({ url: '/outbound/get/' + id, method: 'get' }),
  saveDraft: (data)          => request({ url: '/outbound/saveDraft', method: 'post', data }),
  editDraft: (id, data)      => request({ url: '/outbound/editDraft/' + id, method: 'put', data }),
  saveOrder: (data)          => request({ url: '/outbound/saveOrder', method: 'post', data }),
  confirm: (id, operUser)    => request({ url: '/outbound/confirm/' + id, method: 'post', params: { operUser } }),
  reject: (id)               => request({ url: '/outbound/reject/' + id, method: 'post' }),
  export: (id)               => `/api/outbound/export/${id}`,
  bomImport: (data)          => request({ url: '/outbound/bom/import', method: 'post', data }),
  bomMatch: (data)           => request({ url: '/outbound/bom/match', method: 'post', data }),
  bomSavePlan: (data)        => request({ url: '/outbound/bom/savePlan', method: 'post', data }),
  bomVersions: ()            => request({ url: '/outbound/bom/versions', method: 'get' }),
}

// ====================== 物料 ======================
export const materialApi = {
  page: (params)    => request({ url: '/material/page', method: 'get', params }),
  list: ()          => request({ url: '/material/list', method: 'get' }),
  get: (id)         => request({ url: '/material/' + id, method: 'get' }),
  add: (data)       => request({ url: '/material/add', method: 'post', data }),
  update: (data)    => request({ url: '/material/update', method: 'put', data }),
  del: (id)         => request({ url: '/material/del/' + id, method: 'delete' }),
}

// ====================== 库存 ======================
export const inventoryApi = {
  stockFlow: (params)   => request({ url: '/stockFlow/page', method: 'get', params }),
  stockAlert: (params)  => request({ url: '/stockAlert/page', method: 'get', params }),
  handleAlert: (id, handler, method) => request({ url: '/stockAlert/handle/' + id, method: 'post', params: { handler, method } }),
  manualScan: ()        => request({ url: '/stockAlert/scan', method: 'post' }),
  restockPage: (params) => request({ url: '/restock/page', method: 'get', params }),
  createRestock: (data) => request({ url: '/restock/create', method: 'post', data }),
  handleRestock: (id, method) => request({ url: '/restock/handle/' + id, method: 'post', params: { method } }),
  updateThreshold: (params) => request({ url: '/material/threshold', method: 'put', params }),
}

// ====================== 报表统计 ======================
export const reportApi = {
  inboundStats: (params) => request({ url: '/statistics/inbound', method: 'get', params }),
  outboundStats: (params) => request({ url: '/statistics/outbound', method: 'get', params }),
  stagnant: (params)     => request({ url: '/statistics/stagnant', method: 'get', params }),
  exportInbound: (params) => `/api/statistics/exportInbound?start=${params.start}&end=${params.end}`,
  exportOutbound: (params) => `/api/statistics/exportOutbound?start=${params.start}&end=${params.end}`,
  exportStagnant: (days) => `/api/statistics/exportStagnant?days=${days}`,
}

// ====================== 系统 ======================
export const roleApi = {
  list: ()                => request({ url: '/role/list', method: 'get' }),
  menus: (roleCode)       => request({ url: '/role/menus', method: 'get', params: { roleCode } }),
  save: (data)            => request({ url: '/role/save', method: 'post', data }),
}

export const systemApi = {
  loginLog: (params)  => request({ url: '/loginLog/page', method: 'get', params }),
  sysLog: (params)    => request({ url: '/sysLog/page', method: 'get', params }),
  backup: ()          => request({ url: '/backup/db', method: 'post' }),
  backupRecords: (params) => request({ url: '/backup/records', method: 'get', params }),
  backupStrategy: ()   => request({ url: '/backup/strategy', method: 'get' }),
  saveBackupStrategy: (params) => request({ url: '/backup/strategy', method: 'post', params }),
  inRecord: (params)  => request({ url: '/inRecord/page', method: 'get', params }),
  outRecord: (params) => request({ url: '/outRecord/page', method: 'get', params }),
}
