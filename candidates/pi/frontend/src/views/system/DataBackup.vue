<template>
  <div class="pg"><div class="pg-hd"><h3>数据备份</h3></div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card header="手动备份">
          <p style="color:#999;margin-bottom:12px">点击下方按钮立即进行全量数据库备份</p>
          <el-button type="primary" :loading="backing" @click="doBackup">立即备份</el-button>
          <div v-if="lastBackup" style="margin-top:8px;color:#999;font-size:12px">上次备份: {{ lastBackup }}</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="自动备份策略">
          <el-form label-width="120px" v-if="configs.length">
            <template v-for="cfg in configs" :key="cfg.backupType">
              <el-form-item :label="cfg.backupType==='full' ? '全量备份' : '增量备份'">
                <el-switch v-model="cfg.enabled" :active-value="1" :inactive-value="0" />
                <span style="margin-left:12px;color:#999;font-size:12px">{{ cfg.cronExpr }}</span>
              </el-form-item>
              <el-form-item :label="cfg.backupType==='full' ? '全量保留天数' : '增量保留天数'">
                <el-input-number v-model="cfg.retentionDays" :min="1" :max="365" />
              </el-form-item>
            </template>
            <el-form-item label="备份目录"><el-input model-value="~/bms_backup" disabled /></el-form-item>
            <el-form-item><el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button></el-form-item>
          </el-form>
          <p v-else style="color:#999">正在加载配置...</p>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top:16px">
      <template #header><span style="font-weight:600">备份历史</span></template>
      <el-table :data="records" border stripe v-loading="loading">
        <el-table-column label="类型" width="90"><template #default="r"><el-tag :type="r.row.backupType==='full'?'':'warning'" size="small">{{ r.row.backupType==='full'?'全量':'增量' }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="r"><el-tag :type="r.row.status==='SUCCESS'?'success':'danger'" size="small">{{ r.row.status==='SUCCESS'?'成功':'失败' }}</el-tag></template></el-table-column>
        <el-table-column label="文件" prop="filePath" min-width="240"><template #default="r"><span>{{ r.row.filePath || '-' }}</span></template></el-table-column>
        <el-table-column label="大小" width="100"><template #default="r"><span>{{ r.row.fileSize ? (r.row.fileSize/1024).toFixed(1)+' KB' : '-' }}</span></template></el-table-column>
        <el-table-column label="消息" prop="message" min-width="160" />
        <el-table-column label="时间" prop="createTime" width="160" />
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="loadRecords" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import {ref,onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {systemApi} from '@/api/index'

const backing=ref(false),lastBackup=ref('')
const configs=ref([]),saving=ref(false)
const records=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0)

const doBackup=async()=>{
  backing.value=true
  try{
    const r=await systemApi.backup()
    if(r.code===200){lastBackup.value=new Date().toLocaleString();ElMessage.success(r.msg||'备份成功');loadRecords()}
    else ElMessage.error(r.msg||'备份失败')
  }catch{ElMessage.error('备份异常')}
  finally{backing.value=false}
}

const loadConfig=async()=>{
  try{const r=await systemApi.backupConfig();if(r.code===200)configs.value=r.data||[]}catch{}
}
const saveConfig=async()=>{
  saving.value=true
  try{
    const r=await systemApi.saveBackupConfig(configs.value)
    if(r.code===200)ElMessage.success('配置已保存')
    else ElMessage.error(r.msg||'保存失败')
  }catch{ElMessage.error('保存失败')}
  finally{saving.value=false}
}
const loadRecords=async()=>{
  loading.value=true
  try{const r=await systemApi.backupRecordPage({pageNum:page.value,pageSize:size.value});if(r.code===200&&r.data){records.value=r.data.records||[];total.value=r.data.total||0}}finally{loading.value=false}
}

onMounted(()=>{loadConfig();loadRecords()})
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
