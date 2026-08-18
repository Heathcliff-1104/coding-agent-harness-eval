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
import {reportApi} from '@/api/index'
import {ElMessage} from 'element-plus'

const reportList=[
  {key:'inventory-detail',title:'库存明细',desc:'导出所有物料库存明细报表',icon:List,action:()=>ElMessage.info('导出库存明细（待后端接口）')},
  {key:'inbound-stats',title:'入库统计',desc:'导出时间段内入库统计数据',icon:TrendCharts,action:()=>{const d=new Date();window.open(reportApi.exportInbound({start:`${d.getFullYear()}-01-01`,end:d.toISOString().slice(0,10)}))}},
  {key:'outbound-stats',title:'出库统计',desc:'导出时间段内出库统计数据',icon:TrendCharts,action:()=>{const d=new Date();window.open(reportApi.exportOutbound({start:`${d.getFullYear()}-01-01`,end:d.toISOString().slice(0,10)}))}},
  {key:'stagnant',title:'呆滞物品',desc:'导出呆滞物品清单',icon:Box,action:()=>window.open(reportApi.exportStagnant(90))},
]

const handleExport=(r)=>{r.action()}
</script>

<style scoped>
.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}
.export-card{cursor:pointer;transition:all 0.2s}.export-card:hover{transform:translateY(-2px);box-shadow:0 4px 16px rgba(0,0,0,0.12)}
.card-icon{color:#409eff;margin-bottom:12px}.card-text h4{margin:0 0 6px}.card-text p{margin:0;color:#999;font-size:12px}
</style>
