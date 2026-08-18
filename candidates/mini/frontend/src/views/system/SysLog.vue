<template>
  <div class="pg"><div class="pg-hd"><h3>系统日志</h3></div>
    <el-card>
      <el-tabs v-model="logTab">
        <el-tab-pane label="操作日志" name="sys" />
        <el-tab-pane label="登录日志" name="login" />
      </el-tabs>
      <el-form inline style="margin-top:12px">
        <el-form-item label="用户名"><el-input v-model="s.username" clearable /></el-form-item>
        <el-form-item v-if="logTab==='sys'" label="操作类型"><el-input v-model="s.operation" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="load">查询</el-button><el-button @click="handleExport">导出</el-button></el-form-item>
      </el-form>

      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column label="用户名" prop="username" width="120" />
        <el-table-column label="操作" width="160"><template #default="r">{{ r.row.operation || r.row.loginResult===1?'登录成功':'登录失败' }}</template></el-table-column>
        <el-table-column label="描述" prop="description" min-width="180" />
        <el-table-column label="IP" prop="ip" width="140"><template #default="r">{{ r.row.ip||r.row.loginIp||'-' }}</template></el-table-column>
        <el-table-column label="时间" width="160"><template #default="r">{{ r.row.createTime||r.row.loginTime }}</template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="load" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted,watch} from 'vue'
import {ElMessage} from 'element-plus'
import {systemApi} from '@/api/index'
import {downloadFile} from '@/utils/request'
const rows=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0),logTab=ref('sys')
const s=reactive({username:'',operation:''})
const load=async()=>{loading.value=true;try{const params={pageNum:page.value,pageSize:size.value,username:s.username};if(logTab.value==='sys'){params.operation=s.operation;const r=await systemApi.sysLog(params);if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}else{const r=await systemApi.loginLog(params);if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}}finally{loading.value=false}}
const handleExport=async()=>{try{if(logTab.value==='sys'){const params={};if(s.username)params.username=s.username;if(s.operation)params.operation=s.operation;await downloadFile('/sysLog/export',params,'系统日志_'+new Date().toISOString().slice(0,10)+'.xlsx');ElMessage.success('导出成功')}else{ElMessage.info('登录日志导出请使用用户管理导出')}}catch(e){ElMessage.error('导出失败: '+e.message)}}
watch(logTab,()=>{page.value=1;load()})
onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
