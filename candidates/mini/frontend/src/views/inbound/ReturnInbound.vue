<template>
  <div class="pg">
    <div class="pg-hd"><h3>退库入库</h3></div>
    <el-card>
      <div class="toolbar">
        <el-button type="primary" @click="fetchDingTalkReturns" :loading="fetching">读取钉钉退库申请</el-button>
        <el-button @click="openNewReturn">手动新建退库单</el-button>
      </div>

      <el-table :data="returns" border stripe v-loading="fetching" style="margin-top:16px">
        <el-table-column label="单据号" prop="billNo" width="160" />
        <el-table-column label="退库人" prop="applyUser" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="s">
            <el-tag :type="s.row.orderStatus===0?'warning':s.row.orderStatus===1?'success':'danger'" size="small">
              {{ s.row.orderStatus===0?'待入库':s.row.orderStatus===1?'已入库':'已驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="160" />
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="s">
            <el-button size="small" @click="openDetail(s.row)">查看</el-button>
            <el-button v-if="s.row.orderStatus===0" size="small" type="success" @click="confirmReturn(s.row)">确认退库</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
        layout="total, prev, pager, next" @current-change="load" style="margin-top:16px;justify-content:flex-end" />
    </el-card>

    <el-dialog v-model="dialogVisible" title="退库单详情" width="90%" top="5vh">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="退库人"><el-input v-model="form.userName" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="退库原因"><el-select v-model="form.returnReason" style="width:100%">
            <el-option label="余料退回" value="余料退回" /><el-option label="损坏更换" value="损坏更换" /><el-option label="项目结余" value="项目结余" />
          </el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item></el-col>
        </el-row>
      </el-form>

      <div class="tb-ctrl"><span class="tb-title">退库物料明细</span><el-button type="primary" size="small" @click="addRow">添加行</el-button></div>
      <el-table :data="form.itemList" border max-height="400">
        <el-table-column label="物料名称" width="120"><template #default="s"><el-input v-model="s.row.materialName" size="small" /></template></el-table-column>
        <el-table-column label="封装" width="90"><template #default="s"><el-input v-model="s.row.packageType" size="small" /></template></el-table-column>
        <el-table-column label="规格型号" width="130"><template #default="s"><el-input v-model="s.row.specModel" size="small" /></template></el-table-column>
        <el-table-column label="厂家批次" width="110"><template #default="s"><el-input v-model="s.row.batchNo" size="small" /></template></el-table-column>
        <el-table-column label="退库数量" width="100"><template #default="s"><el-input-number v-model="s.row.num" :min="1" size="small" controls-position="right" /></template></el-table-column>
        <el-table-column label="存放货位" width="120"><template #default="s"><el-input v-model="s.row.locationNo" size="small" /></template></el-table-column>
        <el-table-column label="备注" min-width="120"><template #default="s"><el-input v-model="s.row.remark" size="small" /></template></el-table-column>
        <el-table-column label="操作" width="60" fixed="right"><template #default="s"><el-button type="danger" link size="small" @click="form.itemList.splice(s.$index,1)">删除</el-button></template></el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存并提交审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { inboundApi } from '@/api/index'

const returns = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const fetching = ref(false)
const dialogVisible = ref(false)
const curId = ref(null)

const form = reactive({ userName: '', returnReason: '', remark: '', itemList: [] })

const load = async () => {
  fetching.value = true
  try {
    const res = await inboundApi.page({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200 && res.data) { returns.value = res.data.records || []; total.value = res.data.total || 0 }
  } finally { fetching.value = false }
}

const fetchDingTalkReturns = async () => { ElMessage.info('正在从钉钉读取退库申请...'); await load() }

const openNewReturn = () => { form.userName = ''; form.returnReason = ''; form.remark = ''; form.itemList = []; curId.value = null; dialogVisible.value = true }

const openDetail = async (row) => {
  try {
    const res = await inboundApi.get(row.id)
    if (res.code === 200 && res.data) {
      form.userName = res.data.userName; form.remark = res.data.remark
      form.itemList = (res.data.itemList || []).map(i => ({ ...i, locationNo: '' }))
    }
  } catch { form.itemList = [] }
  curId.value = row.id; dialogVisible.value = true
}

const addRow = () => form.itemList.push({ materialName: '', packageType: '', specModel: '', batchNo: '', num: 1, locationNo: '', remark: '' })

const handleSave = async () => {
  try {
    const res = await inboundApi.saveOrder(form)
    if (res.code === 200) { ElMessage.success('退库单已提交'); dialogVisible.value = false; load() }
  } catch { ElMessage.error('提交失败') }
}

const confirmReturn = async (row) => {
  try {
    const { value: operUser } = await ElMessageBox.prompt('请输入库管员账号', '确认退库')
    if (!operUser) return
    const res = await inboundApi.confirm(row.id, operUser)
    if (res.code === 200) { ElMessage.success('退库完成'); load() }
  } catch { /* cancel */ }
}

onMounted(load)
</script>

<style scoped>
.pg { padding:0; } .pg-hd { margin-bottom:16px; } .pg-hd h3 { margin:0; font-size:18px; font-weight:600; }
.toolbar { display:flex; gap:12px; }
.tb-ctrl { display:flex; justify-content:space-between; align-items:center; margin:16px 0; }
.tb-title { font-size:14px; font-weight:600; }
</style>
