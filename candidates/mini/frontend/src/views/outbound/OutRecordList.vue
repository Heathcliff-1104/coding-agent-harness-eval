<template>
  <div class="pg"><div class="pg-hd"><h3>出库记录</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="出库单号"><el-input v-model="s.outboundCode" clearable /></el-form-item>
        <el-form-item label="物料"><el-input v-model="s.keyword" clearable placeholder="编码/名称" /></el-form-item>
        <el-form-item label="时间"><el-date-picker v-model="s.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" /></el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="reset">重置</el-button><el-button @click="handleExport">导出Excel</el-button><el-button @click="print">打印</el-button></el-form-item>
      </el-form>
      <el-table :data="rows" border stripe v-loading="loading" style="margin-top:12px">
        <el-table-column label="出库单号" prop="outboundCode" width="180" />
        <el-table-column label="物料编码" width="130"><template #default="r"><span>{{ r.row.materialCode || r.row.materialId || '-' }}</span></template></el-table-column>
        <el-table-column label="物料名称" width="120"><template #default="r"><span>{{ r.row.materialName || '-' }}</span></template></el-table-column>
        <el-table-column label="批次号" prop="batchNo" width="120" />
        <el-table-column label="出库数量" prop="outNum" width="90" />
        <el-table-column label="操作人" prop="outUser" width="100" />
        <el-table-column label="出库时间" prop="outTime" width="160" />
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="load" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {systemApi} from '@/api/index'
import { downloadFile } from '@/utils/request'
const rows=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0)
const s=reactive({outboundCode:'',keyword:'',dateRange:null})
const load=async()=>{loading.value=true;try{const p={pageNum:page.value,pageSize:size.value,outboundCode:s.outboundCode};if(s.dateRange){p.startTime=s.dateRange[0];p.endTime=s.dateRange[1]}const r=await systemApi.outRecord(p);if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}finally{loading.value=false}}
const onSearch=()=>{page.value=1;load()}
const reset=()=>{s.outboundCode='';s.keyword='';s.dateRange=null;page.value=1;load()}
const handleExport=async()=>{try{const params={};if(s.outboundCode)params.outboundCode=s.outboundCode;if(s.keyword)params.keyword=s.keyword;if(s.dateRange){params.startTime=s.dateRange[0];params.endTime=s.dateRange[1]}await downloadFile('/outRecord/export',params,'出库记录_'+new Date().toISOString().slice(0,10)+'.xlsx');ElMessage.success('导出成功')}catch(e){ElMessage.error('导出失败: '+e.message)}}
const print=()=>window.print()
onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
