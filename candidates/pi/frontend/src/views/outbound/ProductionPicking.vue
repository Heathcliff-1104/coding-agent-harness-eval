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
            <div class="el-upload__tip">支持 .xlsx / .xls 格式（列：物料编码/物料名称/封装/规格型号/批次/数量）</div>
          </el-upload>
        </div>
        <div v-if="bomItems.length" style="margin-top:16px">
          <div class="tb-ctrl">
            <span class="tb-title">BOM匹配结果（{{ bomItems.length }} 项）</span>
            <el-tag v-for="(v,k) in bomSummary" :key="k" :type="v.type" size="small" style="margin-left:8px">{{ v.text }}</el-tag>
            <span style="flex:1"></span>
            <el-button size="small" @click="loadHistory">历史BOM</el-button>
          </div>
          <el-table :data="bomItems" border max-height="400">
            <el-table-column label="物料编码" prop="materialCode" width="140" />
            <el-table-column label="物料名称" prop="materialName" width="120" />
            <el-table-column label="封装" prop="packageType" width="90" />
            <el-table-column label="规格型号" prop="specModel" width="130" />
            <el-table-column label="厂家批次" prop="batchNo" width="110" />
            <el-table-column label="需要数量" prop="needNum" width="90" />
            <el-table-column label="库存状态" width="120">
              <template #default="r">
                <el-tag v-if="r.row.status==='充足'" type="success" size="small">库存充足</el-tag>
                <el-tag v-else-if="r.row.status==='不足'" type="warning" size="small">库存不足</el-tag>
                <el-tag v-else-if="r.row.status==='被占用'" type="info" size="small">被占用</el-tag>
                <el-tag v-else type="danger" size="small">缺料</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="当前库存" prop="currentStock" width="90" />
            <el-table-column label="可用" prop="available" width="90" />
            <el-table-column label="需补货" width="90">
              <template #default="r"><span v-if="Number(r.row.shortage)>0" style="color:#f56c6c">{{ r.row.shortage }}</span><span v-else>-</span></template>
            </el-table-column>
          </el-table>
          <div style="margin-top:12px">
            <el-button type="primary" :loading="planLoading" @click="saveAsPlan">保存为备料计划单</el-button>
            <el-button type="success" :disabled="!planDraftId" @click="submitDraft">提交出库审批</el-button>
            <span v-if="planDraftId" style="margin-left:8px;color:#67c23a;font-size:12px">备料计划已保存（单号见下方单据列表）</span>
          </div>
        </div>
        <el-divider />
        <el-dialog v-model="historyVisible" title="历史BOM清单" width="700px">
          <el-input v-model="historyKeyword" placeholder="按物料编码/名称/关键字搜索" style="width:260px" clearable @keyup.enter="loadHistory" />
          <el-table :data="historyList" border max-height="360" style="margin-top:12px">
            <el-table-column label="BOM单号" prop="bomNo" width="140" />
            <el-table-column label="名称" prop="bomName" min-width="120" />
            <el-table-column label="版本" prop="version" width="70" />
            <el-table-column label="创建人" prop="createUser" width="100" />
            <el-table-column label="创建时间" prop="createTime" width="160" />
            <el-table-column label="操作" width="90">
              <template #default="r"><el-button size="small" type="primary" @click="reuseHistory(r.row)">复用</el-button></template>
            </el-table-column>
          </el-table>
        </el-dialog>
      </template>

      <!-- ======== 配置BOM ======== -->
      <template v-if="mode==='config'">
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="搜索物料名称/编码" style="width:260px" clearable @input="searchMaterial" />
          <el-select v-model="filterCategory" placeholder="封装筛选" clearable style="width:140px;margin-left:8px" @change="searchMaterial">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
          <el-select v-model="filterSpec" placeholder="规格型号筛选" clearable style="width:160px;margin-left:8px" @change="searchMaterial">
            <el-option v-for="s in specs" :key="s" :label="s" :value="s" />
          </el-select>
        </div>
        <!-- 物料库检索结果 -->
        <el-table :data="searchResults" border max-height="300" style="margin-top:12px">
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
        <div style="margin-top:12px">
          <el-button type="primary" :loading="submitLoading" @click="submitConfigOutbound" :disabled="!configBomList.length || !canSubmit">发起出库审批</el-button>
          <span v-if="!canSubmit" style="margin-left:8px;color:#999;font-size:12px">出库提交需库管员/管理员权限</span>
        </div>
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
        <el-table-column label="备注" prop="remark" min-width="140" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="r">
            <template v-if="r.row.orderStatus===0 && canSubmit">
              <el-button size="small" type="success" @click="confirmOutbound(r.row)">确认出库</el-button>
              <el-button size="small" type="danger" @click="rejectOutbound(r.row)">驳回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="p" v-model:page-size="sz" :total="t" layout="total,prev,pager,next" @current-change="loadOrders" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { outboundApi, materialApi } from '@/api/index'
import { getRole } from '@/utils/permission'

const mode = ref('config')
const keyword = ref(''), filterCategory = ref(''), filterSpec = ref(''), categories = ref([]), specs = ref([])
const searchResults = ref([]), bomItems = ref([]), configBomList = ref([])
const outOrders = ref([]), orderLoading = ref(false), p = ref(1), sz = ref(10), t = ref(0)
const bomSummary = ref([])
const planLoading = ref(false), submitLoading = ref(false)
const planDraftId = ref(null)
const historyVisible = ref(false), historyKeyword = ref(''), historyList = ref([])

const role = getRole()
const canSubmit = computed(() => role === 'admin' || role === 'warehouse')

const searchMaterial = async () => {
  try {
    const r = await materialApi.page({ pageNum: 1, pageSize: 20, keyword: keyword.value, warehouseCode: filterCategory.value || undefined, materialName: keyword.value || undefined })
    if (r.code === 200 && r.data) {
      searchResults.value = r.data.records || []
    }
  } catch { /* ignore */ }
}

const loadCategories = async () => {
  try {
    const r = await materialApi.list()
    if (r.code === 200 && r.data) {
      const cset = new Set(), sset = new Set()
      r.data.forEach(m => { if (m.packageType) cset.add(m.packageType); if (m.specModel) sset.add(m.specModel) })
      categories.value = [...cset]
      specs.value = [...sset]
    }
  } catch { /* ignore */ }
}

const addToBomList = (mat) => {
  configBomList.value.push({ materialId: mat.id, materialCode: mat.materialCode, materialName: mat.materialName, packageType: mat.packageType || '', valueData: mat.valueData || '', specModel: mat.specModel || '', batchNo: '', outNum: 1, remark: '' })
}

// ====================== 导入BOM（服务端解析） ======================
const handleBomUpload = async (file) => {
  const fd = new FormData()
  fd.append('file', file.raw)
  try {
    const r = await outboundApi.bomImport(fd)
    if (r.code === 200 && r.data) {
      bomItems.value = r.data.map(i => ({ ...i, status: 'unknown', currentStock: 0, available: 0, shortage: 0 }))
      planDraftId.value = null
      ElMessage.success(`已解析${bomItems.value.length}条物料`)
      await doMatch()
    } else {
      ElMessage.error(r.msg || 'BOM解析失败')
    }
  } catch {
    ElMessage.error('BOM上传解析失败')
  }
}

const doMatch = async () => {
  if (!bomItems.value.length) return
  try {
    const r = await outboundApi.bomMatch(bomItems.value.map(i => ({
      materialCode: i.materialCode, materialName: i.materialName, packageType: i.packageType,
      specModel: i.specModel, batchNo: i.batchNo, needNum: i.needNum
    })))
    if (r.code === 200 && r.data) {
      bomItems.value = r.data
      updateSummary()
    }
  } catch { /* ignore */ }
}

const updateSummary = () => {
  const count = (s) => bomItems.value.filter(i => i.status === s).length
  bomSummary.value = [
    { text: `库存充足:${count('充足')}`, type: 'success' },
    { text: `库存不足:${count('不足')}`, type: 'warning' },
    { text: `缺料:${count('缺料')}`, type: 'danger' },
    { text: `被占用:${count('被占用')}`, type: 'info' }
  ]
}

const saveAsPlan = async () => {
  planLoading.value = true
  try {
    const r = await outboundApi.bomPlan({
      bomName: 'BOM备料计划-' + new Date().toLocaleDateString(),
      items: bomItems.value.map(i => ({
        materialId: i.materialId, materialCode: i.materialCode, materialName: i.materialName,
        packageType: i.packageType, specModel: i.specModel, batchNo: i.batchNo,
        needNum: Number(i.needNum) || 0, remark: i.status === '充足' ? '' : '需补货'
      }))
    })
    if (r.code === 200 && r.data) {
      planDraftId.value = r.data
      ElMessage.success('备料计划单已保存，已生成出库草稿')
      loadOrders()
    } else {
      ElMessage.error(r.msg || '保存失败')
    }
  } catch {
    ElMessage.error('保存备料计划失败')
  } finally {
    planLoading.value = false
  }
}

const submitDraft = async () => {
  if (!planDraftId.value) return
  try {
    const r = await outboundApi.saveOrder({ id: planDraftId.value, outType: 1, applyUser: localStorage.getItem('username') || '', remark: 'BOM备料计划出库' })
    if (r.code === 200) {
      ElMessage.success('已提交出库审批')
      planDraftId.value = null
      loadOrders()
    }
  } catch { ElMessage.error('提交失败，可能无出库权限') }
}

// ====================== 历史BOM ======================
const loadHistory = async () => {
  historyVisible.value = true
  try {
    const r = await outboundApi.bomHistory({ keyword: historyKeyword.value })
    if (r.code === 200) historyList.value = r.data || []
  } catch { /* ignore */ }
}

const reuseHistory = async (row) => {
  try {
    const r = await outboundApi.bomHistoryDetail(row.id)
    if (r.code === 200 && r.data) {
      bomItems.value = (r.data.items || []).map(i => ({ ...i, status: 'unknown' }))
      planDraftId.value = null
      historyVisible.value = false
      await doMatch()
      ElMessage.success('已复用历史BOM并重新匹配库存')
    }
  } catch { ElMessage.error('复用失败') }
}

// ====================== 配置BOM提交（saveDraft -> saveOrder 链式） ======================
const submitConfigOutbound = async () => {
  submitLoading.value = true
  try {
    const itemList = configBomList.value.map(i => ({ materialId: i.materialId, materialCode: i.materialCode, batchNo: i.batchNo, outNum: i.outNum }))
    const draftRes = await outboundApi.saveDraft({ outType: 1, applyUser: localStorage.getItem('username') || '', remark: '手动配置BOM', itemList })
    if (draftRes.code !== 200 || !draftRes.data) {
      ElMessage.error(draftRes.msg || '保存草稿失败')
      return
    }
    const orderRes = await outboundApi.saveOrder({ id: draftRes.data, outType: 1, applyUser: localStorage.getItem('username') || '', remark: '手动配置BOM' })
    if (orderRes.code === 200) {
      ElMessage.success('出库申请已提交')
      configBomList.value = []
      loadOrders()
    }
  } catch {
    ElMessage.error('提交失败')
  } finally {
    submitLoading.value = false
  }
}

// ====================== 单据确认/驳回 ======================
const confirmOutbound = async (row) => {
  try {
    const { value: operUser } = await ElMessageBox.prompt('请输入库管员账号', '确认出库')
    if (!operUser) return
    const r = await outboundApi.confirm(row.id, operUser)
    if (r.code === 200) { ElMessage.success('出库完成'); loadOrders() }
  } catch { /* cancel */ }
}

const rejectOutbound = async (row) => {
  try {
    await ElMessageBox.confirm('确定驳回该出库单？库存锁定将释放', '驳回出库单')
    const r = await outboundApi.reject(row.id)
    if (r.code === 200) { ElMessage.success('已驳回'); loadOrders() }
  } catch { /* cancel */ }
}

const loadOrders = async () => {
  orderLoading.value = true
  try {
    const r = await outboundApi.page({ pageNum: p.value, pageSize: sz.value })
    if (r.code === 200 && r.data) {
      outOrders.value = r.data.records || []
      t.value = r.data.total || 0
    }
  } finally { orderLoading.value = false }
}

onMounted(() => { loadOrders(); loadCategories() })
</script>

<style scoped>
.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}
.toolbar{display:flex;align-items:center}
.upload-area{margin:16px 0}
.tb-ctrl{display:flex;align-items:center;margin:12px 0}
.tb-title{font-size:14px;font-weight:600}
</style>
