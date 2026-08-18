<template>
  <div class="pg"><div class="pg-hd"><h3>导出报表</h3></div>
    <el-row :gutter="20">
      <el-col :span="8" v-for="r in reportList" :key="r.key">
        <el-card shadow="hover" class="export-card" @click="handleExport(r)">
          <div class="card-icon"><el-icon size="36"><component :is="r.icon" /></el-icon></div>
          <div class="card-text">
            <h4>{{ r.title }}</h4>
            <p>{{ r.desc }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import {Document, TrendCharts, Box, List} from '@element-plus/icons-vue'
import {downloadBlob} from '@/utils/request'

const stamp=()=>{const d=new Date();return `${d.getFullYear()}${String(d.getMonth()+1).padStart(2,'0')}${String(d.getDate()).padStart(2,'0')}_${String(d.getHours()).padStart(2,'0')}${String(d.getMinutes()).padStart(2,'0')}${String(d.getSeconds()).padStart(2,'0')}`}

const reportList=[
  {key:'inventory-detail',title:'库存明细',desc:'导出所有物料库存明细报表',icon:List,action:()=>downloadBlob('/statistics/exportInventory',{},`库存明细_${stamp()}.xlsx`)},
  {key:'inbound-stats',title:'入库统计',desc:'导出时间段内入库统计数据',icon:TrendCharts,action:()=>{const d=new Date();const end=d.toISOString().slice(0,10);downloadBlob('/statistics/exportInbound',{start:`${d.getFullYear()}-01-01`,end},`入库统计_${stamp()}.xlsx`)}},
  {key:'outbound-stats',title:'出库统计',desc:'导出时间段内出库统计数据',icon:TrendCharts,action:()=>{const d=new Date();const end=d.toISOString().slice(0,10);downloadBlob('/statistics/exportOutbound',{start:`${d.getFullYear()}-01-01`,end},`出库统计_${stamp()}.xlsx`)}},
  {key:'stagnant',title:'呆滞物品',desc:'导出呆滞物品清单',icon:Box,action:()=>downloadBlob('/statistics/exportStagnant',{days:90},`呆滞物品_${stamp()}.xlsx`)},
]

const handleExport=(r)=>{r.action()}
</script>

<style scoped>
.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}
.export-card{cursor:pointer;transition:all 0.2s}.export-card:hover{transform:translateY(-2px);box-shadow:0 4px 16px rgba(0,0,0,0.12)}
.card-icon{color:#409eff;margin-bottom:12px}.card-text h4{margin:0 0 6px}.card-text p{margin:0;color:#999;font-size:12px}
</style>
