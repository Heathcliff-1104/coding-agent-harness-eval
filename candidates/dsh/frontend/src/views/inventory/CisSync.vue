<template>
  <div class="pg"><div class="pg-hd"><h3>同步CIS元件库</h3></div>
    <el-card>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px"
        title="将物料编码、封装、值、库存数量、批次信息同步至“示例 CIS 元件库”（CIS系统）。未配置 cis.endpoint 时为演示模式（模拟同步），不调用外部系统。" />
      <div style="display:flex;gap:12px;margin-bottom:16px">
        <el-button type="primary" :loading="syncing" @click="doSync('full')">手动全量同步</el-button>
        <el-button type="warning" :loading="syncing" @click="doSync('incremental')">增量同步</el-button>
      </div>
      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column label="同步类型" width="110"><template #default="r"><el-tag size="small" :type="r.row.syncType==='full'?'primary':'warning'">{{ r.row.syncType==='full'?'全量':'增量' }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="r"><el-tag size="small" :type="r.row.syncStatus==='success'?'success':'danger'">{{ r.row.syncStatus==='success'?'成功':'失败' }}</el-tag></template></el-table-column>
        <el-table-column label="物料数" prop="materialCount" width="90" />
        <el-table-column label="结果" prop="message" min-width="240" />
        <el-table-column label="时间" prop="createTime" width="170" />
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="load" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import {ref,onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {cisApi} from '@/api/index'

const rows=ref([]),loading=ref(false),syncing=ref(false),page=ref(1),size=ref(10),total=ref(0)

const load=async()=>{loading.value=true;try{const r=await cisApi.syncLog({pageNum:page.value,pageSize:size.value});if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}finally{loading.value=false}}

const doSync=async(type)=>{
  syncing.value=true
  try{
    const r=type==='full'?await cisApi.syncFull():await cisApi.syncIncremental()
    if(r.code===200){ElMessage.success(r.data||r.msg||'同步完成');load()}
    else ElMessage.error(r.msg||'同步失败')
  }catch{ElMessage.error('同步请求失败')}
  finally{syncing.value=false}
}

onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
