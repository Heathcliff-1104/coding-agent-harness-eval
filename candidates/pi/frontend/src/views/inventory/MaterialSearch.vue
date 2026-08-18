<template>
  <div class="pg"><div class="pg-hd"><h3>物料检索</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="物料编码"><el-input v-model="s.code" clearable /></el-form-item>
        <el-form-item label="物料名称"><el-input v-model="s.name" clearable /></el-form-item>
        <el-form-item label="封装"><el-input v-model="s.packageType" clearable /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">检索</el-button><el-button @click="reset">重置</el-button>
          <el-button v-if="canEdit" type="success" @click="openAdd">新增物料</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="rows" border stripe v-loading="loading" style="margin-top:12px">
        <el-table-column label="物料编码" prop="materialCode" width="140" />
        <el-table-column label="物料名称" prop="materialName" min-width="140" />
        <el-table-column label="封装" prop="packageType" width="90" />
        <el-table-column label="Value值" prop="valueData" width="100" />
        <el-table-column label="规格型号" prop="specModel" width="140" />
        <el-table-column label="厂家名称" width="110"><template #default="r"><span>{{ r.row.manufacturerName || '-' }}</span></template></el-table-column>
        <el-table-column label="厂家批次" width="110"><template #default="r"><span>{{ r.row.manufacturerBatch || '-' }}</span></template></el-table-column>
        <el-table-column label="库存数量" prop="stock" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="r"><el-tag v-if="r.row.lockStock>0" type="warning" size="small">占用{{r.row.lockStock}}</el-tag><el-tag v-else-if="r.row.stock>0" type="success" size="small">空闲</el-tag><el-tag v-else type="danger" size="small">缺货</el-tag></template>
        </el-table-column>
        <el-table-column label="存放货位" prop="locationNo" width="120" />
        <el-table-column label="备注" min-width="120"><template #default="r"><span>{{ r.row.remark || '-' }}</span></template></el-table-column>
        <el-table-column v-if="canEdit" label="操作" width="120" fixed="right">
          <template #default="r">
            <el-button size="small" @click="openEdit(r.row)">编辑</el-button>
            <el-button v-if="isAdmin" size="small" type="danger" @click="delMaterial(r.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="load" style="margin-top:12px;justify-content:flex-end" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId?'编辑物料':'新增物料'" width="640px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="物料编码"><el-input v-model="form.materialCode" /></el-form-item>
        <el-form-item label="物料名称"><el-input v-model="form.materialName" /></el-form-item>
        <el-form-item label="封装"><el-input v-model="form.packageType" /></el-form-item>
        <el-form-item label="Value值"><el-input v-model="form.valueData" /></el-form-item>
        <el-form-item label="规格型号"><el-input v-model="form.specModel" /></el-form-item>
        <el-form-item label="厂家名称"><el-input v-model="form.manufacturerName" /></el-form-item>
        <el-form-item label="厂家批次"><el-input v-model="form.manufacturerBatch" /></el-form-item>
        <el-form-item label="库存数量">
          <el-input-number v-model="form.stock" :min="0" :disabled="editId!==null" style="width:100%" />
          <div v-if="editId" style="font-size:12px;color:#999;line-height:1.4">库存/占用只能通过入库、出库流程变更，编辑物料不会修改库存。</div>
        </el-form-item>
        <el-form-item label="最低库存"><el-input-number v-model="form.minStock" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="最高库存"><el-input-number v-model="form.maxStock" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="单件成本"><el-input-number v-model="form.materialCost" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="存放货位"><el-input v-model="form.locationNo" /></el-form-item>
        <el-form-item label="呆滞天数"><el-input-number v-model="form.stagnationDays" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref,reactive,computed,onMounted} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import {materialApi} from '@/api/index'
import {getRole} from '@/utils/permission'

const role = getRole()
const isAdmin = role === 'admin'
const canEdit = computed(() => role === 'admin' || role === 'warehouse')

const rows=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0)
const s=reactive({code:'',name:'',packageType:''})
const dialogVisible=ref(false),editId=ref(null)
const form=reactive({materialCode:'',materialName:'',packageType:'',valueData:'',specModel:'',manufacturerName:'',manufacturerBatch:'',stock:0,lockStock:0,minStock:0,maxStock:0,materialCost:0,locationNo:'',stagnationDays:90,remark:''})

const load=async()=>{loading.value=true;try{const r=await materialApi.page({pageNum:page.value,pageSize:size.value,materialCode:s.code,materialName:s.name,warehouseCode:s.packageType});if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}finally{loading.value=false}}
const onSearch=()=>{page.value=1;load()}
const reset=()=>{s.code='';s.name='';s.packageType='';page.value=1;load()}
const openAdd=()=>{editId.value=null;Object.assign(form,{materialCode:'',materialName:'',packageType:'',valueData:'',specModel:'',manufacturerName:'',manufacturerBatch:'',stock:0,minStock:0,maxStock:0,materialCost:0,locationNo:'',stagnationDays:90,remark:''});dialogVisible.value=true}
const openEdit=(row)=>{editId.value=row.id;Object.assign(form,{materialCode:row.materialCode||'',materialName:row.materialName||'',packageType:row.packageType||'',valueData:row.valueData||'',specModel:row.specModel||'',manufacturerName:row.manufacturerName||'',manufacturerBatch:row.manufacturerBatch||'',stock:Number(row.stock||0),minStock:Number(row.minStock||0),maxStock:Number(row.maxStock||0),materialCost:Number(row.materialCost||0),locationNo:row.locationNo||'',stagnationDays:row.stagnationDays||90,remark:row.remark||''});dialogVisible.value=true}
const handleSave=async()=>{
  if(!form.materialName){ElMessage.warning('物料名称不能为空');return}
  try{
    if(editId.value){
      // 编辑：不提交 stock/lockStock（只读，库存仅能通过出入库流程变更）
      const {stock,lockStock,...payload}=form
      await materialApi.update({id:editId.value,...payload})
    }
    else{await materialApi.add(form)}
    ElMessage.success('操作成功');dialogVisible.value=false;load()
  }catch{ElMessage.error('操作失败')}
}
const delMaterial=async(row)=>{try{await ElMessageBox.confirm('确定删除该物料？','删除物料');await materialApi.del(row.id);ElMessage.success('已删除');load()}catch{}}
onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
