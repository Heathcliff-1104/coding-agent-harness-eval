<template>
  <div class="pg"><div class="pg-hd"><h3>库存明细</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="物料编码"><el-input v-model="s.code" clearable /></el-form-item>
        <el-form-item label="物料名称"><el-input v-model="s.name" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="reset">重置</el-button><el-button @click="handleExport">导出Excel</el-button></el-form-item>
      </el-form>
      <el-table :data="rows" border stripe v-loading="loading" style="margin-top:12px">
        <el-table-column label="物料编码" prop="materialCode" width="140" />
        <el-table-column label="物料名称" prop="materialName" min-width="140" />
        <el-table-column label="封装" prop="packageType" width="90" />
        <el-table-column label="规格型号" prop="specModel" width="140" />
        <el-table-column label="厂家批次" width="120"><template #default="r"><span>-</span></template></el-table-column>
        <el-table-column label="库存数量" prop="stock" width="90" />
        <el-table-column label="存放货位" prop="locationNo" width="120" />
        <el-table-column label="物料状态" width="90"><template #default="r"><el-tag v-if="r.row.lockStock>0" type="warning" size="small">占用</el-tag><el-tag v-else-if="r.row.stock>0" type="success" size="small">空闲</el-tag><el-tag v-else type="danger" size="small">缺货</el-tag></template></el-table-column>
        <el-table-column label="备注" prop="remark" min-width="100" />
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="load" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {materialApi} from '@/api/index'
import {downloadFile} from '@/utils/request'
const rows=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0)
const s=reactive({code:'',name:''})
const load=async()=>{loading.value=true;try{const r=await materialApi.page({pageNum:page.value,pageSize:size.value,materialCode:s.code,materialName:s.name});if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}finally{loading.value=false}}
const onSearch=()=>{page.value=1;load()}
const reset=()=>{s.code='';s.name='';page.value=1;load()}
const handleExport=async()=>{try{const params={};if(s.code)params.materialCode=s.code;if(s.name)params.materialName=s.name;await downloadFile('/material/export',params,'库存明细_'+new Date().toISOString().slice(0,10)+'.xlsx');}catch(e){ElMessage.error('导出失败: '+e.message)}}
onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
