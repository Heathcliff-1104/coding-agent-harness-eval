<template>
  <div class="pg"><div class="pg-hd"><h3>库存预警</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="预警类型"><el-select v-model="s.alertType" clearable><el-option label="低库存" :value="1" /><el-option label="超储" :value="2" /></el-select></el-form-item>
        <el-form-item label="处理状态"><el-select v-model="s.handled" clearable><el-option label="未处理" :value="0" /><el-option label="已处理" :value="1" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="doManualScan">手动扫描</el-button></el-form-item>
      </el-form>
      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column label="物料编码" prop="materialCode" width="140" />
        <el-table-column label="物料名称" prop="materialName" width="140" />
        <el-table-column label="预警类型" width="90"><template #default="r"><el-tag :type="r.row.alertType===1?'warning':'info'" size="small">{{ r.row.alertType===1?'低库存':'超储' }}</el-tag></template></el-table-column>
        <el-table-column label="当前库存" prop="currentStock" width="90" />
        <el-table-column label="阈值" prop="thresholdStock" width="90" />
        <el-table-column label="处理状态" width="90"><template #default="r"><el-tag :type="r.row.handled===0?'danger':'success'" size="small">{{ r.row.handled===0?'未处理':'已处理' }}</el-tag></template></el-table-column>
        <el-table-column label="处理人" prop="handler" width="100" />
        <el-table-column label="处理方式" prop="handleMethod" width="120" />
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="180"><template #default="r"><el-button v-if="r.row.handled===0" size="small" type="primary" @click="handleAlert(r.row)">标记处理</el-button><el-button v-if="r.row.alertType===1" size="small" @click="purchaseReq(r.row)">补货申请</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="load" style="margin-top:12px;justify-content:flex-end" />
    </el-card>

    <el-dialog v-model="handleVisible" title="标记处理" width="400px"><el-form label-width="80px"><el-form-item label="处理人"><el-input v-model="handleForm.handler" /></el-form-item><el-form-item label="处理方式"><el-select v-model="handleForm.method" style="width:100%"><el-option label="已采购" value="已采购" /><el-option label="已调拨" value="已调拨" /><el-option label="其他" value="其他" /></el-select></el-form-item></el-form>
      <template #footer><el-button @click="handleVisible=false">取消</el-button><el-button type="primary" @click="doHandle">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import {inventoryApi} from '@/api/index'
const rows=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0)
const s=reactive({alertType:null,handled:null})
const handleVisible=ref(false),handleForm=reactive({handler:'',method:'',curId:null})
const load=async()=>{loading.value=true;try{const r=await inventoryApi.stockAlert({pageNum:page.value,pageSize:size.value,alertType:s.alertType,handled:s.handled});if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}finally{loading.value=false}}
const onSearch=()=>{page.value=1;load()}
const doManualScan=async()=>{try{await inventoryApi.manualScan();ElMessage.success('扫描完成');load()}catch{ElMessage.error('扫描失败')}}
const handleAlert=(row)=>{handleForm.curId=row.id;handleForm.handler='';handleForm.method='';handleVisible.value=true}
const doHandle=async()=>{try{await inventoryApi.handleAlert(handleForm.curId,handleForm.handler,handleForm.method);ElMessage.success('已标记');handleVisible.value=false;load()}catch{ElMessage.error('操作失败')}}
const purchaseReq=async(row)=>{try{const {value:qty}=await ElMessageBox.prompt('请输入采购数量','补货申请',{inputValue:String(row.shortage||row.thresholdStock||1),inputValidator:v=>(v&&Number(v)>0)||'数量必须大于0'});const {value:contact}=await ElMessageBox.prompt('请输入厂家/供应商联系方式（可留空）','补货申请',{inputValue:'',inputPlaceholder:'供应商联系电话'}).catch(()=>({value:''}));const r=await inventoryApi.createRestock({materialId:row.materialId,materialCode:row.materialCode,materialName:row.materialName,purchaseQty:qty,supplierContact:contact||''});if(r.code===200){ElMessage.success('补货申请已提交')}else{ElMessage.error(r.msg||'提交失败')}}catch(e){if(e!=='cancel')ElMessage.error(e.message||'操作失败')}}
onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
