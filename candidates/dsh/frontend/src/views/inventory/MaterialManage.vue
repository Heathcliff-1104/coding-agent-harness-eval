<template>
  <div class="pg"><div class="pg-hd"><h3>物料管理</h3></div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="s.keyword" placeholder="编码/名称/规格/封装" style="width:240px" clearable />
        <el-button type="primary" style="margin-left:8px" @click="onSearch">查询</el-button>
        <el-button type="success" @click="openAdd">新增物料</el-button>
      </div>

      <el-table :data="rows" border stripe v-loading="loading" style="margin-top:12px">
        <el-table-column label="物料编码" prop="materialCode" width="150" />
        <el-table-column label="物料名称" prop="materialName" min-width="120" />
        <el-table-column label="封装" prop="packageType" width="80" />
        <el-table-column label="value值" prop="valueData" width="90" />
        <el-table-column label="规格型号" prop="specModel" width="130" />
        <el-table-column label="厂家" prop="manufacturer" width="110" />
        <el-table-column label="库存" prop="stock" width="80" />
        <el-table-column label="占用" prop="lockStock" width="70" />
        <el-table-column label="存放货位" prop="locationNo" width="100" />
        <el-table-column label="最低库存" prop="minStock" width="80" />
        <el-table-column label="最高库存" prop="maxStock" width="80" />
        <el-table-column label="成本" prop="materialCost" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="r"><el-tag :type="r.row.status==='出库中'?'warning':r.row.status==='空闲'?'success':'danger'" size="small">{{ r.row.status || '-' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="r">
            <el-button size="small" @click="openEdit(r.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="delMaterial(r.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total,prev,pager,next" @current-change="load" style="margin-top:12px;justify-content:flex-end" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId?'编辑物料':'新增物料'" width="640px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="物料编码"><el-input v-model="form.materialCode" placeholder="留空则入库时自动生成" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="物料名称"><el-input v-model="form.materialName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="封装"><el-input v-model="form.packageType" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="value值"><el-input v-model="form.valueData" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规格型号"><el-input v-model="form.specModel" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="厂家名称"><el-input v-model="form.manufacturer" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="仓库编码"><el-input v-model="form.warehouseCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="存放货位"><el-input v-model="form.locationNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="最低库存"><el-input-number v-model="form.minStock" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="最高库存"><el-input-number v-model="form.maxStock" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="单件成本"><el-input-number v-model="form.materialCost" :min="0" :precision="4" style="width:100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import {materialApi} from '@/api/index'

const rows=ref([]),loading=ref(false),page=ref(1),size=ref(10),total=ref(0)
const s=reactive({keyword:''})
const dialogVisible=ref(false),editId=ref(null)
const form=reactive({materialCode:'',materialName:'',packageType:'',valueData:'',specModel:'',manufacturer:'',warehouseCode:'',locationNo:'',remark:'',minStock:0,maxStock:null,materialCost:0})

const load=async()=>{loading.value=true;try{const r=await materialApi.page({pageNum:page.value,pageSize:size.value,keyword:s.keyword});if(r.code===200&&r.data){rows.value=r.data.records||[];total.value=r.data.total||0}}finally{loading.value=false}}
const onSearch=()=>{page.value=1;load()}

const openAdd=()=>{editId.value=null;Object.assign(form,{materialCode:'',materialName:'',packageType:'',valueData:'',specModel:'',manufacturer:'',warehouseCode:'',locationNo:'',remark:'',minStock:0,maxStock:null,materialCost:0});dialogVisible.value=true}
const openEdit=(row)=>{editId.value=row.id;Object.assign(form,{materialCode:row.materialCode,materialName:row.materialName,packageType:row.packageType,valueData:row.valueData,specModel:row.specModel,manufacturer:row.manufacturer,warehouseCode:row.warehouseCode,locationNo:row.locationNo,remark:row.remark,minStock:row.minStock||0,maxStock:row.maxStock,materialCost:row.materialCost||0});dialogVisible.value=true}

const handleSave=async()=>{
  if(!form.materialName){ElMessage.warning('请填写物料名称');return}
  try{
    const payload={...form}
    if(editId.value)payload.id=editId.value
    const r=editId.value?await materialApi.update(payload):await materialApi.add(payload)
    if(r.code===200){ElMessage.success('保存成功');dialogVisible.value=false;load()}
    else ElMessage.error(r.msg||'保存失败')
  }catch(e){ElMessage.error((e&&e.message)||'保存失败')}
}

const delMaterial=async(row)=>{
  try{
    await ElMessageBox.confirm(`确定删除物料【${row.materialName}】？如存在历史单据将无法删除。`,'提示',{type:'warning'})
    const r=await materialApi.del(row.id)
    if(r.code===200){ElMessage.success('已删除');load()}
  }catch{}
}

onMounted(load)
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}.toolbar{display:flex;align-items:center}</style>
