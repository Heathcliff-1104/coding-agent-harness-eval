<template>
  <div class="pg"><div class="pg-hd"><h3>角色权限</h3></div>
    <el-row :gutter="20">
      <el-col :span="10">
        <el-card header="角色列表">
          <div style="margin-bottom:12px">
            <el-button size="small" type="primary" @click="openCreate">新增角色</el-button>
            <el-button size="small" type="danger" :disabled="!selRole || selRole.isSystem" @click="delRole">删除角色</el-button>
          </div>
          <el-radio-group v-model="selRoleId" style="display:flex;flex-direction:column;gap:8px" @change="onSelectRole">
            <el-radio v-for="r in roles" :key="r.id" :value="r.id" border style="padding:10px 16px;width:100%">
              <span style="font-weight:600">{{ r.roleName }}</span>
              <el-tag size="small" style="margin-left:8px">{{ r.roleCode }}</el-tag>
              <span style="color:#999;margin-left:8px;font-size:12px">{{ r.description }}</span>
            </el-radio>
          </el-radio-group>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card header="权限配置">
          <el-form label-width="90px">
            <el-form-item label="数据范围">
              <el-select v-model="dataScope" style="width:220px">
                <el-option label="全部数据" value="all" />
                <el-option label="本部门" value="dept" />
                <el-option label="仅本人" value="self" />
              </el-select>
            </el-form-item>
          </el-form>
          <el-tree ref="treeRef" :data="treeData" show-checkbox node-key="id" default-expand-all :props="{label:'label',children:'children'}" style="max-height:420px;overflow:auto" />
          <el-button type="primary" style="margin-top:16px" :loading="saving" @click="saveRolePerms">保存</el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="createVisible" title="新增角色" width="420px">
      <el-form label-width="80px">
        <el-form-item label="角色编码"><el-input v-model="newRole.roleCode" placeholder="如 designer" /></el-form-item>
        <el-form-item label="角色名称"><el-input v-model="newRole.roleName" placeholder="如 设计员" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="newRole.description" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible=false">取消</el-button><el-button type="primary" @click="doCreate">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { roleApi } from '@/api/index'

const roles = ref([])
const selRoleId = ref(null)
const selRole = ref(null)
const treeData = ref([])
const treeRef = ref(null)
const dataScope = ref('all')
const saving = ref(false)
const createVisible = ref(false)
const newRole = ref({ roleCode: '', roleName: '', description: '' })

const loadRoles = async () => {
  try {
    const r = await roleApi.list()
    if (r.code === 200 && r.data) {
      roles.value = r.data
      if (roles.value.length && !selRoleId.value) {
        selRoleId.value = roles.value[0].id
        onSelectRole(selRoleId.value)
      }
    }
  } catch { /* ignore */ }
}

const loadTree = async () => {
  try {
    const r = await roleApi.permissionTree()
    if (r.code === 200) {
      treeData.value = (r.data || []).map(g => ({
        id: 'group-' + g.path,
        label: g.label,
        children: (g.children || []).map(c => ({ id: c.id, label: c.label + (c.type === 'button' ? ' [按钮]' : ''), type: c.type }))
      }))
    }
  } catch { /* ignore */ }
}

const onSelectRole = async (roleId) => {
  selRole.value = roles.value.find(r => r.id === roleId) || null
  try {
    const r = await roleApi.rolePermissions(roleId)
    const ids = r.code === 200 && r.data ? r.data : []
    dataScope.value = selRole.value?.dataScope || 'all'
    if (treeRef.value) {
      treeRef.value.setCheckedKeys(ids)
    } else {
      checkedIds.value = ids
    }
  } catch { /* ignore */ }
}

const checkedIds = ref([])

const saveRolePerms = async () => {
  if (!selRoleId.value) { ElMessage.warning('请先选择角色'); return }
  saving.value = true
  try {
    const ids = treeRef.value ? treeRef.value.getCheckedKeys().filter(k => typeof k === 'number') : checkedIds.value
    const r = await roleApi.savePermissions(selRoleId.value, { permissionIds: ids, dataScope: dataScope.value })
    if (r.code === 200) { ElMessage.success('权限配置已保存（实时生效）') }
    else ElMessage.error(r.msg || '保存失败')
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

const openCreate = () => { newRole.value = { roleCode: '', roleName: '', description: '' }; createVisible.value = true }

const doCreate = async () => {
  if (!newRole.value.roleCode || !newRole.value.roleName) { ElMessage.warning('请填写角色编码与名称'); return }
  try {
    const r = await roleApi.create({ ...newRole.value, dataScope: 'self' })
    if (r.code === 200) { ElMessage.success('角色已创建'); createVisible.value = false; loadRoles() }
  } catch { ElMessage.error('创建失败') }
}

const delRole = async () => {
  try {
    await ElMessageBox.confirm('确定删除该角色？', '删除角色')
    const r = await roleApi.del(selRoleId.value)
    if (r.code === 200) { ElMessage.success('已删除'); selRoleId.value = null; loadRoles() }
  } catch { /* cancel */ }
}

onMounted(async () => { await loadTree(); await loadRoles() })
</script>
<style scoped>.pg{padding:0}.pg-hd{margin-bottom:16px}.pg-hd h3{margin:0;font-size:18px;font-weight:600}</style>
