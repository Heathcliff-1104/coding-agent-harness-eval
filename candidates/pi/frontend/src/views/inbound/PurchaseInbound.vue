<template>
  <div class="pg">
    <div class="pg-hd"><h3>采购入库</h3></div>
    <el-card>
      <div class="toolbar">
        <el-button type="primary" @click="fetchDingTalkOrders" :loading="fetching">读取钉钉采购单</el-button>
        <el-button v-if="canOperate" @click="openAdd">手动新建入库单</el-button>
        <el-button v-if="canOperate" type="warning" @click="doBatchConfirm" :disabled="!selectedIds.length">批量审核（{{ selectedIds.length }}）</el-button>
      </div>

      <el-table :data="orders" border stripe v-loading="fetching" style="margin-top:16px" @selection-change="onSelectionChange">
        <el-table-column v-if="canOperate" type="selection" width="45" />
        <el-table-column label="单据号" prop="billNo" width="160" />
        <el-table-column label="入库类型" width="90"><template #default="s"><span>{{ s.row.inType==='RETURN'?'退库':'采购' }}</span></template></el-table-column>
        <el-table-column label="供应商" prop="supplier" width="120" />
        <el-table-column label="申请人" prop="applyUser" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="s">
            <el-tag :type="s.row.orderStatus===0?'warning':s.row.orderStatus===1?'success':'danger'" size="small">
              {{ s.row.orderStatus===0?'待入库':s.row.orderStatus===1?'已入库':'已驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="s">
            <el-button size="small" @click="openDetail(s.row)">查看</el-button>
            <el-button v-if="s.row.orderStatus===0 && canOperate" size="small" type="success" @click="confirmInbound(s.row)">确认入库</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
        layout="total, prev, pager, next" @current-change="load" style="margin-top:16px;justify-content:flex-end" />
    </el-card>

    <!-- 入库详情/编辑弹窗 -->
    <el-dialog v-model="detailVisible" :title="'入库单 - ' + curBillNo" width="95%" top="5vh">
      <el-form :model="detailForm" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="单据号"><el-input v-model="detailForm.billNo" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="供应商"><el-input v-model="detailForm.supplier" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="申请人"><el-input v-model="detailForm.userName" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="入库类型">
            <el-select v-model="detailForm.inType" style="width:100%">
              <el-option label="采购入库" value="PURCHASE" />
              <el-option label="退库入库" value="RETURN" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="8"><el-form-item label="退库原因"><el-input v-model="detailForm.returnReason" /></el-form-item></el-col>
        </el-row>
      </el-form>

      <div class="tb-ctrl"><span class="tb-title">物料明细</span>
        <div style="display:flex;gap:8px">
          <el-select v-model="pickerMaterialId" filterable placeholder="从物料库选择" style="width:260px" @change="pickMaterial">
            <el-option v-for="m in materialOptions" :key="m.id" :label="(m.materialCode||'') + ' ' + m.materialName + ' ' + (m.specModel||'')" :value="m.id" />
          </el-select>
          <el-button type="primary" size="small" @click="addRow">添加空行</el-button>
        </div>
      </div>

      <el-table :data="detailForm.itemList" border max-height="420">
        <el-table-column label="物料名称" width="120">
          <template #default="s"><el-input v-model="s.row.materialName" size="small" /></template>
        </el-table-column>
        <el-table-column label="封装" width="90">
          <template #default="s"><el-input v-model="s.row.packageType" size="small" /></template>
        </el-table-column>
        <el-table-column label="value值" width="100">
          <template #default="s"><el-input v-model="s.row.valueData" size="small" /></template>
        </el-table-column>
        <el-table-column label="规格型号" width="130">
          <template #default="s"><el-input v-model="s.row.specModel" size="small" /></template>
        </el-table-column>
        <el-table-column label="厂家名称" width="110">
          <template #default="s"><el-input v-model="s.row.manufacturerName" size="small" /></template>
        </el-table-column>
        <el-table-column label="厂家批次" width="110">
          <template #default="s"><el-input v-model="s.row.batchNo" size="small" /></template>
        </el-table-column>
        <el-table-column label="入库数量" width="100">
          <template #default="s"><el-input-number v-model="s.row.num" :min="1" size="small" controls-position="right" /></template>
        </el-table-column>
        <el-table-column label="存放货位" width="120">
          <template #default="s"><el-input v-model="s.row.locationNo" size="small" placeholder="货架位置" /></template>
        </el-table-column>
        <el-table-column label="备注" min-width="120">
          <template #default="s"><el-input v-model="s.row.remark" size="small" placeholder="增删原因" /></template>
        </el-table-column>
        <el-table-column label="操作" width="60" fixed="right">
          <template #default="s"><el-button type="danger" link size="small" @click="detailForm.itemList.splice(s.$index,1)">删除</el-button></template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="detailVisible=false">取消</el-button>
        <el-button v-if="curOrderId" type="warning" @click="handleSaveDraft">保存草稿</el-button>
        <el-button type="primary" @click="handleSubmit">提交入库单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { inboundApi, materialApi } from '@/api/index'
import { getRole } from '@/utils/permission'

const role = getRole()
const canOperate = computed(() => role === 'admin' || role === 'warehouse')

const orders = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const fetching = ref(false)
const detailVisible = ref(false)
const curBillNo = ref('')
const curOrderId = ref(null)
const selectedIds = ref([])
const materialOptions = ref([])
const pickerMaterialId = ref(null)

const detailForm = reactive({ billNo: '', supplier: '', userName: '', inType: 'PURCHASE', returnReason: '', remark: '', itemList: [] })

const load = async () => {
  fetching.value = true
  try {
    const res = await inboundApi.page({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200 && res.data) {
      orders.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally { fetching.value = false }
}

const loadMaterials = async () => {
  try {
    const r = await materialApi.list()
    if (r.code === 200) materialOptions.value = r.data || []
  } catch { /* ignore */ }
}

const fetchDingTalkOrders = async () => {
  ElMessage.info('正在从钉钉读取已审批的采购单...')
  await load()
}

const onSelectionChange = (rows) => { selectedIds.value = rows.map(r => r.id) }

const doBatchConfirm = async () => {
  try {
    const { value: operUser } = await ElMessageBox.prompt('请输入库管员账号', '批量审核入库')
    if (!operUser) return
    const r = await inboundApi.batchConfirm(selectedIds.value, operUser)
    if (r.code === 200) { ElMessage.success('批量审核完成'); load() }
    else ElMessage.error(r.msg || '批量审核失败')
  } catch { /* cancel */ }
}

const openDetail = async (row) => {
  curBillNo.value = row.billNo
  curOrderId.value = row.id
  try {
    const res = await inboundApi.get(row.id)
    if (res.code === 200 && res.data) {
      detailForm.billNo = res.data.billNo || ''
      detailForm.supplier = res.data.supplier || ''
      detailForm.userName = res.data.userName || ''
      detailForm.inType = res.data.inType || 'PURCHASE'
      detailForm.returnReason = res.data.returnReason || ''
      detailForm.remark = res.data.remark || ''
      detailForm.itemList = (res.data.itemList || []).map(i => ({
        materialId: i.materialId, materialName: i.materialName || '', packageType: i.packageType || '',
        valueData: i.valueData || '', specModel: i.specModel || '', manufacturerName: i.manufacturerName || '',
        batchNo: i.batchNo || '', num: i.num || 1, locationNo: i.locationNo || '', remark: i.remark || ''
      }))
    }
  } catch { detailForm.itemList = [] }
  detailVisible.value = true
}

const openAdd = () => {
  curBillNo.value = ''
  curOrderId.value = null
  detailForm.billNo = 'RK' + Date.now()
  detailForm.supplier = ''
  detailForm.userName = localStorage.getItem('realName') || localStorage.getItem('username') || ''
  detailForm.inType = 'PURCHASE'
  detailForm.returnReason = ''
  detailForm.remark = ''
  detailForm.itemList = []
  detailVisible.value = true
}

const pickMaterial = (mid) => {
  const m = materialOptions.value.find(x => x.id === mid)
  if (!m) return
  detailForm.itemList.push({
    materialId: m.id, materialName: m.materialName || '', packageType: m.packageType || '',
    valueData: m.valueData || '', specModel: m.specModel || '', manufacturerName: m.manufacturerName || '',
    batchNo: m.manufacturerBatch || '', num: 1, locationNo: m.locationNo || '', remark: ''
  })
  pickerMaterialId.value = null
}

const addRow = () => detailForm.itemList.push({ materialId: null, materialName: '', packageType: '', valueData: '', specModel: '', manufacturerName: '', batchNo: '', num: 1, locationNo: '', remark: '' })

const handleSaveDraft = async () => {
  try {
    const res = curOrderId.value
      ? await inboundApi.editDraft(curOrderId.value, detailForm)
      : await inboundApi.saveDraft(detailForm)
    if (res.code === 200) { ElMessage.success('草稿已保存'); detailVisible.value = false; load() }
  } catch { ElMessage.error('保存失败') }
}

const handleSubmit = async () => {
  try {
    const res = curOrderId.value
      ? await inboundApi.editDraft(curOrderId.value, detailForm)
      : await inboundApi.saveOrder(detailForm)
    if (res.code === 200) {
      ElMessage.success('入库单已提交')
      detailVisible.value = false
      load()
    }
  } catch { ElMessage.error('提交失败') }
}

const confirmInbound = async (row) => {
  try {
    const { value: operUser } = await ElMessageBox.prompt('请输入库管员账号', '确认入库')
    if (!operUser) return
    const res = await inboundApi.confirm(row.id, operUser)
    if (res.code === 200) {
      ElMessage.success('入库完成')
      load()
    }
  } catch { /* cancel */ }
}

onMounted(() => { load(); loadMaterials() })
</script>

<style scoped>
.pg { padding:0; }
.pg-hd { margin-bottom:16px; }
.pg-hd h3 { margin:0; font-size:18px; font-weight:600; }
.toolbar { display:flex; gap:12px; }
.tb-ctrl { display:flex; justify-content:space-between; align-items:center; margin:16px 0; }
.tb-title { font-size:14px; font-weight:600; }
</style>
