<template>
  <div class="pg"><div class="pg-hd"><h3>出库统计</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="时间范围"><el-date-picker v-model="s.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" /></el-form-item>
        <el-form-item label="粒度"><el-select v-model="s.groupBy" style="width:120px"><el-option label="按日" value="%Y-%m-%d" /><el-option label="按周" value="%Y-%u" /><el-option label="按月" value="%Y-%m" /><el-option label="按年" value="%Y" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="handleExport">导出Excel</el-button></el-form-item>
      </el-form>
      <div ref="chartRef" style="width:100%;height:360px;margin-top:16px"></div>
      <el-table :data="rows" border stripe style="margin-top:12px">
        <el-table-column label="时间段" prop="period" width="180" />
        <el-table-column label="出库次数" prop="cnt" width="120" />
        <el-table-column label="出库总数" prop="total" width="120" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted,nextTick} from 'vue'
import {reportApi} from '@/api/index'
import * as echarts from 'echarts'
const rows=ref([]),chartRef=ref(null)
let chart=null
const s=reactive({dateRange:null,groupBy:'%Y-%m-%d'})
const load=async()=>{if(!s.dateRange)return;try{const r=await reportApi.outboundStats({start:s.dateRange[0],end:s.dateRange[1],groupBy:s.groupBy});if(r.code===200&&r.data){rows.value=r.data||[];await nextTick();renderChart()}}catch{}}
const renderChart=()=>{if(!chartRef.value)return;if(chart)chart.dispose();chart=echarts.init(chartRef.value);chart.setOption({tooltip:{trigger:'axis'},xAxis:{type:'category',data:rows.value.map(r=>r.period)},yAxis:{type:'value'},series:[{name:'出库数量',type:'line',smooth:true,data:rows.value.map(r=>parseInt(r.total)||0),itemStyle:{color:'#e6a23c'}}]})}
const onSearch=()=>load()
const handleExport=()=>{if(s.dateRange)window.open(reportApi.exportOutbound({start:s.dateRange[0],end:s.dateRange[1]}))}
onMounted(()=>{const now=new Date();s.dateRange=[new Date(now.getFullYear(),now.getMonth(),1).toISOString().slice(0,10),now.toISOString().slice(0,10)];load()})
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
