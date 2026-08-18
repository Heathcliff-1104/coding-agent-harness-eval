<template>
  <div class="pg"><div class="pg-hd"><h3>入库统计</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="时间范围"><el-date-picker v-model="s.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" /></el-form-item>
        <el-form-item label="统计维度"><el-select v-model="s.dimension" style="width:140px"><el-option label="按时间" value="time" /><el-option label="按物料" value="material" /><el-option label="按供应商" value="supplier" /></el-select></el-form-item>
        <el-form-item v-if="s.dimension==='time'" label="粒度"><el-select v-model="s.groupBy" style="width:120px"><el-option label="按日" value="%Y-%m-%d" /><el-option label="按周" value="%Y-%u" /><el-option label="按月" value="%Y-%m" /><el-option label="按年" value="%Y" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="handleExport">导出Excel</el-button></el-form-item>
      </el-form>
      <div v-if="s.dimension==='time'" ref="chartRef" style="width:100%;height:360px;margin-top:16px"></div>
      <el-table :data="rows" border stripe style="margin-top:12px">
        <template v-if="s.dimension==='time'">
          <el-table-column label="时间段" prop="period" width="180" />
          <el-table-column label="入库次数" prop="cnt" width="120" />
          <el-table-column label="入库总数" prop="total" width="120" />
        </template>
        <template v-else-if="s.dimension==='material'">
          <el-table-column label="物料编码" prop="materialCode" width="140" />
          <el-table-column label="物料名称" prop="materialName" min-width="140" />
          <el-table-column label="入库总数" prop="totalIn" width="120" />
          <el-table-column label="入库次数" prop="inTimes" width="100" />
          <el-table-column label="平均批次数量" prop="avgBatchNum" width="120" />
        </template>
        <template v-else>
          <el-table-column label="供应商" prop="supplier" min-width="160" />
          <el-table-column label="入库次数" prop="cnt" width="120" />
          <el-table-column label="入库总数" prop="total" width="120" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted,nextTick,watch} from 'vue'
import {ElMessage} from 'element-plus'
import {reportApi} from '@/api/index'
import {downloadWithAuth} from '@/utils/download'
import * as echarts from 'echarts'
const rows=ref([]),chartRef=ref(null)
let chart=null
const s=reactive({dateRange:null,groupBy:'%Y-%m-%d',dimension:'time'})
const load=async()=>{if(!s.dateRange)return;try{let r
if(s.dimension==='time'){r=await reportApi.inboundStats({start:s.dateRange[0],end:s.dateRange[1],groupBy:s.groupBy})}
else if(s.dimension==='material'){r=await reportApi.inboundByMaterial({start:s.dateRange[0],end:s.dateRange[1]})}
else{r=await reportApi.inboundBySupplier({start:s.dateRange[0],end:s.dateRange[1]})}
if(r.code===200&&r.data){rows.value=r.data||[];await nextTick();if(s.dimension==='time')renderChart()}}catch{}}
const renderChart=()=>{if(!chartRef.value)return;if(chart)chart.dispose();chart=echarts.init(chartRef.value);chart.setOption({tooltip:{trigger:'axis'},xAxis:{type:'category',data:rows.value.map(r=>r.period)},yAxis:{type:'value'},series:[{name:'入库数量',type:'bar',data:rows.value.map(r=>parseInt(r.total)||0),itemStyle:{color:'#409eff'}}]})}
const onSearch=()=>load()
const handleExport=async()=>{if(s.dateRange){await downloadWithAuth(reportApi.exportInbound({start:s.dateRange[0],end:s.dateRange[1]}));ElMessage.success('导出完成')}}
onMounted(()=>{const now=new Date();s.dateRange=[new Date(now.getFullYear(),now.getMonth(),1).toISOString().slice(0,10),now.toISOString().slice(0,10)];load()})
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
