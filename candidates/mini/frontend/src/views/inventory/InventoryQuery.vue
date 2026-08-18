<template>
  <div class="pg"><div class="pg-hd"><h3>库存查询</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="物料编码"><el-input v-model="s.code" clearable /></el-form-item>
        <el-form-item label="物料名称"><el-input v-model="s.name" clearable /></el-form-item>
        <el-form-item label="仓库"><el-input v-model="s.warehouse" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>

      <el-row :gutter="16" style="margin:16px 0">
        <el-col :span="6"><el-statistic title="物料种类" :value="total" /></el-col>
        <el-col :span="6"><el-statistic title="总库存量" :value="totalStock" /></el-col>
        <el-col :span="6"><el-statistic title="低库存预警" :value="lowStockCount"><template #suffix><span style="color:#e6a23c">项</span></template></el-statistic></el-col>
        <el-col :span="6"><el-statistic title="缺货物料" :value="outStockCount"><template #suffix><span style="color:#f56c6c">项</span></template></el-statistic></el-col>
      </el-row>

      <el-table :data="rows" border stripe v-loading="loading" style="margin-top:12px">
        <el-table-column label="物料编码" prop="materialCode" width="150" />
        <el-table-column label="物料名称" prop="materialName" min-width="150" />
        <el-table-column label="封装" prop="packageType" width="90" />
        <el-table-column label="规格型号" prop="specModel" width="140" />
        <el-table-column label="库存" prop="stock" width="80" />
        <el-table-column label="占用" width="80"><template #default="r"><span v-if="r.row.lockStock>0" style="color:#e6a23c">{{ r.row.lockStock }}</span><span v-else>-</span></template></el-table-column>
        <el-table-column label="可用" width="80"><template #default="r">{{ (r.row.stock||0) - (r.row.lockStock||0) }}</template></el-table-column>
        <el-table-column label="最低库存" prop="minStock" width="90" />
        <el-table-column label="最高库存" prop="maxStock" width="90" />
        <el-table-column label="状态" width="80">
          <template #default="r"><el-tag v-if="r.row.stock===0" type="danger" size="small">缺货</el-tag><el-tag v-else-if="r.row.stock<=(r.row.minStock||0)" type="warning" size="small">低库存</el-tag><el-tag v-else type="success" size="small">正常</el-tag></template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="load" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import {ref,reactive,computed,onMounted} from 'vue'
import {materialApi} from '@/api/index'
const rows=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0)
const s=reactive({code:'',name:'',warehouse:''})
const totalStock=computed(()=>rows.value.reduce((sum,r)=>sum+(r.stock||0),0))
const lowStockCount=computed(()=>rows.value.filter(r=>r.stock>0&&r.stock<=(r.minStock||0)).length)
const outStockCount=computed(()=>rows.value.filter(r=>r.stock===0).length)
const load=async()=>{loading.value=true;try{const r=await materialApi.page({pageNum:page.value,pageSize:size.value,materialCode:s.code,materialName:s.name,warehouseCode:s.warehouse});if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}finally{loading.value=false}}
const onSearch=()=>{page.value=1;load()}
const reset=()=>{s.code='';s.name='';s.warehouse='';page.value=1;load()}
onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
