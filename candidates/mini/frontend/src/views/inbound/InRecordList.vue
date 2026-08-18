<template>
  <div class="pg"><div class="pg-hd"><h3>入库记录</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="入库单号"><el-input v-model="s.billNo" clearable /></el-form-item>
        <el-form-item label="关键词"><el-input v-model="s.keyword" clearable placeholder="物料/批次" /></el-form-item>
        <el-form-item label="时间"><el-date-picker v-model="s.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" /></el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="reset">重置</el-button><el-button @click="handleExport">导出Excel</el-button><el-button @click="print">打印</el-button></el-form-item>
      </el-form>
      <el-table :data="rows" border stripe v-loading="loading" style="margin-top:12px">
        <el-table-column label="入库单号" prop="billNo" width="160" />
        <el-table-column label="入库时间" prop="inTime" width="160" />
        <el-table-column label="操作人" prop="inUser" width="100" />
        <el-table-column label="物料编码" width="120"><template #default="r"><span>{{ r.row.materialCode || r.row.materialId || '-' }}</span></template></el-table-column>
        <el-table-column label="物料名称" width="120"><template #default="r"><span>{{ r.row.materialName || '-' }}</span></template></el-table-column>
        <el-table-column label="批次号" prop="batchNo" width="120" />
        <el-table-column label="数量" prop="inNum" width="80" />
        <el-table-column label="存放货位" width="120"><template #default="r"><span>{{ r.row.locationNo || '-' }}</span></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="load" style="margin-top:16px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { systemApi } from '@/api/index'
import { downloadFile } from '@/utils/request'
const rows=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0)
const s=reactive({billNo:'',keyword:'',dateRange:null})
const load=async()=>{loading.value=true;try{const p={pageNum:page.value,pageSize:size.value,billNo:s.billNo,keyword:s.keyword};if(s.dateRange){p.startDate=s.dateRange[0];p.endDate=s.dateRange[1]}const r=await systemApi.inRecord(p);if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}finally{loading.value=false}}
const onSearch=()=>{page.value=1;load()}
const reset=()=>{s.billNo='';s.keyword='';s.dateRange=null;page.value=1;load()}
const handleExport=async()=>{try{const params={};if(s.billNo)params.billNo=s.billNo;if(s.keyword)params.keyword=s.keyword;if(s.dateRange){params.startDate=s.dateRange[0];params.endDate=s.dateRange[1]}await downloadFile('/inRecord/export',params,'入库记录_'+new Date().toISOString().slice(0,10)+'.xlsx');ElMessage.success('导出成功')}catch(e){ElMessage.error('导出失败: '+e.message)}}
const print=()=>window.print()
onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
