const ROLE_MENUS = {
  admin: [
    '/inbound/purchase', '/inbound/return', '/inbound/records',
    '/outbound/picking', '/outbound/records',
    '/inventory/search', '/inventory/query', '/inventory/alert', '/inventory/flow',
    '/report/inventory-detail', '/report/inbound-stats', '/report/outbound-stats', '/report/stagnant', '/report/export',
    '/system/users', '/system/roles', '/system/backup', '/system/logs', '/system/password'
  ],
  warehouse: [
    '/inbound/purchase', '/inbound/return', '/inbound/records',
    '/outbound/picking', '/outbound/records',
    '/inventory/search', '/inventory/query', '/inventory/alert', '/inventory/flow',
    '/report/inventory-detail', '/report/inbound-stats', '/report/outbound-stats', '/report/stagnant', '/report/export',
    '/system/password'
  ],
  engineer: [
    '/inventory/search', '/inventory/query',
    '/outbound/picking',
    '/system/password'
  ],
  purchaser: [
    '/inbound/records',
    '/inventory/search', '/inventory/query', '/inventory/alert', '/inventory/flow',
    '/report/inventory-detail', '/report/inbound-stats', '/report/stagnant',
    '/system/password'
  ],
  inspector: [
    '/inbound/records',
    '/inventory/search', '/inventory/query', '/inventory/flow',
    '/system/password'
  ],
  manager: [
    '/inbound/purchase', '/inbound/return', '/inbound/records',
    '/outbound/picking', '/outbound/records',
    '/inventory/search', '/inventory/query', '/inventory/alert', '/inventory/flow',
    '/report/inventory-detail', '/report/inbound-stats', '/report/outbound-stats', '/report/stagnant', '/report/export',
    '/system/password'
  ]
}

export function getRole() {
  return localStorage.getItem('role') || 'engineer'
}

export function canAccess(path) {
  const role = getRole()
  const menus = ROLE_MENUS[role] || ROLE_MENUS.engineer
  return menus.some(m => path === m || path.startsWith(m + '/'))
}

export function getFirstAllowedMenu(role) {
  const menus = ROLE_MENUS[role] || ROLE_MENUS.engineer
  return menus.length ? menus[0] : '/inventory/search'
}

export function getRoleMenus() {
  return ROLE_MENUS
}
