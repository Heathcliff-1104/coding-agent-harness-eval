/**
 * 权限工具：菜单/按钮权限由后端 /user/info 返回的权限编码列表驱动（localStorage 缓存）。
 * 兼容旧数据：localStorage 无权限列表时回退到角色判断。
 */

/** 路由顺序：登录后跳转到用户有权访问的第一个菜单 */
const ROUTE_ORDER = [
  '/inbound/purchase', '/inbound/return', '/inbound/records',
  '/outbound/picking', '/outbound/records',
  '/inventory/search', '/inventory/query', '/inventory/alert', '/inventory/flow',
  '/report/inventory-detail', '/report/inbound-stats', '/report/outbound-stats', '/report/stagnant', '/report/export',
  '/system/users', '/system/roles', '/system/backup', '/system/logs', '/system/password'
]

export function getRole() {
  return localStorage.getItem('role') || 'engineer'
}

export function getPermissions() {
  try {
    const raw = localStorage.getItem('permissions')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function setPermissions(list) {
  localStorage.setItem('permissions', JSON.stringify(list || []))
}

export function clearPermissions() {
  localStorage.removeItem('permissions')
}

/**
 * 判断当前用户是否拥有某权限编码（菜单路由路径 或 按钮编码）
 */
export function hasPerm(code) {
  if (!code) return true
  const perms = getPermissions()
  if (perms && Array.isArray(perms)) {
    return perms.includes(code)
  }
  // 回退：按角色判断（旧数据兼容）
  const role = getRole()
  if (role === 'admin') return true
  if (role === 'warehouse' || role === 'manager') {
    return !(code.startsWith('/system/')) || code === '/system/password'
  }
  if (role === 'engineer') {
    return ['/outbound/picking', '/inventory/search', '/inventory/query', '/system/password',
      'bom:import', 'bom:match', 'bom:plan'].includes(code)
  }
  if (role === 'purchaser') {
    return ['/inbound/records', '/inventory/search', '/inventory/query', '/inventory/alert',
      '/inventory/flow', '/report/inventory-detail', '/report/inbound-stats', '/report/stagnant',
      '/system/password', 'alert:handle', 'replenishment:apply', 'report:export'].includes(code)
  }
  return ['/inbound/records', '/inventory/search', '/inventory/query', '/inventory/flow', '/system/password'].includes(code)
}

export function canAccess(path) {
  return hasPerm(path)
}

/**
 * 登录后跳转目标：用户有权限的第一个路由。
 * - 权限列表可用时按 ROUTE_ORDER 顺序取第一个有权限的菜单；
 * - 无权限列表（旧数据）时按角色兜底：admin/warehouse -> 采购入库，其他（如工程师）-> 生产领料。
 */
export function firstPermittedRoute() {
  const role = getRole()
  const fallback = (role === 'admin' || role === 'warehouse') ? '/inbound/purchase' : '/outbound/picking'
  const perms = getPermissions()
  if (perms && Array.isArray(perms) && perms.length) {
    const hit = ROUTE_ORDER.find(r => perms.includes(r))
    return hit || fallback
  }
  return fallback
}
