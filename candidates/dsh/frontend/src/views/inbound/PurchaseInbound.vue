<template>
  <div class="pg">
    <div class="pg-hd"><h3>采购入库</h3></div>
    <el-card>
      <div class="toolbar">
        <el-button type="primary" @click="fetchDingTalkOrders" :loading="fetching">读取钉钉采购单</el-button>
        <el-button @click="showAddDialog = true">手动新建入库单</el-button>
      </div>
      <el-form inline style="margin-top:12px">
        <el-form-item label="物料名称"><el-input v-model="filters.materialName" clearable placeholder="按明细物料名称筛选" style="width:170px" /></el-form-item>
        <el-form-item label="入库时间"><el-date-picker v-model="filters.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" /></el-form-item>
        <el-form-item><el-button type="primary" size="small" @click="pageNum=1;load()">筛选</el-button></el-form-item>
      </el-form>

      <div v-if="isWarehouse" style="margin:12px 0;display:flex;gap:8px">
        <el-button type="success" size="small" :disabled="!selectedOrders.length" @click="batchAudit(1)">批量通过入库</el-button>
        <el-button type="danger" size="small" :disabled="!selectedOrders.length" @click="batchAudit(2)">批量拒绝</el-button>
      </div>
      <el-table :data="orders" border stripe v-loading="fetching" style="margin-top:16px" @selection-change="sel=>selectedOrders=sel">
        <el-table-column v-if="isWarehouse" type="selection" width="45" />
        <el-table-column label="单据号" prop="billNo" width="160" />
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
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="s">
            <el-button size="small" @click="openDetail(s.row)">查看</el-button>
            <el-button v-if="s.row.orderStatus===0" size="small" type="success" @click="confirmInbound(s.row)">确认入库</el-button>
            <el-button v-if="isWarehouse" size="small" @click="exportOrder(s.row)">导出单</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
        layout="total, prev, pager, next" @current-change="load" style="margin-top:16px;justify-content:flex-end" />
    </el-card>

    <!-- 入库详情/编辑弹窗 -->
    <el-dialog v-model="detailVisible" :title="'入库单详情 - ' + curBillNo" width="90%" top="5vh">
      <el-form :model="detailForm" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="单据号"><el-input v-model="detailForm.billNo" disabled /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="供应商"><el-input v-model="detailForm.supplier" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="申请人"><el-input v-model="detailForm.userName" /></el-form-item></el-col>
        </el-row>
      </el-form>

      <div class="tb-ctrl"><span class="tb-title">物料明细</span>
        <el-button type="primary" size="small" @click="addRow">添加行</el-button>
      </div>

      <el-table :data="detailForm.itemList" border max-height="400">
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
          <template #default="s"><el-input v-model="s.row.manufacturer" size="small" /></template>
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
        <el-button type="primary" @click="handleDetailSave">保存并提交审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { inboundApi } from '@/api/index'
import request from '@/utils/request'
import { downloadWithAuth } from '@/utils/download'

const orders = ref([])
const selectedOrders = ref([])
const isWarehouse = ['admin','warehouse'].includes(localStorage.getItem('role') || '')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const fetching = ref(false)
const detailVisible = ref(false)
const curBillNo = ref('')
const curOrderId = ref(null)

const detailForm = reactive({ billNo: '', supplier: '', userName: '', itemList: [] })

const filters = reactive({ materialName: '', dateRange: null })
const load = async () => {
  fetching.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value, materialName: filters.materialName, inType: 1 }
    if (filters.dateRange) { params.startDate = filters.dateRange[0]; params.endDate = filters.dateRange[1] }
    const res = await inboundApi.page(params)
    if (res.code === 200 && res.data) {
      orders.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally { fetching.value = false }
}

const fetchDingTalkOrders = async () => {
  fetching.value = true
  try {
    const res = await request({ url: '/inbound/dingtalk/pull', method: 'post', params: { inType: 1 } })
    if (res.code === 200 && res.data) {
      ElMessage.success('已读取钉钉采购入库申请: ' + res.data.billNo)
      load()
    } else {
      ElMessage.warning(res.msg || '未读取到新申请')
    }
  } catch {
    ElMessage.error('读取失败')
  } finally { fetching.value = false }
}

const openDetail = async (row) => {
  curBillNo.value = row.billNo
  curOrderId.value = row.id
  try {
    const res = await inboundApi.get(row.id)
    if (res.code === 200 && res.data) {
      detailForm.billNo = res.data.billNo
      detailForm.supplier = res.data.supplier
      detailForm.userName = res.data.userName
      detailForm.itemList = (res.data.itemList || []).map(i => ({ ...i, manufacturer: '', locationNo: '', remark: '' }))
    }
  } catch { detailForm.itemList = [] }
  detailVisible.value = true
}

const addRow = () => detailForm.itemList.push({ materialName: '', packageType: '', valueData: '', specModel: '', manufacturer: '', batchNo: '', num: 1, locationNo: '', remark: '' })

const handleDetailSave = async () => {
  try {
    const res = await inboundApi.saveOrder({ ...detailForm, inType: 1 })
    if (res.code === 200) {
      ElMessage.success('入库单已提交审批')
      detailVisible.value = false
      load()
    }
  } catch { ElMessage.error('提交失败') }
}

const exportOrder = async (row) => {
  try { await downloadWithAuth(inboundApi.export(row.id)); ElMessage.success('导出完成') } catch { ElMessage.error('导出失败') }
}

const batchAudit = async (status) => {
  const ids = selectedOrders.value.map(o => o.id).join(',')
  if (!ids) return
  try {
    const res = await inboundApi.batchAudit({ ids, status })
    if (res.code === 200) { ElMessage.success(res.msg || '批量处理完成'); load() }
  } catch { ElMessage.error('批量处理失败') }
}

const confirmInbound = async (row) => {
  try {
    await ElMessageBox.confirm('确认入库？操作人将记录为当前登录用户。', '确认入库', { type: 'warning' })
    const res = await inboundApi.confirm(row.id)
    if (res.code === 200) {
      ElMessage.success('入库完成')
      load()
    }
  } catch { /* cancel */ }
}

onMounted(load)
</script>

<style scoped>
.pg { padding:0; }
.pg-hd { margin-bottom:16px; }
.pg-hd h3 { margin:0; font-size:18px; font-weight:600; }
.toolbar { display:flex; gap:12px; }
.tb-ctrl { display:flex; justify-content:space-between; align-items:center; margin:16px 0; }
.tb-title { font-size:14px; font-weight:600; }
</style>
