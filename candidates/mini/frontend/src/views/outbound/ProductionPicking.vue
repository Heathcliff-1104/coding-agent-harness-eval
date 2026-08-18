<template>
  <div class="pg"><div class="pg-hd"><h3>生产领料</h3></div>
    <el-card>
      <!-- 模式切换 -->
      <el-tabs v-model="mode">
        <el-tab-pane label="导入BOM表" name="import" />
        <el-tab-pane label="配置BOM清单" name="config" />
      </el-tabs>

      <!-- ======== 导入BOM ======== -->
      <template v-if="mode==='import'">
        <div class="upload-area">
          <el-upload drag :auto-upload="false" :on-change="handleBomUpload" accept=".xlsx,.xls" :show-file-list="false">
            <el-icon size="48"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽或<em>点击上传</em> BOM Excel文件</div>
            <div class="el-upload__tip">支持 .xlsx / .xls 格式</div>
          </el-upload>
        </div>
        <div v-if="bomItems.length" style="margin-top:16px">
          <div class="tb-ctrl"><span class="tb-title">BOM匹配结果</span><el-tag v-for="(v,k) in bomSummary" :key="k" :type="v.type" size="small" style="margin-left:8px">{{ v.text }}</el-tag></div>
          <el-table :data="bomItems" border max-height="400">
            <el-table-column label="物料编码" prop="materialCode" width="140" />
            <el-table-column label="物料名称" prop="materialName" width="120" />
            <el-table-column label="封装" prop="packageType" width="90" />
            <el-table-column label="规格型号" prop="specModel" width="130" />
            <el-table-column label="厂家批次" prop="batchNo" width="110" />
            <el-table-column label="需要数量" prop="needNum" width="90" />
            <el-table-column label="库存状态" width="120">
              <template #default="r">
                <el-tag v-if="r.row.stockStatus==='sufficient'" type="success" size="small">库存充足</el-tag>
                <el-tag v-else-if="r.row.stockStatus==='insufficient'" type="warning" size="small">库存不足</el-tag>
                <el-tag v-else-if="r.row.stockStatus==='occupied'" type="info" size="small">被占用</el-tag>
                <el-tag v-else type="danger" size="small">缺料</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="当前库存" prop="currentStock" width="90" />
            <el-table-column label="需要补货" width="90">
              <template #default="r"><span v-if="r.row.shortage>0" style="color:#f56c6c">{{ r.row.shortage }}</span><span v-else>-</span></template>
            </el-table-column>
          </el-table>
          <div style="margin-top:12px"><el-button type="primary" @click="saveAsPlan">保存为备料计划单</el-button></div>
        </div>
        <el-divider />
        <el-button type="primary" @click="submitBomOutbound" :disabled="!bomItems.length">发起钉钉出库审批</el-button>
      </template>

      <!-- ======== 配置BOM ======== -->
      <template v-if="mode==='config'">
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="搜索物料名称/编码" style="width:260px" clearable @input="searchMaterial" />
          <el-select v-model="filterCategory" placeholder="封装筛选" clearable style="width:140px;margin-left:8px" @change="searchMaterial">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </div>
        <!-- 物料库检索结果 -->
        <el-table :data="searchResults" border max-height="300" @selection-change="handleSelect" style="margin-top:12px" ref="searchTableRef">
          <el-table-column type="selection" width="45" />
          <el-table-column label="物料编码" prop="materialCode" width="140" />
          <el-table-column label="物料名称" prop="materialName" width="140" />
          <el-table-column label="封装" prop="packageType" width="90" />
          <el-table-column label="规格型号" prop="specModel" width="140" />
          <el-table-column label="库存" width="80"><template #default="r"><el-tag :type="r.row.stock>0?'success':'danger'" size="small">{{ r.row.stock }}</el-tag></template></el-table-column>
          <el-table-column label="占用" width="70"><template #default="r"><span v-if="r.row.lockStock>0" style="color:#e6a23c">{{ r.row.lockStock }}</span><span v-else>-</span></template></el-table-column>
          <el-table-column label="操作" width="70"><template #default="r"><el-button type="primary" link size="small" @click="addToBomList(r.row)">添加</el-button></template></el-table-column>
        </el-table>

        <el-divider />
        <div class="tb-ctrl"><span class="tb-title">配置BOM清单</span></div>
        <el-table :data="configBomList" border>
          <el-table-column label="物料编码" width="140"><template #default="r"><el-input v-model="r.row.materialCode" size="small" /></template></el-table-column>
          <el-table-column label="物料名称" width="140"><template #default="r"><el-input v-model="r.row.materialName" size="small" /></template></el-table-column>
          <el-table-column label="封装" width="90"><template #default="r"><el-input v-model="r.row.packageType" size="small" /></template></el-table-column>
          <el-table-column label="Value值" width="100"><template #default="r"><el-input v-model="r.row.valueData" size="small" /></template></el-table-column>
          <el-table-column label="规格型号" width="140"><template #default="r"><el-input v-model="r.row.specModel" size="small" /></template></el-table-column>
          <el-table-column label="厂家批次" width="110"><template #default="r"><el-input v-model="r.row.batchNo" size="small" /></template></el-table-column>
          <el-table-column label="出库数量" width="100"><template #default="r"><el-input-number v-model="r.row.outNum" :min="1" size="small" controls-position="right" /></template></el-table-column>
          <el-table-column label="备注" min-width="150"><template #default="r"><el-input v-model="r.row.remark" size="small" /></template></el-table-column>
          <el-table-column label="操作" width="60"><template #default="r"><el-button type="danger" link size="small" @click="configBomList.splice(r.$index,1)">删除</el-button></template></el-table-column>
        </el-table>
        <div style="margin-top:12px"><el-button type="primary" @click="submitConfigOutbound" :disabled="!configBomList.length">发起钉钉出库审批</el-button></div>
      </template>
    </el-card>

    <!-- 待出库单据 -->
    <el-card style="margin-top:16px">
      <template #header><span style="font-weight:600">待出库/已出库单据</span></template>
      <el-table :data="outOrders" border stripe v-loading="orderLoading">
        <el-table-column label="出库单号" prop="outboundCode" width="180" />
        <el-table-column label="申请人" prop="applyUser" width="100" />
        <el-table-column label="类型" width="100"><template #default="r"><span>{{ r.row.outType===1?'生产领料':'其他' }}</span></template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="r">
          <el-tag :type="r.row.orderStatus===0?'warning':r.row.orderStatus===1?'success':'danger'" size="small">{{ r.row.orderStatus===0?'待审批':r.row.orderStatus===1?'已出库':'已驳回' }}</el-tag>
        </template></el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="备注" prop="remark" min-width="160" />
      </el-table>
      <el-pagination v-model:current-page="p" v-model:page-size="sz" :total="t" layout="total,prev,pager,next" @current-change="loadOrders" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { outboundApi, materialApi } from '@/api/index'
import * as XLSX from 'xlsx'

const mode=ref('config')
const keyword=ref(''),filterCategory=ref(''),categories=ref([])
const searchResults=ref([]),bomItems=ref([]),configBomList=ref([]),selectedMaterials=ref([])
const outOrders=ref([]),orderLoading=ref(false),p=ref(1),sz=ref(10),t=ref(0)
const bomSummary=ref([])
const searchTableRef=ref(null)

const searchMaterial=async()=>{try{const r=await materialApi.page({pageNum:1,pageSize:20,keyword:keyword.value,warehouseCode:filterCategory.value||undefined});if(r.code===200&&r.data){searchResults.value=r.data.records||[]}}catch{}}

const loadCategories=async()=>{try{const r=await materialApi.list();if(r.code===200&&r.data){const set=new Set();r.data.forEach(m=>{if(m.packageType)set.add(m.packageType)});categories.value=[...set]}}catch{}}

const handleSelect=(rows)=>{ selectedMaterials.value=rows }

const addToBomList=(mat)=>{configBomList.value.push({materialId:mat.id,materialCode:mat.materialCode,materialName:mat.materialName,packageType:mat.packageType||'',valueData:mat.valueData||'',specModel:mat.specModel||'',batchNo:'',outNum:1,remark:''})}

const handleBomUpload=(file)=>{const reader=new FileReader();reader.onload=(e)=>{try{const wb=XLSX.read(e.target.result,{type:'binary'});const ws=wb.Sheets[wb.SheetNames[0]];const data=XLSX.utils.sheet_to_json(ws);bomItems.value=data.map((r,i)=>({materialCode:r['物料编码']||'',materialName:r['物料名称']||'',packageType:r['封装']||'',specModel:r['规格型号']||'',batchNo:r['批次']||'',needNum:parseInt(r['数量'])||1,stockStatus:'unknown',currentStock:0,shortage:0}));matchBomInventory();ElMessage.success(`已解析${bomItems.value.length}条物料`)}catch{ElMessage.error('解析失败，请检查格式')}};reader.readAsBinaryString(file.raw)}

const matchBomInventory=async()=>{const promises=bomItems.value.map(async(item,i)=>{try{const r=await materialApi.page({pageNum:1,pageSize:1,keyword:item.materialCode||item.materialName});if(r.code===200&&r.data&&r.data.records[0]){const m=r.data.records[0];item.currentStock=m.stock||0;item.shortage=Math.max(0,item.needNum-m.stock);if(m.stock>=item.needNum)item.stockStatus='sufficient';else if(m.stock>0)item.stockStatus='insufficient';else if(m.lockStock>0)item.stockStatus='occupied';else item.stockStatus='out_of_stock'}else{item.stockStatus='out_of_stock';item.shortage=item.needNum}}catch{}});await Promise.all(promises);updateSummary()}

const updateSummary=()=>{const sufficient=bomItems.value.filter(i=>i.stockStatus==='sufficient').length;const insufficient=bomItems.value.filter(i=>i.stockStatus==='insufficient').length;const out=bomItems.value.filter(i=>i.stockStatus==='out_of_stock').length;const occupied=bomItems.value.filter(i=>i.stockStatus==='occupied').length;bomSummary.value=[{text:`库存充足:${sufficient}`,type:'success'},{text:`库存不足:${insufficient}`,type:'warning'},{text:`缺料:${out}`,type:'danger'},{text:`被占用:${occupied}`,type:'info'}]}

const saveAsPlan=async()=>{try{const items=bomItems.value.map(i=>({materialId:i.materialId,materialCode:i.materialCode,materialName:i.materialName,packageType:i.packageType,valueData:i.valueData,specModel:i.specModel,batchNo:i.batchNo,needNum:i.needNum}));const r=await outboundApi.bomSavePlan({items,remark:'BOM导入备料计划'});if(r.code===200){ElMessage.success('已保存为备料计划单: '+r.data)}else{ElMessage.error(r.msg||'保存失败')}}catch(e){ElMessage.error(e.message||'保存失败')}}

const submitBomOutbound=async()=>{try{const r=await outboundApi.saveOrder({outType:1,applyUser:localStorage.getItem('username')||'',remark:'BOM导入',itemList:bomItems.value.map(i=>({materialId:i.materialId,materialCode:i.materialCode,batchNo:i.batchNo,outNum:i.needNum}))});if(r.code===200){ElMessage.success('出库申请已提交钉钉审批');loadOrders()}else{ElMessage.error(r.msg||'提交失败')}}catch(e){ElMessage.error(e.message||'提交失败')}}

const submitConfigOutbound=async()=>{try{const r=await outboundApi.saveOrder({outType:1,applyUser:localStorage.getItem('username')||'',remark:'手动配置',itemList:configBomList.value.map(i=>({materialId:i.materialId,materialCode:i.materialCode,batchNo:i.batchNo,outNum:i.outNum}))});if(r.code===200){ElMessage.success('出库申请已提交钉钉审批');configBomList.value=[];loadOrders()}else{ElMessage.error(r.msg||'提交失败')}}catch(e){ElMessage.error(e.message||'提交失败')}}

const loadOrders=async()=>{orderLoading.value=true;try{const r=await outboundApi.page({pageNum:p.value,pageSize:sz.value});if(r.code===200&&r.data){outOrders.value=r.data.records||[];t.value=r.data.total||0}}finally{orderLoading.value=false}}

onMounted(()=>{loadOrders();loadCategories()})
</script>

<style scoped>
.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}
.toolbar{display:flex;align-items:center}
.upload-area{margin:16px 0}
.tb-ctrl{display:flex;justify-content:space-between;align-items:center;margin:12px 0}
.tb-title{font-size:14px;font-weight:600}
</style>
