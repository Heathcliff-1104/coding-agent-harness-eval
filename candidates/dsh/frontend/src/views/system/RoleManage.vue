<template>
  <div class="pg"><div class="pg-hd"><h3>角色权限</h3></div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card header="角色列表">
          <el-radio-group v-model="selRoleCode" style="display:flex;flex-direction:column;gap:8px" @change="loadRolePerms">
            <el-radio v-for="r in roles" :key="r.code" :value="r.code" border style="padding:10px 16px;width:100%">
              <span style="font-weight:600">{{ r.name }}</span>
              <span style="color:#999;margin-left:8px;font-size:12px">数据范围:{{ scopeText(r.dataScope) }}</span>
            </el-radio>
          </el-radio-group>
          <el-divider />
          <el-form inline>
            <el-form-item label="角色编码"><el-input v-model="newRole.code" style="width:130px" placeholder="如 quality" /></el-form-item>
            <el-form-item label="名称"><el-input v-model="newRole.name" style="width:130px" placeholder="如 质检员" /></el-form-item>
            <el-form-item label="数据范围"><el-select v-model="newRole.dataScope" style="width:100px"><el-option label="仅本人" value="self" /><el-option label="本部门" value="dept" /><el-option label="全部" value="all" /></el-select></el-form-item>
            <el-form-item><el-button type="success" @click="createRole">新增角色</el-button></el-form-item>
          </el-form>
          <div v-if="!['admin','warehouse','engineer'].includes(selRoleCode)">
            <el-button type="danger" size="small" @click="deleteRole">删除当前角色</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="菜单/按钮权限（勾选后保存即实时生效）">
          <el-tree ref="treeRef" :data="menuTree" show-checkbox node-key="code" default-expand-all :props="{label:'name',children:'children'}" />
          <div style="margin-top:16px;display:flex;gap:8px">
            <el-button type="primary" @click="saveRoleMenus">保存权限</el-button>
            <el-button @click="refresh">刷新</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import {ref,onMounted} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import {roleApi} from '@/api/index'

const roles=ref([])
const selRoleCode=ref('engineer')
const checkedCodes=ref([])
const treeRef=ref(null)
const newRole=ref({code:'',name:'',dataScope:'all'})

const scopeText=(s)=>({self:'仅本人',dept:'本部门',all:'全部'})[s||'all']||s

// 菜单树 = 权限清单（menu + button）
const menuTree=ref([])

const loadRoles=async()=>{
  try{const r=await roleApi.list();if(r.code===200){roles.value=r.data||[];if(!roles.value.find(x=>x.code===selRoleCode.value)&&roles.value.length){selRoleCode.value=roles.value[0].code}loadRolePerms()}}catch{}
}

const loadPermTree=async()=>{
  try{
    const r=await roleApi.permissionList()
    if(r.code===200&&r.data){
      const perms=r.data||[]
      const tree=[]
      const groups=[
        {code:'group:inbound',name:'入库管理',prefix:'menu:inbound'},
        {code:'group:outbound',name:'出库管理',prefix:'menu:outbound'},
        {code:'group:inventory',name:'库存管理',prefix:'menu:inventory'},
        {code:'group:report',name:'报表统计',prefix:'menu:report'},
        {code:'group:system',name:'系统管理',prefix:'menu:system'},
      ]
      for(const g of groups){
        const children=perms.filter(p=>p.code.startsWith(g.prefix)).map(p=>({code:p.code,name:p.name,type:p.type}))
        if(children.length)tree.push({code:g.code,name:g.name,children})
      }
      const buttons=perms.filter(p=>p.type==='button').map(p=>({code:p.code,name:p.name,type:p.type}))
      if(buttons.length)tree.push({code:'group:buttons',name:'按钮权限',children:buttons})
      menuTree.value=tree
    }
  }catch{}
}

const loadRolePerms=async()=>{
  try{
    const r=await roleApi.rolePermissions(selRoleCode.value)
    if(r.code===200){
      checkedCodes.value=r.data||[]
      if(treeRef.value)treeRef.value.setCheckedKeys(checkedCodes.value)
    }
  }catch{}
}

const refresh=()=>{loadRolePerms()}

const saveRoleMenus=async()=>{
  if(!treeRef.value)return
  const codes=treeRef.value.getCheckedKeys().filter(c=>!String(c).startsWith('group:'))
  try{
    const r=await roleApi.updatePermissions({roleCode:selRoleCode.value,permissionCodes:codes})
    if(r.code===200){ElMessage.success(r.msg||'权限配置已保存（实时生效）')}
  }catch{ElMessage.error('保存失败')}
}

const createRole=async()=>{
  if(!newRole.value.code||!newRole.value.name){ElMessage.warning('请填写角色编码与名称');return}
  try{
    const r=await roleApi.save({...newRole.value})
    if(r.code===200){ElMessage.success('角色已创建');newRole.value={code:'',name:'',dataScope:'all'};loadRoles()}
  }catch{ElMessage.error('创建失败')}
}

const deleteRole=async()=>{
  try{
    await ElMessageBox.confirm('确定删除该角色？删除后用户将无法使用此角色','提示',{type:'warning'})
    const role=roles.value.find(x=>x.code===selRoleCode.value)
    const r=await roleApi.delete(role.id)
    if(r.code===200){ElMessage.success('已删除');loadRoles()}
  }catch{}
}

onMounted(()=>{loadRoles();loadPermTree()})
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
