<template>
  <div class="pg"><div class="pg-hd"><h3>库存流水</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="类型"><el-select v-model="s.type" clearable style="width:110px"><el-option label="入库" value="in" /><el-option label="出库" value="out" /></el-select></el-form-item>
        <el-form-item label="物料编码"><el-input v-model="s.materialCode" clearable placeholder="物料编码" /></el-form-item>
        <el-form-item label="关键词"><el-input v-model="s.keyword" clearable placeholder="单号/批次" /></el-form-item>
        <el-form-item label="时间"><el-date-picker v-model="s.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" /></el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="reset">重置</el-button><el-button @click="handleExport">导出Excel</el-button></el-form-item>
      </el-form>
      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column label="类型" width="80"><template #default="r"><el-tag :type="r.row.recordType==='in'?'success':'danger'" size="small">{{ r.row.recordType==='in'?'入库':'出库' }}</el-tag></template></el-table-column>
        <el-table-column label="单据号" prop="billNo" width="170" />
        <el-table-column label="物料ID" prop="materialId" width="90" />
        <el-table-column label="批次号" prop="batchNo" width="120" />
        <el-table-column label="数量" prop="num" width="90" />
        <el-table-column label="操作人" prop="operator" width="100" />
        <el-table-column label="时间" prop="opTime" width="160" />
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="load" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted} from 'vue'
import {inventoryApi} from '@/api/index'
import {downloadBlob} from '@/utils/request'
const rows=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0)
const s=reactive({keyword:'',materialCode:'',type:null,dateRange:null})
const buildParams=()=>{
  const p={pageNum:page.value,pageSize:size.value,keyword:s.keyword,materialCode:s.materialCode,type:s.type||undefined}
  if(s.dateRange){p.startTime=s.dateRange[0];p.endTime=s.dateRange[1]}
  return p
}
const load=async()=>{loading.value=true;try{const r=await inventoryApi.stockFlow(buildParams());if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}finally{loading.value=false}}
const onSearch=()=>{page.value=1;load()}
const reset=()=>{s.keyword='';s.materialCode='';s.type=null;s.dateRange=null;page.value=1;load()}
const handleExport=()=>{const p={keyword:s.keyword,materialCode:s.materialCode,type:s.type||undefined};if(s.dateRange){p.startTime=s.dateRange[0];p.endTime=s.dateRange[1]}downloadBlob('/stockFlow/export',p,`库存流水_${new Date().toISOString().slice(0,10)}.xlsx`)}
onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
