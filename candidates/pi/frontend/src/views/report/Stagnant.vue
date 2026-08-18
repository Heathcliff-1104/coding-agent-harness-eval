<template>
  <div class="pg"><div class="pg-hd"><h3>呆滞物品</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="呆滞天数"><el-select v-model="s.days" style="width:140px"><el-option label="超过90天" :value="90" /><el-option label="超过180天" :value="180" /><el-option label="超过365天" :value="365" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="handleExport">导出Excel</el-button></el-form-item>
      </el-form>
      <el-row :gutter="16" style="margin:16px 0">
        <el-col :span="6"><el-statistic title="呆滞物料数" :value="total"><template #suffix><span style="color:#f56c6c">项</span></template></el-statistic></el-col>
        <el-col :span="6"><el-statistic title="呆滞库存总量" :value="stagnantTotal" /></el-col>
        <el-col :span="6"><el-statistic title="呆滞库存金额" :value="stagnantAmount" /></el-col>
      </el-row>
      <el-table :data="pageRows" border stripe v-loading="loading">
        <el-table-column label="物料编码" prop="materialCode" width="140" />
        <el-table-column label="物料名称" prop="materialName" min-width="140" />
        <el-table-column label="当前库存" prop="stock" width="90" />
        <el-table-column label="库存金额" width="110"><template #default="r">{{ Number(r.row.amount||0).toFixed(2) }}</template></el-table-column>
        <el-table-column label="最后出库时间" width="170"><template #default="r"><span>{{ r.row.lastOutTime ? r.row.lastOutTime : '从未出库' }}</span></template></el-table-column>
        <el-table-column label="存放货位" prop="locationNo" width="110"><template #default="r"><span>{{ r.row.locationNo || '-' }}</span></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="onPageChange" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import {ref,reactive,computed,onMounted} from 'vue'
import {reportApi} from '@/api/index'
import {downloadBlob} from '@/utils/request'
const all=ref([]),rows=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0)
const s=reactive({days:90})
const stagnantTotal=computed(()=>all.value.reduce((sum,r)=>sum+Number(r.stock||0),0))
const stagnantAmount=computed(()=>all.value.reduce((sum,r)=>sum+Number(r.amount||0),0))
const load=async()=>{loading.value=true;try{const r=await reportApi.stagnant({days:s.days});if(r.code===200&&r.data){all.value=r.data||[];total.value=all.value.length;applyPage()}}finally{loading.value=false}}
const applyPage=()=>{const from=(page.value-1)*size.value;rows.value=all.value.slice(from,from+size.value)}
const onSearch=()=>{page.value=1;load()}
const onPageChange=()=>applyPage()
const handleExport=()=>downloadBlob('/statistics/exportStagnant',{days:s.days},`呆滞物品_${new Date().toISOString().slice(0,10)}.xlsx`)
onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
