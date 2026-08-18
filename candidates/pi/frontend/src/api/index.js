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
  add: (data)             => request({ url: '/user/add', method: 'post', data }),
  checkUsername: (username) => request({ url: '/user/checkUsername', method: 'get', params: { username } }),
  checkPhone: (phone)     => request({ url: '/user/checkPhone', method: 'get', params: { phone } }),
  importUsers: (data)     => request({ url: '/user/import', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' }, timeout: 60000 }),
  dingtalkAuthUrl: (p)    => request({ url: '/user/dingtalk/auth-url', method: 'get', params: p }),
  dingtalkLogin: (data)   => request({ url: '/user/dingtalk/login', method: 'post', data }),
}

// ====================== 入库 ======================
export const inboundApi = {
  page: (params)          => request({ url: '/inbound/page', method: 'get', params }),
  get: (id)               => request({ url: '/inbound/get/' + id, method: 'get' }),
  saveDraft: (data)       => request({ url: '/inbound/saveDraft', method: 'post', data }),
  editDraft: (id, data)   => request({ url: '/inbound/editDraft/' + id, method: 'put', data }),
  saveOrder: (data)       => request({ url: '/inbound/saveOrder', method: 'post', data }),
  confirm: (id, operUser) => request({ url: '/inbound/confirm/' + id, method: 'post', params: { operUser } }),
  batchConfirm: (ids, operUser) => request({ url: '/inbound/batchConfirm', method: 'post', data: ids, params: { operUser } }),
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
  bomImport: (data)          => request({ url: '/outbound/bom/import', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' }, timeout: 60000 }),
  bomMatch: (data)           => request({ url: '/outbound/bom/match', method: 'post', data }),
  bomPlan: (data)            => request({ url: '/outbound/bom/plan', method: 'post', data }),
  bomHistory: (params)       => request({ url: '/outbound/bom/history', method: 'get', params }),
  bomHistoryDetail: (id)     => request({ url: '/outbound/bom/history/' + id, method: 'get' }),
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
}

// ====================== 补货申请 ======================
export const replenishmentApi = {
  apply: (data)   => request({ url: '/replenishment/apply', method: 'post', data }),
  page: (params)  => request({ url: '/replenishment/page', method: 'get', params }),
  handle: (id)    => request({ url: '/replenishment/handle/' + id, method: 'post' }),
}

// ====================== 报表统计 ======================
export const reportApi = {
  inboundStats: (params) => request({ url: '/statistics/inbound', method: 'get', params }),
  outboundStats: (params) => request({ url: '/statistics/outbound', method: 'get', params }),
  inboundBySupplier: (params) => request({ url: '/statistics/inboundBySupplier', method: 'get', params }),
  outboundByDept: (params) => request({ url: '/statistics/outboundByDept', method: 'get', params }),
  materialSummary: (params) => request({ url: '/statistics/materialSummary', method: 'get', params }),
  stagnant: (params)     => request({ url: '/statistics/stagnant', method: 'get', params }),
}

// ====================== 系统 ======================
export const systemApi = {
  loginLog: (params)  => request({ url: '/loginLog/page', method: 'get', params }),
  sysLog: (params)    => request({ url: '/sysLog/page', method: 'get', params }),
  backup: ()          => request({ url: '/backup/db', method: 'post' }),
  backupConfig: ()    => request({ url: '/backup/config', method: 'get' }),
  saveBackupConfig: (data) => request({ url: '/backup/config', method: 'put', data }),
  backupRecordPage: (params) => request({ url: '/backup/record/page', method: 'get', params }),
  inRecord: (params)  => request({ url: '/inRecord/page', method: 'get', params }),
  outRecord: (params) => request({ url: '/outRecord/page', method: 'get', params }),
}

// ====================== 角色权限 ======================
export const roleApi = {
  list: ()                  => request({ url: '/role/list', method: 'get' }),
  permissionTree: ()        => request({ url: '/role/permission-tree', method: 'get' }),
  rolePermissions: (id)     => request({ url: '/role/' + id + '/permissions', method: 'get' }),
  savePermissions: (id, data) => request({ url: '/role/' + id + '/permissions', method: 'put', data }),
  create: (data)            => request({ url: '/role', method: 'post', data }),
  update: (id, data)        => request({ url: '/role/' + id, method: 'put', data }),
  del: (id)                 => request({ url: '/role/' + id, method: 'delete' }),
}

// ====================== CIS 同步 ======================
export const cisApi = {
  full: ()            => request({ url: '/cis/sync/full', method: 'post' }),
  incremental: ()     => request({ url: '/cis/sync/incremental', method: 'post' }),
  logPage: (params)   => request({ url: '/cis/sync/log/page', method: 'get', params }),
}
