<template>
  <div class="pg"><div class="pg-hd"><h3>库存流水</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="关键词"><el-input v-model="s.keyword" clearable placeholder="单号/批次" /></el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="reset">重置</el-button><el-button @click="handleExport">导出Excel</el-button></el-form-item>
      </el-form>
      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column label="类型" width="80"><template #default="r"><el-tag :type="r.row.recordType==='in'?'success':'danger'" size="small">{{ r.row.recordType==='in'?'入库':'出库' }}</el-tag></template></el-table-column>
        <el-table-column label="单据号" prop="billNo" width="160" />
        <el-table-column label="物料ID" prop="materialId" width="80" />
        <el-table-column label="批次号" prop="batchNo" width="120" />
        <el-table-column label="数量" prop="num" width="80" />
        <el-table-column label="操作人" prop="operator" width="100" />
        <el-table-column label="时间" prop="opTime" width="160" />
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="load" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {inventoryApi} from '@/api/index'
const rows=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0)
const s=reactive({keyword:''})
const load=async()=>{loading.value=true;try{const r=await inventoryApi.stockFlow({pageNum:page.value,pageSize:size.value,keyword:s.keyword});if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}finally{loading.value=false}}
const onSearch=()=>{page.value=1;load()}
const reset=()=>{s.keyword='';page.value=1;load()}
const handleExport=()=>ElMessage.info('导出功能待后端接口')
onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
