import request from '@/utils/request'

// ====================== 用户 ======================
export const userApi = {
  captcha: ()             => request({ url: '/user/captcha', method: 'get' }),
  check: (params)         => request({ url: '/user/check', method: 'get', params }),
  login: (data)           => request({ url: '/user/login', method: 'post', data }),
  register: (data)        => request({ url: '/user/register', method: 'post', data }),
  info: ()                => request({ url: '/user/info', method: 'get' }),
  logout: ()              => request({ url: '/user/logout', method: 'post' }),
  changePwd: (data)       => request({ url: '/user/changePwd', method: 'put', data }),
  list: ()                => request({ url: '/user/list', method: 'get' }),
  page: (params)          => request({ url: '/user/page', method: 'get', params }),
  add: (data)             => request({ url: '/user/add', method: 'post', data }),
  update: (data)          => request({ url: '/user/update', method: 'put', data }),
  delete: (id)            => request({ url: '/user/delete/' + id, method: 'delete' }),
  resetPwd: (id)          => request({ url: '/user/resetPwd/' + id, method: 'post' }),
  importUsers: (data)     => request({ url: '/user/import', method: 'post', data }),
  dingtalkAuthUrl: (p)    => request({ url: '/user/dingtalk/auth-url', method: 'get', params: p }),
  dingtalkLogin: (data)   => request({ url: '/user/dingtalk/login', method: 'post', data }),
}

// ====================== 角色权限 ======================
export const roleApi = {
  list: ()                  => request({ url: '/role/list', method: 'get' }),
  page: (params)            => request({ url: '/role/page', method: 'get', params }),
  permissionList: ()        => request({ url: '/role/permissionList', method: 'get' }),
  rolePermissions: (code)   => request({ url: '/role/permissions/' + code, method: 'get' }),
  save: (data)              => request({ url: '/role/save', method: 'post', data }),
  updatePermissions: (data) => request({ url: '/role/updatePermissions', method: 'post', data }),
  delete: (id)              => request({ url: '/role/delete/' + id, method: 'delete' }),
}

// ====================== 入库 ======================
export const inboundApi = {
  page: (params)          => request({ url: '/inbound/page', method: 'get', params }),
  get: (id)               => request({ url: '/inbound/get/' + id, method: 'get' }),
  saveDraft: (data)       => request({ url: '/inbound/saveDraft', method: 'post', data }),
  saveOrder: (data)       => request({ url: '/inbound/saveOrder', method: 'post', data }),
  confirm: (id)           => request({ url: '/inbound/confirm/' + id, method: 'post' }),
  updateStatus: (params)  => request({ url: '/inbound/updateStatus', method: 'post', params }),
  batchAudit: (params)    => request({ url: '/inbound/batchAudit', method: 'post', params }),
  export: (id)            => `/api/inbound/export/${id}`,
}

// ====================== 出库 ======================
export const outboundApi = {
  page: (params)             => request({ url: '/outbound/page', method: 'get', params }),
  get: (id)                  => request({ url: '/outbound/get/' + id, method: 'get' }),
  saveDraft: (data)          => request({ url: '/outbound/saveDraft', method: 'post', data }),
  editDraft: (id, data)      => request({ url: '/outbound/editDraft/' + id, method: 'put', data }),
  saveOrder: (data)          => request({ url: '/outbound/saveOrder', method: 'post', data }),
  confirm: (id)             => request({ url: '/outbound/confirm/' + id, method: 'post' }),
  reject: (id)               => request({ url: '/outbound/reject/' + id, method: 'post' }),
  export: (id)               => `/api/outbound/export/${id}`,
  bomImport: (data)          => request({ url: '/outbound/bom/import', method: 'post', data }),
  bomMatch: (data)           => request({ url: '/outbound/bom/match', method: 'post', data }),
  bomPlan: (params)          => request({ url: '/outbound/bom/plan', method: 'get', params }),
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
  purchaseRequest: (data) => request({ url: '/stockAlert/purchaseRequest', method: 'post', data }),
}

// ====================== 报表统计 ======================
export const reportApi = {
  inboundStats: (params) => request({ url: '/statistics/inbound', method: 'get', params }),
  outboundStats: (params) => request({ url: '/statistics/outbound', method: 'get', params }),
  inboundByMaterial: (params) => request({ url: '/statistics/inbound/material', method: 'get', params }),
  inboundBySupplier: (params) => request({ url: '/statistics/inbound/supplier', method: 'get', params }),
  outboundByMaterial: (params) => request({ url: '/statistics/outbound/material', method: 'get', params }),
  outboundByDept: (params) => request({ url: '/statistics/outbound/dept', method: 'get', params }),
  stagnant: (params)     => request({ url: '/statistics/stagnant', method: 'get', params }),
  exportInbound: (params) => `/api/statistics/exportInbound?start=${params.start}&end=${params.end}`,
  exportOutbound: (params) => `/api/statistics/exportOutbound?start=${params.start}&end=${params.end}`,
  exportStagnant: (days) => `/api/statistics/exportStagnant?days=${days}`,
}

// ====================== 系统 ======================
export const systemApi = {
  loginLog: (params)  => request({ url: '/loginLog/page', method: 'get', params }),
  sysLog: (params)    => request({ url: '/sysLog/page', method: 'get', params }),
  backup: ()          => request({ url: '/backup/db', method: 'post' }),
  backupConfig: ()    => request({ url: '/backup/config', method: 'get' }),
  saveBackupConfig: (data) => request({ url: '/backup/config', method: 'post', data }),
  inRecord: (params)  => request({ url: '/inRecord/page', method: 'get', params }),
  outRecord: (params) => request({ url: '/outRecord/page', method: 'get', params }),
}

// ====================== CIS同步 ======================
export const cisApi = {
  syncFull: ()        => request({ url: '/cis/sync/full', method: 'post' }),
  syncIncremental: () => request({ url: '/cis/sync/incremental', method: 'post' }),
  syncLog: (params)   => request({ url: '/cis/sync/log', method: 'get', params }),
}
