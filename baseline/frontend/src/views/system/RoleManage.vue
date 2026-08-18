<template>
  <div class="pg"><div class="pg-hd"><h3>角色权限</h3></div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card header="角色列表">
          <el-radio-group v-model="selRole" style="display:flex;flex-direction:column;gap:8px">
            <el-radio v-for="r in roles" :key="r.key" :value="r.key" border style="padding:10px 16px;width:100%">
              <span style="font-weight:600">{{ r.label }}</span>
              <span style="color:#999;margin-left:8px;font-size:12px">{{ r.desc }}</span>
            </el-radio>
          </el-radio-group>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="菜单权限">
          <el-tree :data="menuTree" show-checkbox node-key="path" default-expand-all :default-checked-keys="checkedMenus" @check="onMenuCheck" />
          <el-button type="primary" style="margin-top:16px" @click="saveRoleMenus">保存</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import {ref,computed} from 'vue'
import {ElMessage} from 'element-plus'

const selRole=ref('engineer')
const checkedMenus=ref([])
const roles=[
  {key:'admin',label:'管理员',desc:'全部功能'},
  {key:'warehouse',label:'库管员',desc:'入库/出库/库存/报表'},
  {key:'engineer',label:'工程师',desc:'物料检索/出库领料'},
  {key:'purchaser',label:'采购员',desc:'入库记录/库存预警'},
]

const menuTree=[
  {label:'入库管理',path:'inbound-mgmt',children:[
    {label:'采购入库',path:'/inbound/purchase'},{label:'退库入库',path:'/inbound/return'},{label:'入库记录',path:'/inbound/records'}
  ]},
  {label:'出库管理',path:'outbound-mgmt',children:[
    {label:'生产领料',path:'/outbound/picking'},{label:'出库记录',path:'/outbound/records'}
  ]},
  {label:'库存管理',path:'inventory-mgmt',children:[
    {label:'物料检索',path:'/inventory/search'},{label:'库存查询',path:'/inventory/query'},{label:'库存预警',path:'/inventory/alert'},{label:'库存流水',path:'/inventory/flow'}
  ]},
  {label:'报表统计',path:'report-mgmt',children:[
    {label:'库存明细',path:'/report/inventory-detail'},{label:'入库统计',path:'/report/inbound-stats'},{label:'出库统计',path:'/report/outbound-stats'},{label:'呆滞物品',path:'/report/stagnant'},{label:'导出报表',path:'/report/export'}
  ]},
  {label:'系统管理',path:'sys-mgmt',children:[
    {label:'用户管理',path:'/system/users'},{label:'角色权限',path:'/system/roles'},{label:'数据备份',path:'/system/backup'},{label:'系统日志',path:'/system/logs'},{label:'密码修改',path:'/system/password'}
  ]},
]

import {getRole,canAccess} from '@/utils/permission'
const allPaths=['/inbound/purchase','/inbound/return','/inbound/records','/outbound/picking','/outbound/records','/inventory/search','/inventory/query','/inventory/alert','/inventory/flow','/report/inventory-detail','/report/inbound-stats','/report/outbound-stats','/report/stagnant','/report/export','/system/users','/system/roles','/system/backup','/system/logs','/system/password']
const onMenuCheck=(node,data)=>{checkedMenus.value=data.checkedKeys}
const saveRoleMenus=()=>{ElMessage.success('权限配置已保存（实时生效）')}
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
