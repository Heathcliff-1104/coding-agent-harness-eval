<template>
  <div class="pg"><div class="pg-hd"><h3>用户管理</h3></div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="s.keyword" placeholder="搜索用户名/姓名" style="width:220px" clearable />
        <el-button type="primary" @click="load" style="margin-left:8px">查询</el-button>
        <el-button type="success" @click="openAdd">新增用户</el-button>
        <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls" :on-change="handleImport" style="display:inline-block;margin-left:8px">
          <el-button>批量导入</el-button>
        </el-upload>
        <el-button @click="handleExport">导出用户</el-button>
      </div>

      <el-table :data="rows" border stripe v-loading="loading" style="margin-top:12px">
        <el-table-column label="用户名" prop="username" width="120" />
        <el-table-column label="真实姓名" prop="realName" width="100" />
        <el-table-column label="部门" prop="dept" width="100" />
        <el-table-column label="手机号" prop="phone" width="130" />
        <el-table-column label="角色" prop="role" width="100" />
        <el-table-column label="状态" width="80"><template #default="r"><el-tag :type="r.row.status===1?'success':'danger'" size="small">{{ r.row.status===1?'启用':'禁用' }}</el-tag></template></el-table-column>
        <el-table-column label="上次登录时间" width="160"><template #default="r"><span>{{ r.row.lastLoginTime || '-' }}</span></template></el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="r"><el-button size="small" @click="openEdit(r.row)">编辑</el-button><el-button size="small" type="danger" @click="delUser(r.row)">删除</el-button><el-button size="small" @click="doResetPwd(r.row)">重置密码</el-button></template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="load" style="margin-top:12px;justify-content:flex-end" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId?'编辑用户':'新增用户'" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!editId" /></el-form-item>
        <el-form-item v-if="!editId" label="初始密码"><el-input v-model="form.password" type="password" show-password placeholder="8~20位，含大小写/数字/特殊至少3类" /></el-form-item>
        <el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="form.dept" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="form.role"><el-option label="工程师" value="engineer" /><el-option label="库管员" value="warehouse" /><el-option label="采购员" value="purchaser" /><el-option label="质检员" value="inspector" /><el-option label="部门主管" value="manager" /><el-option label="管理员" value="admin" /></el-select></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import {userApi} from '@/api/index'
import {downloadWithAuth} from '@/utils/download'
import * as XLSX from 'xlsx'
const rows=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0)
const s=reactive({keyword:''})
const dialogVisible=ref(false),editId=ref(null)
const form=reactive({username:'',password:'',realName:'',dept:'',phone:'',role:'engineer',status:1})
const load=async()=>{loading.value=true;try{const r=await userApi.page({pageNum:page.value,pageSize:size.value,keyword:s.keyword});if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}finally{loading.value=false}}
const openAdd=()=>{editId.value=null;form.username='';form.password='';form.realName='';form.dept='';form.phone='';form.role='engineer';form.status=1;dialogVisible.value=true}
const openEdit=(row)=>{editId.value=row.id;Object.assign(form,{username:row.username,password:'',realName:row.realName,dept:row.dept,phone:row.phone,role:row.role,status:row.status});dialogVisible.value=true}
const handleSave=async()=>{try{if(editId.value){await userApi.update({id:editId.value,username:form.username,realName:form.realName,dept:form.dept,phone:form.phone,role:form.role,status:form.status})}else{await userApi.add(form)}ElMessage.success('操作成功');dialogVisible.value=false;load()}catch(e){ElMessage.error((e&&e.message)||'操作失败')}}
const delUser=async(row)=>{try{await ElMessageBox.confirm('确定删除该用户？');await userApi.delete(row.id);ElMessage.success('已删除');load()}catch{}}
const doResetPwd=async(row)=>{try{await userApi.resetPwd(row.id);ElMessage.success('已重置为Abc@12345')}catch{ElMessage.error('操作失败')}}
const handleImport=(file)=>{
  const reader=new FileReader()
  reader.onload=async (e)=>{
    try{
      const wb=XLSX.read(e.target.result,{type:'binary'})
      const ws=wb.Sheets[wb.SheetNames[0]]
      const data=XLSX.utils.sheet_to_json(ws)
      const users=data.map(r=>({
        username:String(r['用户名']||'').trim(),
        password:String(r['密码']||'').trim(),
        realName:String(r['真实姓名']||'').trim(),
        phone:String(r['手机号']||'').trim(),
        dept:String(r['部门']||'').trim(),
        role:String(r['角色']||'engineer').trim(),
        status:r['状态']==='禁用'?'0':'1'
      })).filter(u=>u.username&&u.password)
      if(!users.length){ElMessage.warning('未解析到有效用户数据（需含 用户名/密码 列）');return}
      const res=await userApi.importUsers(users)
      if(res.code===200){ElMessage.success(res.msg||'导入完成');load()}
      else ElMessage.error(res.msg||'导入失败')
    }catch{ElMessage.error('解析失败，请检查Excel格式')}
  }
  reader.readAsBinaryString(file.raw)
}
const handleExport=async()=>{try{await downloadWithAuth('/user/export');ElMessage.success('导出完成')}catch{ElMessage.error('导出失败')}}
onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}.toolbar{display:flex;align-items:center;gap:8px}</style>
