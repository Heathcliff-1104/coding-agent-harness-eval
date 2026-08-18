<template>
  <div class="pg"><div class="pg-hd"><h3>修改密码</h3></div>
    <el-card style="max-width:480px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword"><el-input v-model="form.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码" prop="newPassword"><el-input v-model="form.newPassword" type="password" show-password /></el-form-item>
        <el-form-item label="确认密码" prop="confirmPwd"><el-input v-model="form.confirmPwd" type="password" show-password /></el-form-item>
        <el-form-item><el-button type="primary" @click="handleChange">修改密码</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import {ref,reactive} from 'vue'
import {ElMessage} from 'element-plus'
import {userApi} from '@/api/index'
const formRef=ref(null)
const form=reactive({oldPassword:'',newPassword:'',confirmPwd:''})
const rules={
  oldPassword:[{required:true,message:'请输入原密码',trigger:'blur'}],
  newPassword:[{required:true,min:8,message:'至少8位',trigger:'blur'}],
  confirmPwd:[{required:true,message:'请确认密码'},{validator:(_,v,cb)=>v!==form.newPassword?cb(new Error('两次密码不一致')):cb(),trigger:'blur'}]
}
const handleChange=async()=>{
  const valid=await formRef.value.validate().catch(()=>false)
  if(!valid)return
  try{
    const r=await userApi.changePwd({oldPassword:form.oldPassword,newPassword:form.newPassword})
    if(r.code===200){ElMessage.success('密码修改成功，请重新登录');localStorage.clear();setTimeout(()=>window.location.href='/login',1000)}
    else ElMessage.error(r.msg||'修改失败')
  }catch{ElMessage.error('请求失败')}
}
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
