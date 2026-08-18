import { createRouter, createWebHistory } from 'vue-router'
import { canAccess } from '../utils/permission'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/login/index.vue') },
  {
    path: '/',
    component: () => import('../layout/index.vue'),
    redirect: '/inbound/purchase',
    children: [
      // ---------- 入库管理 ----------
      { path: 'inbound/purchase', name: 'InboundPurchase', component: () => import('../views/inbound/PurchaseInbound.vue') },
      { path: 'inbound/return',   name: 'InboundReturn',   component: () => import('../views/inbound/ReturnInbound.vue') },
      { path: 'inbound/records',  name: 'InboundRecords',  component: () => import('../views/inbound/InRecordList.vue') },
      // ---------- 出库管理 ----------
      { path: 'outbound/picking', name: 'OutboundPicking', component: () => import('../views/outbound/ProductionPicking.vue') },
      { path: 'outbound/records', name: 'OutboundRecords', component: () => import('../views/outbound/OutRecordList.vue') },
      // ---------- 库存管理 ----------
      { path: 'inventory/search', name: 'MaterialSearch', component: () => import('../views/inventory/MaterialSearch.vue') },
      { path: 'inventory/query',  name: 'InventoryQuery', component: () => import('../views/inventory/InventoryQuery.vue') },
      { path: 'inventory/alert',  name: 'StockAlert',     component: () => import('../views/inventory/StockAlert.vue') },
      { path: 'inventory/flow',   name: 'StockFlow',      component: () => import('../views/inventory/StockFlow.vue') },
      // ---------- 报表统计 ----------
      { path: 'report/inventory-detail', name: 'ReportInventoryDetail', component: () => import('../views/report/InventoryDetail.vue') },
      { path: 'report/inbound-stats',    name: 'ReportInboundStats',    component: () => import('../views/report/InboundStats.vue') },
      { path: 'report/outbound-stats',   name: 'ReportOutboundStats',   component: () => import('../views/report/OutboundStats.vue') },
      { path: 'report/stagnant',         name: 'ReportStagnant',        component: () => import('../views/report/Stagnant.vue') },
      { path: 'report/export',           name: 'ReportExport',          component: () => import('../views/report/ExportCenter.vue') },
      // ---------- 系统管理 ----------
      { path: 'system/users',    name: 'SystemUsers',    component: () => import('../views/system/UserManage.vue') },
      { path: 'system/roles',    name: 'SystemRoles',    component: () => import('../views/system/RoleManage.vue') },
      { path: 'system/backup',   name: 'SystemBackup',   component: () => import('../views/system/DataBackup.vue') },
      { path: 'system/logs',     name: 'SystemLogs',     component: () => import('../views/system/SysLog.vue') },
      { path: 'system/password', name: 'SystemPassword', component: () => import('../views/system/ChangePassword.vue') },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/inbound/purchase')
  } else if (to.path !== '/login' && !canAccess(to.path)) {
    ElMessage.warning('您没有访问该页面的权限')
    next('/inbound/purchase')
  } else {
    next()
  }
})

export default router
