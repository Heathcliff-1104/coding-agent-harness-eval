<template>
  <div class="sb-wrap">
    <div class="sb-hd">
      <h3>通用物料管理系统</h3>
      <span class="sb-role">{{ roleName }}</span>
    </div>
    <el-scrollbar class="sb-body">
      <el-menu :default-active="activeMenu" router background-color="#001529" text-color="#ffffffa6" active-text-color="#fff" :unique-opened="true">
        <!-- 入库管理 -->
        <el-sub-menu v-if="hasMenu('/inbound/purchase') || hasMenu('/inbound/return') || hasMenu('/inbound/records')" index="inbound-mgmt">
          <template #title><el-icon><Plus /></el-icon><span>入库管理</span></template>
          <el-sub-menu v-if="hasMenu('/inbound/purchase') || hasMenu('/inbound/return')" index="inbound-func">
            <template #title><span>入库功能</span></template>
            <el-menu-item v-if="hasMenu('/inbound/purchase')" index="/inbound/purchase"><span>采购入库</span></el-menu-item>
            <el-menu-item v-if="hasMenu('/inbound/return')" index="/inbound/return"><span>退库入库</span></el-menu-item>
          </el-sub-menu>
          <el-menu-item v-if="hasMenu('/inbound/records')" index="/inbound/records"><span>入库记录</span></el-menu-item>
        </el-sub-menu>

        <!-- 出库管理 -->
        <el-sub-menu v-if="hasMenu('/outbound/picking') || hasMenu('/outbound/records')" index="outbound-mgmt">
          <template #title><el-icon><Minus /></el-icon><span>出库管理</span></template>
          <el-menu-item v-if="hasMenu('/outbound/picking')" index="/outbound/picking"><span>生产领料</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/outbound/records')" index="/outbound/records"><span>出库记录</span></el-menu-item>
        </el-sub-menu>

        <!-- 库存管理 -->
        <el-sub-menu v-if="hasMenu('/inventory/search') || hasMenu('/inventory/query') || hasMenu('/inventory/alert') || hasMenu('/inventory/flow')" index="inventory-mgmt">
          <template #title><el-icon><Box /></el-icon><span>库存管理</span></template>
          <el-menu-item v-if="hasMenu('/inventory/search')" index="/inventory/search"><span>物料检索</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/inventory/query')" index="/inventory/query"><span>库存查询</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/inventory/alert')" index="/inventory/alert"><span>库存预警</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/inventory/flow')" index="/inventory/flow"><span>库存流水</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/inventory/cis')" index="/inventory/cis"><span>同步CIS元件库</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/inventory/materials')" index="/inventory/materials"><span>物料管理</span></el-menu-item>
        </el-sub-menu>

        <!-- 报表统计 -->
        <el-sub-menu v-if="hasMenu('/report/inventory-detail')" index="report-mgmt">
          <template #title><el-icon><DataAnalysis /></el-icon><span>报表统计</span></template>
          <el-menu-item v-if="hasMenu('/report/inventory-detail')" index="/report/inventory-detail"><span>库存明细</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/report/inbound-stats')" index="/report/inbound-stats"><span>入库统计</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/report/outbound-stats')" index="/report/outbound-stats"><span>出库统计</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/report/stagnant')" index="/report/stagnant"><span>呆滞物品</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/report/export')" index="/report/export"><span>导出报表</span></el-menu-item>
        </el-sub-menu>

        <!-- 系统管理 -->
        <el-sub-menu v-if="hasMenu('/system/users') || hasMenu('/system/roles') || hasMenu('/system/backup') || hasMenu('/system/logs') || hasMenu('/system/password')" index="sys-mgmt">
          <template #title><el-icon><Setting /></el-icon><span>系统管理</span></template>
          <el-menu-item v-if="hasMenu('/system/users')" index="/system/users"><span>用户管理</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/system/roles')" index="/system/roles"><span>角色权限</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/system/backup')" index="/system/backup"><span>数据备份</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/system/logs')" index="/system/logs"><span>系统日志</span></el-menu-item>
          <el-menu-item v-if="hasMenu('/system/password')" index="/system/password"><span>密码修改</span></el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-scrollbar>
    <div class="sb-ft">
      <span class="user-tag">{{ realName }}</span>
      <el-button link type="danger" size="small" @click="handleLogout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Plus, Minus, Box, DataAnalysis, Setting } from '@element-plus/icons-vue'
import { canAccess } from '../utils/permission'
import { userApi } from '../api/index'

const router = useRouter()
const route = useRoute()
const realName = ref(localStorage.getItem('realName') || localStorage.getItem('username') || '未登录')

const roleMap = { admin: '管理员', warehouse: '库管员', engineer: '工程师', purchaser: '采购员', inspector: '质检员', manager: '部门主管' }
const roleName = computed(() => roleMap[localStorage.getItem('role') || 'engineer'])
const activeMenu = computed(() => route.path)

const hasMenu = (path) => canAccess(path)

const handleLogout = async () => {
  try { await userApi.logout() } catch { /* ignore */ }
  localStorage.clear()
  router.push('/login')
}
</script>

<style scoped>
.sb-wrap { display:flex; flex-direction:column; height:100%; background:#001529; }
.sb-hd { padding:20px 16px; text-align:center; border-bottom:1px solid rgba(255,255,255,0.08); }
.sb-hd h3 { margin:0 0 6px; font-size:15px; color:#fff; font-weight:600; }
.sb-role { font-size:11px; color:#4fc3f7; background:rgba(79,195,247,0.12); padding:2px 10px; border-radius:4px; }
.sb-body { flex:1; overflow-y:auto; }
.sb-body .el-menu { border-right:none; }
.sb-ft { padding:14px 16px; border-top:1px solid rgba(255,255,255,0.08); display:flex; flex-direction:column; gap:6px; align-items:center; }
.user-tag { color:#ffffff99; font-size:12px; }
</style>
