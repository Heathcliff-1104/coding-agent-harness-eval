<template>
  <div class="pg"><div class="pg-hd"><h3>角色权限</h3></div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card header="角色列表">
          <el-radio-group v-model="selRole" style="display:flex;flex-direction:column;gap:8px" @change="loadMenus">
            <el-radio v-for="r in roles" :key="r.roleCode" :value="r.roleCode" border style="padding:10px 16px;width:100%">
              <span style="font-weight:600">{{ r.roleName }}</span>
              <span style="color:#999;margin-left:8px;font-size:12px">{{ r.description }}</span>
            </el-radio>
          </el-radio-group>
          <el-divider />
          <el-form inline style="margin-top:8px">
            <el-form-item label="新增角色">
              <el-input v-model="newRole.roleCode" placeholder="角色编码" style="width:110px" />
              <el-input v-model="newRole.roleName" placeholder="角色名称" style="width:120px;margin-left:4px" />
              <el-button type="primary" style="margin-left:4px" @click="addRole">添加</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="菜单权限">
          <el-tree ref="treeRef" :data="menuTree" show-checkbox node-key="path" default-expand-all :default-checked-keys="checkedMenus" @check="onMenuCheck" />
          <el-button type="primary" style="margin-top:16px" @click="saveRoleMenus">保存</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import {ref,onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {roleApi} from '@/api/index'

const selRole=ref('engineer')
const checkedMenus=ref([])
const roles=ref([])
const treeRef=ref(null)
const newRole=ref({roleCode:'',roleName:''})

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

const loadRoles=async()=>{try{const r=await roleApi.list();if(r.code===200&&r.data){roles.value=r.data||[];if(roles.value.length&&!roles.value.some(x=>x.roleCode===selRole.value)){selRole.value=roles.value[0].roleCode}await loadMenus()}}catch{}}
const loadMenus=async()=>{try{const r=await roleApi.menus(selRole.value);if(r.code===200&&r.data){checkedMenus.value=r.data||[];treeRef.value?.setCheckedKeys(checkedMenus.value)}}catch{}}
const onMenuCheck=()=>{}
const saveRoleMenus=async()=>{try{const checked=treeRef.value?.['getCheckedKeys']?.()||[];const half=treeRef.value?.['getHalfCheckedKeys']?.()||[];const menus=[...checked,...half].filter(p=>p.startsWith('/'));await roleApi.save({roleCode:selRole.value,roleName:roles.value.find(x=>x.roleCode===selRole.value)?.roleName||selRole.value,menus});ElMessage.success('权限配置已保存（实时生效）')}catch(e){ElMessage.error(e.message||'保存失败')}}
const addRole=async()=>{if(!newRole.value.roleCode||!newRole.value.roleName){ElMessage.warning('请填写角色编码和名称');return}try{await roleApi.save({roleCode:newRole.value.roleCode,roleName:newRole.value.roleName,menus:[]});ElMessage.success('角色已添加');newRole.value={roleCode:'',roleName:''};loadRoles()}catch(e){ElMessage.error(e.message||'添加失败')}}
onMounted(loadRoles)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
