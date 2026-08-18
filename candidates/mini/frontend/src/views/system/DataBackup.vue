<template>
  <div class="pg"><div class="pg-hd"><h3>数据备份</h3></div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card header="手动备份">
          <p style="color:#999;margin-bottom:12px">点击下方按钮立即进行全量数据库备份</p>
          <el-button type="primary" :loading="backing" @click="doBackup">立即备份</el-button>
          <div v-if="lastBackup" style="margin-top:8px;color:#999;font-size:12px">上次备份: {{ lastBackup }}</div>
          <el-divider />
          <h4>备份记录</h4>
          <el-table :data="records" border size="small" max-height="300">
            <el-table-column label="类型" prop="backupType" width="100" />
            <el-table-column label="文件" prop="filePath" min-width="200" />
            <el-table-column label="状态" prop="status" width="90" />
            <el-table-column label="时间" prop="createTime" width="150" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="自动备份策略">
          <el-form label-width="130px">
            <el-form-item label="全量备份"><el-tag type="success">{{ strategy.fullCron || '每周日凌晨2:00' }}</el-tag></el-form-item>
            <el-form-item label="增量备份"><el-tag type="warning" :type="strategy.incrementalEnabled==='true'?'success':'info'">{{ strategy.incrementalEnabled==='true'?'已启用':'已禁用' }}</el-tag></el-form-item>
            <el-form-item label="保留周期"><el-input-number v-model="retainDays" :min="1" :max="365" /> 天</el-form-item>
            <el-form-item label="备份目录"><el-input v-model="backupDir" disabled value="~/bms_backup" /></el-form-item>
            <el-form-item><el-button type="primary" @click="saveConfig">保存配置</el-button></el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import {ref,onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {systemApi} from '@/api/index'

const backing=ref(false)
const lastBackup=ref('')
const retainDays=ref(30)
const backupDir=ref('~/bms_backup')
const strategy=ref({})
const records=ref([])

const loadStrategy=async()=>{try{const r=await systemApi.backupStrategy();if(r.code===200&&r.data){strategy.value=r.data;retainDays.value=parseInt(r.data.retentionDays||'30')}}catch{}}
const loadRecords=async()=>{try{const r=await systemApi.backupRecords({pageNum:1,pageSize:10});if(r.code===200&&r.data){records.value=r.data.records||[]}}catch{}}

const doBackup=async()=>{
  backing.value=true
  try{
    const r=await systemApi.backup()
    if(r.code===200){lastBackup.value=new Date().toLocaleString();ElMessage.success(r.msg||'备份成功');loadRecords()}
    else ElMessage.error(r.msg||'备份失败')
  }catch{ElMessage.error('备份异常')}
  finally{backing.value=false}
}

const saveConfig=async()=>{try{const r=await systemApi.saveBackupStrategy({retentionDays:retainDays.value});if(r.code===200){ElMessage.success(r.msg||'配置已保存');loadStrategy()}else{ElMessage.error(r.msg||'保存失败')}}catch(e){ElMessage.error(e.message||'保存失败')}}

onMounted(()=>{loadStrategy();loadRecords()})
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
