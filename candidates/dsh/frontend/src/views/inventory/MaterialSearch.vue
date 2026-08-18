<template>
  <div class="pg"><div class="pg-hd"><h3>物料检索</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="物料编码"><el-input v-model="s.code" clearable /></el-form-item>
        <el-form-item label="物料名称"><el-input v-model="s.name" clearable /></el-form-item>
        <el-form-item label="封装"><el-input v-model="s.packageType" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">检索</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
      <el-table :data="rows" border stripe v-loading="loading" style="margin-top:12px">
        <el-table-column label="物料编码" prop="materialCode" width="140" />
        <el-table-column label="物料名称" prop="materialName" min-width="140" />
        <el-table-column label="封装" prop="packageType" width="90" />
        <el-table-column label="Value值" prop="valueData" width="100" />
        <el-table-column label="规格型号" prop="specModel" width="140" />
        <el-table-column label="厂家批次" width="120"><template #default="r"><span>-</span></template></el-table-column>
        <el-table-column label="库存数量" prop="stock" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="r"><el-tag v-if="r.row.lockStock>0" type="warning" size="small">占用{{r.row.lockStock}}</el-tag><el-tag v-else-if="r.row.stock>0" type="success" size="small">空闲</el-tag><el-tag v-else type="danger" size="small">缺货</el-tag></template>
        </el-table-column>
        <el-table-column label="存放货位" prop="locationNo" width="120" />
        <el-table-column label="备注" min-width="120"><template #default="r"><span>{{ r.row.remark || '-' }}</span></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="load" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted} from 'vue'
import {materialApi} from '@/api/index'
const rows=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0)
const s=reactive({code:'',name:'',packageType:''})
const load=async()=>{loading.value=true;try{const r=await materialApi.page({pageNum:page.value,pageSize:size.value,materialCode:s.code,materialName:s.name,packageType:s.packageType});if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}finally{loading.value=false}}
const onSearch=()=>{page.value=1;load()}
const reset=()=>{s.code='';s.name='';s.packageType='';page.value=1;load()}
onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
